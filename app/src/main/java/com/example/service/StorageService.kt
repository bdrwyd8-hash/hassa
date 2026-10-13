package com.example.service

import android.content.Context
import android.content.SharedPreferences
import com.example.data.EquipmentCatalog
import com.example.engine.ExpeditionAiEngine
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.CharacterConfig
import com.example.model.ChecklistEntry
import com.example.model.EquippedLayer
import com.example.model.ExpeditionProgression
import com.example.model.InventoryGridCell
import com.example.model.LayerSlot
import com.example.model.SavedCharacter
import com.example.model.SceneBackground
import com.example.model.TripType
import com.example.model.UserPreferences
import org.json.JSONArray
import org.json.JSONObject

data class PersistedAppState(
    val activeCharacterId: String,
    val activeCharacterName: String,
    val activeConfig: CharacterConfig,
    val savedCharacters: List<SavedCharacter>,
    val checklistEntries: List<ChecklistEntry>,
    val preferences: UserPreferences,
    val inventoryGrid: List<InventoryGridCell> = ExpeditionAiEngine.createStarterInventoryGrid(),
    val progression: ExpeditionProgression = ExpeditionProgression()
)

class StorageService(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "trailkit_studio_v1_storage"
        const val CURRENT_SCHEMA_VERSION = 1

        private const val KEY_SCHEMA_VERSION = "schema_version"
        private const val KEY_ACTIVE_CHARACTER = "active_character_json"
        private const val KEY_SAVED_CHARACTERS = "saved_characters_json"
        private const val KEY_CHECKLIST = "checklist_json"
        private const val KEY_PREFERENCES = "preferences_json"
        private const val KEY_INVENTORY_GRID = "inventory_grid_json"
        private const val KEY_PROGRESSION = "expedition_progression_json"

        fun configToJson(config: CharacterConfig): JSONObject {
            val obj = JSONObject()
            obj.put("skinColorHex", config.skinColorHex)
            obj.put("hairStyle", config.hairStyle)
            obj.put("hairColorHex", config.hairColorHex)
            obj.put("eyeColorHex", config.eyeColorHex)
            obj.put("baseUnderwearColorHex", config.baseUnderwearColorHex)
            obj.put("scene", config.scene.id)

            val layersArr = JSONArray()
            config.orderedLayers().forEach { layer ->
                val lObj = JSONObject()
                lObj.put("slot", layer.slot.id)
                lObj.put("itemId", layer.itemId)
                lObj.put("colorHex", layer.colorHex)
                lObj.put("visible", layer.visible)
                lObj.put("zOrder", layer.zOrder)
                layersArr.put(lObj)
            }
            obj.put("equippedLayers", layersArr)
            return obj
        }

        fun configFromJson(obj: JSONObject): CharacterConfig {
            val defaultConfig = EquipmentCatalog.defaultCharacterConfig()
            val skin = sanitizeHex(obj.optString("skinColorHex", defaultConfig.skinColorHex), defaultConfig.skinColorHex)
            val hairStyle = obj.optInt("hairStyle", defaultConfig.hairStyle).coerceIn(0, 3)
            val hairColor = sanitizeHex(obj.optString("hairColorHex", defaultConfig.hairColorHex), defaultConfig.hairColorHex)
            val eyeColor = sanitizeHex(obj.optString("eyeColorHex", defaultConfig.eyeColorHex), defaultConfig.eyeColorHex)
            val baseColor = sanitizeHex(obj.optString("baseUnderwearColorHex", defaultConfig.baseUnderwearColorHex), defaultConfig.baseUnderwearColorHex)
            val sceneId = obj.optString("scene", defaultConfig.scene.id)
            val scene = SceneBackground.entries.find { it.id == sceneId } ?: SceneBackground.PINE_VALLEY

            val equippedMap = mutableMapOf<LayerSlot, EquippedLayer>()
            val layersArr = obj.optJSONArray("equippedLayers")
            if (layersArr != null) {
                for (i in 0 until layersArr.length()) {
                    val lObj = layersArr.optJSONObject(i) ?: continue
                    val slotId = lObj.optString("slot", "")
                    val itemId = lObj.optString("itemId", "")
                    val slot = LayerSlot.entries.find { it.id == slotId } ?: continue
                    val catalogItem = EquipmentCatalog.findById(itemId) ?: continue
                    val defaultColor = catalogItem.colors.firstOrNull() ?: "#3D6B52"
                    val colorHex = sanitizeHex(lObj.optString("colorHex", defaultColor), defaultColor)
                    val visible = lObj.optBoolean("visible", true)
                    val zOrder = lObj.optInt("zOrder", slot.defaultZOrder).coerceIn(1, 200)
                    equippedMap[slot] = EquippedLayer(
                        slot = slot,
                        itemId = catalogItem.id,
                        colorHex = colorHex,
                        visible = visible,
                        zOrder = zOrder
                    )
                }
            }
            return CharacterConfig(
                skinColorHex = skin,
                hairStyle = hairStyle,
                hairColorHex = hairColor,
                eyeColorHex = eyeColor,
                baseUnderwearColorHex = baseColor,
                scene = scene,
                equippedLayers = equippedMap
            )
        }

        fun sanitizeHex(input: String?, fallback: String): String {
            if (input.isNullOrBlank()) return fallback
            val trimmed = input.trim()
            val regex = Regex("^#[0-9A-Fa-f]{6}$")
            return if (regex.matches(trimmed)) trimmed.uppercase() else fallback
        }
    }

    fun loadState(): PersistedAppState {
        return try {
            val preferences = loadPreferences()
            val savedCharacters = loadSavedCharacters(preferences.language)
            val (activeId, activeName, activeConfig) = loadActiveCharacter(savedCharacters)
            val checklist = loadChecklist()
            val grid = loadInventoryGrid()
            val progression = loadProgression()
            PersistedAppState(
                activeCharacterId = activeId,
                activeCharacterName = activeName,
                activeConfig = activeConfig,
                savedCharacters = savedCharacters,
                checklistEntries = checklist,
                preferences = preferences,
                inventoryGrid = grid,
                progression = progression
            )
        } catch (e: Exception) {
            createDefaultState()
        }
    }

    fun saveActiveCharacter(id: String, name: String, config: CharacterConfig): Boolean {
        return try {
            val obj = JSONObject()
            obj.put("id", id)
            obj.put("name", name)
            obj.put("config", configToJson(config))
            prefs.edit()
                .putInt(KEY_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION)
                .putString(KEY_ACTIVE_CHARACTER, obj.toString())
                .commit()
        } catch (e: Exception) {
            false
        }
    }

    fun saveCharactersList(characters: List<SavedCharacter>): Boolean {
        return try {
            val arr = JSONArray()
            characters.forEach { ch ->
                val obj = JSONObject()
                obj.put("id", ch.id)
                obj.put("name", ch.name)
                obj.put("updatedAt", ch.updatedAt)
                obj.put("config", configToJson(ch.config))
                arr.put(obj)
            }
            prefs.edit()
                .putInt(KEY_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION)
                .putString(KEY_SAVED_CHARACTERS, arr.toString())
                .commit()
        } catch (e: Exception) {
            false
        }
    }

    fun saveChecklist(entries: List<ChecklistEntry>): Boolean {
        return try {
            val arr = JSONArray()
            entries.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("tripType", item.tripType.id)
                obj.put("titleAr", item.titleAr)
                obj.put("titleEn", item.titleEn)
                obj.put("weightGrams", item.weightGrams)
                obj.put("isPacked", item.isPacked)
                obj.put("isCustom", item.isCustom)
                arr.put(obj)
            }
            prefs.edit()
                .putInt(KEY_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION)
                .putString(KEY_CHECKLIST, arr.toString())
                .commit()
        } catch (e: Exception) {
            false
        }
    }

    fun savePreferences(preferences: UserPreferences): Boolean {
        return try {
            val obj = JSONObject()
            obj.put("language", preferences.language.code)
            obj.put("themeMode", preferences.themeMode.name)
            obj.put("animationsEnabled", preferences.animationsEnabled)
            obj.put("soundEnabled", preferences.soundEnabled)
            prefs.edit()
                .putInt(KEY_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION)
                .putString(KEY_PREFERENCES, obj.toString())
                .commit()
        } catch (e: Exception) {
            false
        }
    }

    fun saveInventoryGrid(grid: List<InventoryGridCell>): Boolean {
        return try {
            val arr = JSONArray()
            grid.forEach { cell ->
                val obj = JSONObject()
                obj.put("index", cell.index)
                obj.put("itemId", cell.itemId ?: "")
                obj.put("quantity", cell.quantity)
                arr.put(obj)
            }
            prefs.edit()
                .putString(KEY_INVENTORY_GRID, arr.toString())
                .commit()
        } catch (e: Exception) {
            false
        }
    }

    fun saveProgression(progression: ExpeditionProgression): Boolean {
        return try {
            val obj = JSONObject()
            obj.put("missionsCompleted", progression.missionsCompleted)
            obj.put("totalCachesSecured", progression.totalCachesSecured)
            obj.put("highestDifficultyWon", progression.highestDifficultyWon)
            prefs.edit()
                .putString(KEY_PROGRESSION, obj.toString())
                .commit()
        } catch (e: Exception) {
            false
        }
    }

    private fun loadInventoryGrid(): List<InventoryGridCell> {
        val raw = prefs.getString(KEY_INVENTORY_GRID, null)
            ?: return ExpeditionAiEngine.createStarterInventoryGrid()
        return try {
            val arr = JSONArray(raw)
            val cells = MutableList(ExpeditionAiEngine.GRID_SLOTS_COUNT) { idx ->
                InventoryGridCell(index = idx)
            }
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val idx = obj.optInt("index", -1)
                if (idx !in cells.indices) continue
                val itemId = obj.optString("itemId", "").takeIf {
                    it.isNotBlank() && EquipmentCatalog.findById(it) != null
                }
                val qty = obj.optInt("quantity", 0).coerceIn(0, 99)
                cells[idx] = InventoryGridCell(
                    index = idx,
                    itemId = if (qty > 0) itemId else null,
                    quantity = if (itemId != null) qty else 0
                )
            }
            cells
        } catch (e: Exception) {
            ExpeditionAiEngine.createStarterInventoryGrid()
        }
    }

    private fun loadProgression(): ExpeditionProgression {
        val raw = prefs.getString(KEY_PROGRESSION, null) ?: return ExpeditionProgression()
        return try {
            val obj = JSONObject(raw)
            ExpeditionProgression(
                missionsCompleted = obj.optInt("missionsCompleted", 0),
                totalCachesSecured = obj.optInt("totalCachesSecured", 0),
                highestDifficultyWon = obj.optString("highestDifficultyWon", "")
            )
        } catch (e: Exception) {
            ExpeditionProgression()
        }
    }

    fun clearAllData(): PersistedAppState {
        prefs.edit().clear().commit()
        val fresh = createDefaultState()
        savePreferences(fresh.preferences)
        saveCharactersList(fresh.savedCharacters)
        saveActiveCharacter(fresh.activeCharacterId, fresh.activeCharacterName, fresh.activeConfig)
        saveChecklist(fresh.checklistEntries)
        return fresh
    }

    private fun loadPreferences(): UserPreferences {
        val raw = prefs.getString(KEY_PREFERENCES, null) ?: return UserPreferences()
        return try {
            val obj = JSONObject(raw)
            val langCode = obj.optString("language", "ar")
            val lang = if (langCode == "en") AppLanguage.EN else AppLanguage.AR
            val themeName = obj.optString("themeMode", AppThemeMode.DARK.name)
            val theme = AppThemeMode.entries.find { it.name == themeName } ?: AppThemeMode.DARK
            UserPreferences(
                language = lang,
                themeMode = theme,
                animationsEnabled = obj.optBoolean("animationsEnabled", true),
                soundEnabled = obj.optBoolean("soundEnabled", true)
            )
        } catch (e: Exception) {
            UserPreferences()
        }
    }

    private fun loadSavedCharacters(lang: AppLanguage): List<SavedCharacter> {
        val raw = prefs.getString(KEY_SAVED_CHARACTERS, null)
        if (raw.isNullOrBlank()) {
            val starter = starterSavedCharacters(lang)
            saveCharactersList(starter)
            return starter
        }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SavedCharacter>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").takeIf { it.isNotBlank() } ?: continue
                val name = obj.optString("name", "").trim().takeIf { it.isNotBlank() } ?: continue
                val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                val configObj = obj.optJSONObject("config") ?: continue
                val config = configFromJson(configObj)
                list.add(SavedCharacter(id = id, name = name, updatedAt = updatedAt, config = config))
            }
            list
        } catch (e: Exception) {
            starterSavedCharacters(lang)
        }
    }

    private fun loadActiveCharacter(saved: List<SavedCharacter>): Triple<String, String, CharacterConfig> {
        val raw = prefs.getString(KEY_ACTIVE_CHARACTER, null)
        if (!raw.isNullOrBlank()) {
            try {
                val obj = JSONObject(raw)
                val id = obj.optString("id", "char-starter-1")
                val name = obj.optString("name", "حارس الغابة")
                val configObj = obj.optJSONObject("config")
                if (configObj != null) {
                    return Triple(id, name, configFromJson(configObj))
                }
            } catch (_: Exception) {
            }
        }
        val first = saved.firstOrNull()
        return if (first != null) {
            Triple(first.id, first.name, first.config)
        } else {
            Triple("char-starter-1", "حارس الغابة", EquipmentCatalog.defaultCharacterConfig())
        }
    }

    private fun loadChecklist(): List<ChecklistEntry> {
        val raw = prefs.getString(KEY_CHECKLIST, null)
        if (raw.isNullOrBlank()) {
            val defaults = EquipmentCatalog.allDefaultChecklists()
            saveChecklist(defaults)
            return defaults
        }
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<ChecklistEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").takeIf { it.isNotBlank() } ?: continue
                val tripId = obj.optString("tripType", TripType.DAY_HIKE.id)
                val tripType = TripType.entries.find { it.id == tripId } ?: TripType.DAY_HIKE
                val titleAr = obj.optString("titleAr", "").trim()
                val titleEn = obj.optString("titleEn", "").trim()
                if (titleAr.isEmpty() && titleEn.isEmpty()) continue
                result.add(
                    ChecklistEntry(
                        id = id,
                        tripType = tripType,
                        titleAr = titleAr.ifEmpty { titleEn },
                        titleEn = titleEn.ifEmpty { titleAr },
                        weightGrams = obj.optInt("weightGrams", 200).coerceIn(5, 25000),
                        isPacked = obj.optBoolean("isPacked", false),
                        isCustom = obj.optBoolean("isCustom", false)
                    )
                )
            }
            if (result.isEmpty()) EquipmentCatalog.allDefaultChecklists() else result
        } catch (e: Exception) {
            EquipmentCatalog.allDefaultChecklists()
        }
    }

    private fun starterSavedCharacters(lang: AppLanguage): List<SavedCharacter> {
        val now = System.currentTimeMillis()
        return EquipmentCatalog.presets.take(3).mapIndexed { index, preset ->
            SavedCharacter(
                id = "char-starter-${index + 1}",
                name = preset.label(lang),
                updatedAt = now - (index * 60_000L),
                config = preset.config
            )
        }
    }

    private fun createDefaultState(): PersistedAppState {
        val prefsObj = UserPreferences()
        val starters = starterSavedCharacters(prefsObj.language)
        val first = starters.first()
        return PersistedAppState(
            activeCharacterId = first.id,
            activeCharacterName = first.name,
            activeConfig = first.config,
            savedCharacters = starters,
            checklistEntries = EquipmentCatalog.allDefaultChecklists(),
            preferences = prefsObj
        )
    }
}
