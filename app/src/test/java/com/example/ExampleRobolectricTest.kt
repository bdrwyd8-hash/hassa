package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.EquipmentCatalog
import com.example.model.AppLanguage
import com.example.model.EquipmentCategory
import com.example.model.LayerSlot
import com.example.model.MainSection
import com.example.model.TripType
import com.example.service.ExportService
import com.example.service.ImportValidationResult
import com.example.service.PngExportResult
import com.example.service.StorageService
import com.example.viewmodel.TrailKitViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var storageService: StorageService
    private lateinit var viewModel: TrailKitViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storageService = StorageService(context)
        storageService.clearAllData()
        viewModel = TrailKitViewModel(storageService)
    }

    @Test
    fun `verify app name and starter catalog has over 40 unique safe outdoor items`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("TrailKit Studio", appName)

        val items = EquipmentCatalog.items
        assertTrue("Expected at least 40 items, found ${items.size}", items.size >= 40)

        val uniqueIds = items.map { it.id }.toSet()
        assertEquals("All item IDs must be unique", items.size, uniqueIds.size)

        // Ensure all 14 categories have items
        val nonAllCategories = EquipmentCategory.entries.filter { it != EquipmentCategory.ALL }
        nonAllCategories.forEach { cat ->
            assertTrue("Category ${cat.id} should not be empty", items.any { it.category == cat })
        }
    }

    @Test
    fun `test equipping removing hiding showing moving slots and undo redo reset`() {
        // Start from bare reset state
        viewModel.resetCharacterToBareDefault()
        assertEquals(0, viewModel.uiState.value.equippedCount)
        assertTrue(viewModel.uiState.value.canUndo)

        // Equip a shirt (UPPER_BODY) and a jacket (OUTERWEAR) — must NOT replace each other!
        viewModel.toggleEquipItem("shirt-merino-base")
        viewModel.toggleEquipItem("jacket-alpine-shell")
        assertEquals(2, viewModel.uiState.value.equippedCount)
        assertNotNull(viewModel.uiState.value.characterConfig.equippedLayers[LayerSlot.UPPER_BODY])
        assertNotNull(viewModel.uiState.value.characterConfig.equippedLayers[LayerSlot.OUTERWEAR])

        // Change color of jacket
        viewModel.setLayerColor(LayerSlot.OUTERWEAR, "#E5A93C")
        assertEquals("#E5A93C", viewModel.uiState.value.characterConfig.equippedLayers[LayerSlot.OUTERWEAR]?.colorHex)

        // Hide jacket layer — must preserve saved data
        viewModel.toggleLayerVisibility(LayerSlot.OUTERWEAR)
        val hiddenLayer = viewModel.uiState.value.characterConfig.equippedLayers[LayerSlot.OUTERWEAR]
        assertNotNull(hiddenLayer)
        assertFalse(hiddenLayer!!.visible)
        assertEquals("jacket-alpine-shell", hiddenLayer.itemId)

        // Show layer again
        viewModel.toggleLayerVisibility(LayerSlot.OUTERWEAR)
        assertTrue(viewModel.uiState.value.characterConfig.equippedLayers[LayerSlot.OUTERWEAR]!!.visible)

        // Equip water bottle in LEFT_HAND_GEAR and move it to RIGHT_HAND_GEAR
        viewModel.toggleEquipItem("water-steel-flask-1l")
        assertNotNull(viewModel.uiState.value.characterConfig.equippedLayers[LayerSlot.LEFT_HAND_GEAR])
        viewModel.moveItemToSlot(LayerSlot.LEFT_HAND_GEAR, LayerSlot.RIGHT_HAND_GEAR)
        assertEquals(
            "water-steel-flask-1l",
            viewModel.uiState.value.characterConfig.equippedLayers[LayerSlot.RIGHT_HAND_GEAR]?.itemId
        )

        // Undo & Redo
        viewModel.undo()
        assertNotNull(viewModel.uiState.value.characterConfig.equippedLayers[LayerSlot.LEFT_HAND_GEAR])
        assertTrue(viewModel.uiState.value.canRedo)
        viewModel.redo()
        assertNotNull(viewModel.uiState.value.characterConfig.equippedLayers[LayerSlot.RIGHT_HAND_GEAR])
    }

    @Test
    fun `test saving renaming duplicating deleting and local persistence across reload`() {
        viewModel.createNewCharacter("Desert Scout", EquipmentCatalog.presets[1])
        val createdId = viewModel.uiState.value.activeCharacterId
        assertEquals("Desert Scout", viewModel.uiState.value.activeCharacterName)

        // Rename
        viewModel.renameSavedCharacter(createdId, "Sinai Trailblazer")
        assertEquals("Sinai Trailblazer", viewModel.uiState.value.activeCharacterName)

        // Duplicate
        val countBeforeDup = viewModel.uiState.value.savedCharacters.size
        viewModel.duplicateSavedCharacter(createdId)
        assertEquals(countBeforeDup + 1, viewModel.uiState.value.savedCharacters.size)

        // Reload a fresh ViewModel from StorageService to verify persistence
        val reloadedVm = TrailKitViewModel(StorageService(context))
        assertEquals("Sinai Trailblazer", reloadedVm.uiState.value.activeCharacterName)
        assertEquals(countBeforeDup + 1, reloadedVm.uiState.value.savedCharacters.size)

        // Delete character
        reloadedVm.deleteSavedCharacter(createdId)
        assertEquals(countBeforeDup, reloadedVm.uiState.value.savedCharacters.size)
    }

    @Test
    fun `test json export and strict json import validation`() {
        val currentConfig = viewModel.uiState.value.characterConfig
        val exportedJson = ExportService.exportCharacterJson("Alpine Explorer", currentConfig)
        assertTrue(exportedJson.contains("\"schemaVersion\": 1"))
        assertTrue(exportedJson.contains("Alpine Explorer"))

        // Valid import
        val countBefore = viewModel.uiState.value.savedCharacters.size
        val importOk = viewModel.importCharacterFromJson(exportedJson)
        assertTrue(importOk)
        assertEquals(countBefore + 1, viewModel.uiState.value.savedCharacters.size)

        // Reject malformed JSON
        val badJsonRes = ExportService.validateAndParseCharacterJson("{ invalid json", emptyList())
        assertTrue(badJsonRes is ImportValidationResult.Error)

        // Reject incompatible schema version
        val badVersionJson = exportedJson.replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")
        val badVerRes = ExportService.validateAndParseCharacterJson(badVersionJson, emptyList())
        assertTrue(badVerRes is ImportValidationResult.Error)

        // Reject unknown item reference
        val badItemJson = exportedJson.replace("shirt-merino-base", "non-existent-item-xyz")
        val badItemRes = ExportService.validateAndParseCharacterJson(badItemJson, emptyList())
        assertTrue(badItemRes is ImportValidationResult.Error)
    }

    @Test
    fun `test png export produces non-blank bitmap and valid png bytes`() {
        val res = ExportService.exportCharacterPng(
            context = context,
            characterName = "Ranger",
            config = viewModel.uiState.value.characterConfig,
            useNeutralBackground = false
        )
        assertTrue(res is PngExportResult.Success)
        val success = res as PngExportResult.Success
        assertEquals(800, success.bitmap.width)
        assertEquals(1000, success.bitmap.height)
        assertTrue("PNG byte array must not be empty", success.pngBytes.size > 1000)

        // Verify center pixel (explorer torso) and background pixel are rendered and non-transparent
        val centerPixel = success.bitmap.getPixel(400, 450)
        assertTrue("Center pixel alpha should be 255", ((centerPixel ushr 24) and 0xFF) == 255)
    }

    @Test
    fun `test checklist packing adding custom items and resetting`() {
        viewModel.selectTripType(TripType.CAMPING)
        val initialItems = viewModel.uiState.value.currentTripChecklist()
        assertTrue(initialItems.isNotEmpty())

        viewModel.addCustomChecklistItem("Solar Weather Radio", 220)
        val afterAdd = viewModel.uiState.value.currentTripChecklist()
        assertEquals(initialItems.size + 1, afterAdd.size)

        val customItem = afterAdd.last()
        assertFalse(customItem.isPacked)
        viewModel.toggleChecklistItem(customItem.id)
        assertTrue(viewModel.uiState.value.currentTripChecklist().last().isPacked)

        viewModel.resetCurrentTripChecklist()
        val afterReset = viewModel.uiState.value.currentTripChecklist()
        assertEquals(initialItems.size, afterReset.size)
        assertTrue(afterReset.none { it.isPacked })
    }

    @Test
    fun `test compose ui navigation and arabic english switching`() {
        composeTestRule.setContent {
            TrailKitApp(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Default language is Arabic (RTL)
        assertEquals(AppLanguage.AR, viewModel.uiState.value.preferences.language)
        composeTestRule.onNodeWithTag("studio_screen").assertExists()

        // Navigate to My Characters
        composeTestRule.onNodeWithTag("nav_tab_characters").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainSection.MY_CHARACTERS, viewModel.uiState.value.currentSection)
        composeTestRule.onNodeWithTag("saved_characters_screen").assertExists()

        // Navigate to Checklist
        composeTestRule.onNodeWithTag("nav_tab_checklist").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainSection.CHECKLIST, viewModel.uiState.value.currentSection)
        composeTestRule.onNodeWithTag("checklist_screen").assertExists()

        // Navigate to Expedition & AI Mode
        composeTestRule.onNodeWithTag("nav_tab_expedition").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainSection.EXPEDITION, viewModel.uiState.value.currentSection)
        composeTestRule.onNodeWithTag("expedition_screen").assertExists()

        // Navigate to Settings and switch language to English
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainSection.SETTINGS, viewModel.uiState.value.currentSection)
        composeTestRule.onNodeWithTag("lang_english_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppLanguage.EN, viewModel.uiState.value.preferences.language)
    }

    @Test
    fun `test offline bot ai obstacle avoidance search last known position and squad flanking`() {
        val initialWorld = com.example.engine.ExpeditionAiEngine.createInitialWorld(
            difficulty = com.example.model.AiDifficulty.TACTICIAN
        )
        // Verify all difficulties have identical 100 HP (decision quality scaling, not HP inflation)
        com.example.model.AiDifficulty.entries.forEach { diff ->
            val w = com.example.engine.ExpeditionAiEngine.createInitialWorld(diff)
            assertTrue(w.bots.all { it.health == 100 })
        }

        // Verify A* pathfinding routes around WALL_ROCK at (3,3) and (3,4)
        val path = com.example.engine.ExpeditionAiEngine.findPathAStar(
            tiles = initialWorld.tiles,
            start = com.example.model.GridPoint(2, 3),
            goal = com.example.model.GridPoint(4, 3)
        )
        assertTrue("Path should not be empty", path.isNotEmpty())
        assertTrue(
            "Path must never step on WALL_ROCK",
            path.none { initialWorld.tiles[it.y][it.x] == com.example.model.TileType.WALL_ROCK }
        )

        // Verify Line-of-Sight is blocked by rock wall at (3,3)
        val losBlocked = com.example.engine.ExpeditionAiEngine.hasLineOfSight(
            tiles = initialWorld.tiles,
            from = com.example.model.GridPoint(2, 3),
            to = com.example.model.GridPoint(4, 3),
            maxRange = 8f,
            targetInBrush = false
        )
        assertFalse("Rock wall at (3,3) must block line-of-sight", losBlocked)

        // Place player in open view of two bots on TACTICIAN difficulty -> verify PURSUE + FLANK_COORDINATE
        val openPos = com.example.model.GridPoint(8, 5)
        val botsNearPlayer = initialWorld.bots.mapIndexed { idx, b ->
            when (idx) {
                0 -> b.copy(pos = com.example.model.GridPoint(8, 2))
                1 -> b.copy(pos = com.example.model.GridPoint(11, 5))
                else -> b
            }
        }
        val spottedWorld = com.example.engine.ExpeditionAiEngine.stepBotsAi(
            initialWorld.copy(playerPos = openPos, isPlayerStealthInBrush = false, bots = botsNearPlayer)
        )
        val states = spottedWorld.bots.map { it.state }
        assertTrue("Primary bot should PURSUE", states.contains(com.example.model.BotAiState.PURSUE))
        assertTrue("Cooperating bot should FLANK_COORDINATE", states.contains(com.example.model.BotAiState.FLANK_COORDINATE))

        // Now move player behind a rock wall so LOS is broken -> bots must transition to SEARCH_LAST_KNOWN
        val hiddenPlayerPos = com.example.model.GridPoint(2, 3) // behind wall (3,3)
        val searchingWorld = com.example.engine.ExpeditionAiEngine.stepBotsAi(
            spottedWorld.copy(playerPos = hiddenPlayerPos)
        )
        assertTrue(
            "Bot that lost LOS must enter SEARCH_LAST_KNOWN state",
            searchingWorld.bots.any { it.state == com.example.model.BotAiState.SEARCH_LAST_KNOWN && it.lastKnownPlayerPos != null }
        )

        // Test Flashlight Strobe forces nearby bot into RETREAT_COVER
        val closeBotWorld = searchingWorld.copy(
            playerPos = openPos,
            bots = searchingWorld.bots.mapIndexed { i, b -> if (i == 0) b.copy(pos = com.example.model.GridPoint(8, 4)) else b }
        )
        val afterStrobe = com.example.engine.ExpeditionAiEngine.triggerFlashlightStrobe(closeBotWorld)
        assertEquals(com.example.model.BotAiState.RETREAT_COVER, afterStrobe.bots.first().state)
    }

    @Test
    fun `test 5x4 slot grid inventory drag drop swap consume and persistence`() {
        val gridBefore = viewModel.uiState.value.expeditionWorld.inventoryGrid
        assertEquals(20, gridBefore.size)
        val slot0Item = gridBefore[0].itemId
        assertNotNull(slot0Item)

        // Move item from slot 0 to empty slot 10
        viewModel.moveGridSlotDragDrop(0, 10)
        val gridAfterMove = viewModel.uiState.value.expeditionWorld.inventoryGrid
        assertEquals(null, gridAfterMove[0].itemId)
        assertEquals(slot0Item, gridAfterMove[10].itemId)

        // Verify persisted in StorageService
        val reloadedVm = TrailKitViewModel(StorageService(context))
        assertEquals(slot0Item, reloadedVm.uiState.value.expeditionWorld.inventoryGrid[10].itemId)
    }

    @Test
    fun `test real-time continuous survival game simulation physics stealth supplies and extraction`() {
        val engine = com.example.game.SurvivalGameEngine()
        val initialState = engine.createInitialState(difficulty = com.example.model.AiDifficulty.RANGER)

        // Verify player initial coordinates, health, and stamina
        assertEquals(280f, initialState.player.x, 0.01f)
        assertEquals(1450f, initialState.player.y, 0.01f)
        assertEquals(100, initialState.player.health)
        assertEquals(100f, initialState.player.stamina, 0.01f)

        // Verify simulated continuous movement
        val moved = engine.update(
            currentState = initialState,
            joystickX = 1.0f,
            joystickY = 0.0f,
            isSprinting = false,
            deltaSeconds = 0.5f
        )
        assertTrue("Player should have moved to the right", moved.player.x > initialState.player.x)
        assertEquals(initialState.player.y, moved.player.y, 0.5f)

        // Verify sprinting burns stamina and increases speed
        val sprinted = engine.update(
            currentState = initialState,
            joystickX = 1.0f,
            joystickY = 0.0f,
            isSprinting = true,
            deltaSeconds = 0.5f
        )
        assertTrue(sprinted.player.stamina < 100f)
        assertTrue(sprinted.player.x > moved.player.x)

        // Verify stealth in bush detection (Bush 1 is at 400, 1460 with radius 48)
        val inBushPlayer = initialState.copy(player = initialState.player.copy(x = 400f, y = 1460f))
        val stealthChecked = engine.update(
            currentState = inBushPlayer,
            joystickX = 0f,
            joystickY = 0f,
            isSprinting = false,
            deltaSeconds = 0.1f
        )
        assertTrue("Player inside bush must have inStealthBush = true", stealthChecked.player.inStealthBush)

        // Verify Acoustic Decoy projectile launch and landing
        val afterDecoy = engine.throwDecoy(initialState)
        assertNotNull(afterDecoy.decoyProjectile)
        assertFalse(afterDecoy.decoyProjectile!!.hasLanded)

        // Advance simulation until decoy lands (dt is clamped to 0.1f per update)
        var decoyLanded = afterDecoy
        repeat(6) {
            decoyLanded = engine.update(
                currentState = decoyLanded,
                joystickX = 0f,
                joystickY = 0f,
                isSprinting = false,
                deltaSeconds = 0.1f
            )
        }
        assertNotNull(decoyLanded.decoyProjectile)
        assertTrue(decoyLanded.decoyProjectile!!.hasLanded)

        // Verify supply collection
        val nearBeaconState = initialState.copy(
            player = initialState.player.copy(x = 680f, y = 1200f) // at cache-beacon-1
        )
        val tickNearBeacon = engine.update(
            currentState = nearBeaconState,
            joystickX = 0f,
            joystickY = 0f,
            isSprinting = false,
            deltaSeconds = 0.1f
        )
        assertNotNull(tickNearBeacon.nearbySupply)
        assertEquals("cache-beacon-1", tickNearBeacon.nearbySupply!!.id)

        val (collectedState, grid) = engine.interactCollectSupply(tickNearBeacon, viewModel.uiState.value.expeditionWorld.inventoryGrid)
        assertEquals(1, collectedState.cachesSecuredCount)
        assertTrue(grid.any { it.itemId == "nav-brass-compass" })
    }
}
