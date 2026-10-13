package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.EquipmentCatalog
import com.example.engine.ExpeditionAiEngine
import com.example.engine.ExpeditionWorldState
import com.example.game.SurvivalGameEngine
import com.example.game.SurvivalGameplayState
import com.example.localization.AppStrings
import com.example.model.AiDifficulty
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.CharacterConfig
import com.example.model.CharacterPreset
import com.example.model.CharacterSortOption
import com.example.model.ChecklistEntry
import com.example.model.EquipmentCategory
import com.example.model.EquipmentItem
import com.example.model.EquippedLayer
import com.example.model.ExpeditionProgression
import com.example.model.GridPoint
import com.example.model.ItemSortOption
import com.example.model.LayerSlot
import com.example.model.MainSection
import com.example.model.SavedCharacter
import com.example.model.SceneBackground
import com.example.model.TripType
import com.example.model.UserPreferences
import com.example.service.ExportService
import com.example.service.ImportValidationResult
import com.example.service.PngExportResult
import com.example.service.StorageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TrailKitUiState(
    val currentSection: MainSection = MainSection.STUDIO,
    val activeCharacterId: String = "char-starter-1",
    val activeCharacterName: String = "حارس الغابة",
    val characterConfig: CharacterConfig = EquipmentCatalog.defaultCharacterConfig(),
    val undoStack: List<CharacterConfig> = emptyList(),
    val redoStack: List<CharacterConfig> = emptyList(),
    // Equipment Library Filter/Search/Sort/Selection
    val selectedCategory: EquipmentCategory = EquipmentCategory.ALL,
    val searchQuery: String = "",
    val itemSortOption: ItemSortOption = ItemSortOption.DEFAULT,
    val selectedItemId: String? = "pack-canvas-heritage-40l",
    val selectedSlot: LayerSlot? = LayerSlot.BACKPACK,
    // Saved Characters
    val savedCharacters: List<SavedCharacter> = emptyList(),
    val characterSortOption: CharacterSortOption = CharacterSortOption.RECENT,
    val previewingCharacter: SavedCharacter? = null,
    // Equipment Checklist
    val selectedTripType: TripType = TripType.DAY_HIKE,
    val checklistEntries: List<ChecklistEntry> = emptyList(),
    // Preferences
    val preferences: UserPreferences = UserPreferences(),
    // Offline Single-Player Expedition & Slot Grid Inventory
    val expeditionWorld: ExpeditionWorldState = ExpeditionAiEngine.createInitialWorld(),
    val progression: ExpeditionProgression = ExpeditionProgression(),
    val survivalGameplay: SurvivalGameplayState = SurvivalGameEngine().createInitialState(),
    // Export / Import / Notifications
    val exportedJsonPreview: String? = null,
    val exportedPngResult: PngExportResult.Success? = null,
    val importErrorMessage: String? = null,
    val statusBannerMessage: String? = null
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    val totalWeightGrams: Int
        get() = characterConfig.equippedLayers.values
            .filter { it.visible }
            .sumOf { layer -> EquipmentCatalog.findById(layer.itemId)?.weightGrams ?: 0 }

    val equippedCount: Int
        get() = characterConfig.equippedLayers.size

    fun filteredItems(): List<EquipmentItem> {
        val query = searchQuery.trim().lowercase()
        val filtered = EquipmentCatalog.items.filter { item ->
            val matchesCat = selectedCategory == EquipmentCategory.ALL || item.category == selectedCategory
            val matchesQuery = query.isEmpty() ||
                item.nameAr.lowercase().contains(query) ||
                item.nameEn.lowercase().contains(query) ||
                item.descriptionAr.lowercase().contains(query) ||
                item.descriptionEn.lowercase().contains(query)
            matchesCat && matchesQuery
        }
        return when (itemSortOption) {
            ItemSortOption.DEFAULT -> filtered
            ItemSortOption.NAME -> filtered.sortedBy {
                if (preferences.language == AppLanguage.AR) it.nameAr else it.nameEn
            }
            ItemSortOption.WEIGHT_ASC -> filtered.sortedBy { it.weightGrams }
            ItemSortOption.WEIGHT_DESC -> filtered.sortedByDescending { it.weightGrams }
        }
    }

    fun sortedSavedCharacters(): List<SavedCharacter> {
        return when (characterSortOption) {
            CharacterSortOption.RECENT -> savedCharacters.sortedByDescending { it.updatedAt }
            CharacterSortOption.NAME -> savedCharacters.sortedBy { it.name.lowercase() }
        }
    }

    fun currentTripChecklist(): List<ChecklistEntry> =
        checklistEntries.filter { it.tripType == selectedTripType }
}

class TrailKitViewModel(
    private val storageService: StorageService
) : ViewModel() {

    companion object {
        private const val MAX_HISTORY = 35

        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TrailKitViewModel(StorageService(context.applicationContext)) as T
                }
            }
    }

    private val _uiState = MutableStateFlow(TrailKitUiState())
    val uiState: StateFlow<TrailKitUiState> = _uiState.asStateFlow()

    init {
        val loaded = storageService.loadState()
        val initialSlot = loaded.activeConfig.orderedLayers().lastOrNull()?.slot
        val initialItem = initialSlot?.let { loaded.activeConfig.equippedLayers[it]?.itemId }
            ?: EquipmentCatalog.items.first().id
        _uiState.value = TrailKitUiState(
            activeCharacterId = loaded.activeCharacterId,
            activeCharacterName = loaded.activeCharacterName,
            characterConfig = loaded.activeConfig,
            selectedItemId = initialItem,
            selectedSlot = initialSlot,
            savedCharacters = loaded.savedCharacters,
            checklistEntries = loaded.checklistEntries,
            preferences = loaded.preferences,
            expeditionWorld = ExpeditionAiEngine.createInitialWorld(
                difficulty = AiDifficulty.RANGER,
                existingGrid = loaded.inventoryGrid
            ),
            progression = loaded.progression
        )
    }

    fun navigateTo(section: MainSection) {
        _uiState.update { it.copy(currentSection = section, statusBannerMessage = null) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    // --- Undo / Redo / Config Mutation Helper ---

    private fun updateCharacterConfig(
        newConfig: CharacterConfig,
        selectedItemId: String? = _uiState.value.selectedItemId,
        selectedSlot: LayerSlot? = _uiState.value.selectedSlot
    ) {
        val current = _uiState.value
        if (newConfig == current.characterConfig) return

        val updatedUndo = (current.undoStack + current.characterConfig).takeLast(MAX_HISTORY)
        storageService.saveActiveCharacter(
            id = current.activeCharacterId,
            name = current.activeCharacterName,
            config = newConfig
        )

        // Also keep the corresponding savedCharacter entry synced if it exists in savedCharacters
        val updatedSavedList = current.savedCharacters.map { ch ->
            if (ch.id == current.activeCharacterId) {
                ch.copy(config = newConfig, updatedAt = System.currentTimeMillis())
            } else {
                ch
            }
        }
        storageService.saveCharactersList(updatedSavedList)

        _uiState.update {
            it.copy(
                characterConfig = newConfig,
                undoStack = updatedUndo,
                redoStack = emptyList(),
                selectedItemId = selectedItemId,
                selectedSlot = selectedSlot,
                savedCharacters = updatedSavedList
            )
        }
    }

    fun undo() {
        val current = _uiState.value
        if (current.undoStack.isEmpty()) return
        val previous = current.undoStack.last()
        val remainingUndo = current.undoStack.dropLast(1)
        val updatedRedo = (current.redoStack + current.characterConfig).takeLast(MAX_HISTORY)

        storageService.saveActiveCharacter(current.activeCharacterId, current.activeCharacterName, previous)
        val updatedSaved = current.savedCharacters.map {
            if (it.id == current.activeCharacterId) it.copy(config = previous, updatedAt = System.currentTimeMillis()) else it
        }
        storageService.saveCharactersList(updatedSaved)

        _uiState.update {
            it.copy(
                characterConfig = previous,
                undoStack = remainingUndo,
                redoStack = updatedRedo,
                savedCharacters = updatedSaved
            )
        }
    }

    fun redo() {
        val current = _uiState.value
        if (current.redoStack.isEmpty()) return
        val next = current.redoStack.last()
        val remainingRedo = current.redoStack.dropLast(1)
        val updatedUndo = (current.undoStack + current.characterConfig).takeLast(MAX_HISTORY)

        storageService.saveActiveCharacter(current.activeCharacterId, current.activeCharacterName, next)
        val updatedSaved = current.savedCharacters.map {
            if (it.id == current.activeCharacterId) it.copy(config = next, updatedAt = System.currentTimeMillis()) else it
        }
        storageService.saveCharactersList(updatedSaved)

        _uiState.update {
            it.copy(
                characterConfig = next,
                undoStack = updatedUndo,
                redoStack = remainingRedo,
                savedCharacters = updatedSaved
            )
        }
    }

    fun resetCharacterToBareDefault() {
        val current = _uiState.value
        val bareConfig = CharacterConfig(
            skinColorHex = "#E0B187",
            hairStyle = 0,
            hairColorHex = "#3B271A",
            scene = current.characterConfig.scene,
            equippedLayers = emptyMap()
        )
        updateCharacterConfig(bareConfig, selectedSlot = null)
    }

    fun applyPreset(preset: CharacterPreset) {
        val firstSlot = preset.config.orderedLayers().lastOrNull()?.slot
        val firstItem = firstSlot?.let { preset.config.equippedLayers[it]?.itemId }
        updateCharacterConfig(
            newConfig = preset.config,
            selectedItemId = firstItem,
            selectedSlot = firstSlot
        )
    }

    // --- Base Character Appearance ---

    fun setSkinColor(hex: String) {
        val clean = StorageService.sanitizeHex(hex, _uiState.value.characterConfig.skinColorHex)
        updateCharacterConfig(_uiState.value.characterConfig.copy(skinColorHex = clean))
    }

    fun setHairStyle(styleIndex: Int) {
        updateCharacterConfig(_uiState.value.characterConfig.copy(hairStyle = styleIndex.coerceIn(0, 3)))
    }

    fun setHairColor(hex: String) {
        val clean = StorageService.sanitizeHex(hex, _uiState.value.characterConfig.hairColorHex)
        updateCharacterConfig(_uiState.value.characterConfig.copy(hairColorHex = clean))
    }

    fun setSceneBackground(scene: SceneBackground) {
        updateCharacterConfig(_uiState.value.characterConfig.copy(scene = scene))
    }

    // --- Equipment Library Filtering & Actions ---

    fun selectCategory(category: EquipmentCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setItemSortOption(sortOption: ItemSortOption) {
        _uiState.update { it.copy(itemSortOption = sortOption) }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedCategory = EquipmentCategory.ALL,
                searchQuery = "",
                itemSortOption = ItemSortOption.DEFAULT
            )
        }
    }

    fun selectItem(itemId: String) {
        val current = _uiState.value
        val equippedEntry = current.characterConfig.equippedLayers.entries.find { it.value.itemId == itemId }
        val item = EquipmentCatalog.findById(itemId)
        _uiState.update {
            it.copy(
                selectedItemId = itemId,
                selectedSlot = equippedEntry?.key ?: item?.defaultSlot
            )
        }
    }

    fun selectLayerSlot(slot: LayerSlot) {
        val current = _uiState.value
        val layer = current.characterConfig.equippedLayers[slot]
        _uiState.update {
            it.copy(
                selectedSlot = slot,
                selectedItemId = layer?.itemId ?: it.selectedItemId
            )
        }
    }

    fun toggleEquipItem(itemId: String) {
        val current = _uiState.value
        val item = EquipmentCatalog.findById(itemId) ?: return
        val existingEntry = current.characterConfig.equippedLayers.entries.find { it.value.itemId == itemId }

        if (existingEntry != null) {
            // Unequip only this item
            val updatedLayers = current.characterConfig.equippedLayers.toMutableMap()
            updatedLayers.remove(existingEntry.key)
            updateCharacterConfig(
                newConfig = current.characterConfig.copy(equippedLayers = updatedLayers),
                selectedItemId = itemId,
                selectedSlot = null
            )
        } else {
            // Equip in defaultSlot (or preserve existing zOrder if replacing same slot)
            val targetSlot = item.defaultSlot
            val previousInSlot = current.characterConfig.equippedLayers[targetSlot]
            val updatedLayers = current.characterConfig.equippedLayers.toMutableMap()
            updatedLayers[targetSlot] = EquippedLayer(
                slot = targetSlot,
                itemId = item.id,
                colorHex = item.colors.firstOrNull() ?: "#3D6B52",
                visible = true,
                zOrder = previousInSlot?.zOrder ?: targetSlot.defaultZOrder
            )
            updateCharacterConfig(
                newConfig = current.characterConfig.copy(equippedLayers = updatedLayers),
                selectedItemId = item.id,
                selectedSlot = targetSlot
            )
        }
    }

    fun equipItem(itemId: String, colorHex: String? = null) {
        val current = _uiState.value
        val item = EquipmentCatalog.findById(itemId) ?: return
        val existingSlot = current.characterConfig.equippedLayers.entries
            .find { it.value.itemId == itemId }?.key
        val targetSlot = existingSlot ?: item.defaultSlot
        val existingLayer = current.characterConfig.equippedLayers[targetSlot]
        val chosenColor = colorHex
            ?: (if (existingLayer?.itemId == itemId) existingLayer.colorHex else item.colors.firstOrNull())
            ?: "#3D6B52"

        val updatedLayers = current.characterConfig.equippedLayers.toMutableMap()
        updatedLayers[targetSlot] = EquippedLayer(
            slot = targetSlot,
            itemId = item.id,
            colorHex = StorageService.sanitizeHex(chosenColor, "#3D6B52"),
            visible = existingLayer?.visible ?: true,
            zOrder = existingLayer?.zOrder ?: targetSlot.defaultZOrder
        )
        updateCharacterConfig(
            newConfig = current.characterConfig.copy(equippedLayers = updatedLayers),
            selectedItemId = item.id,
            selectedSlot = targetSlot
        )
    }

    fun unequipSlot(slot: LayerSlot) {
        val current = _uiState.value
        if (!current.characterConfig.equippedLayers.containsKey(slot)) return
        val updated = current.characterConfig.equippedLayers.toMutableMap()
        updated.remove(slot)
        updateCharacterConfig(
            newConfig = current.characterConfig.copy(equippedLayers = updated),
            selectedSlot = null
        )
    }

    fun clearCategoryItems(category: EquipmentCategory) {
        val current = _uiState.value
        val updated = current.characterConfig.equippedLayers.filterValues { layer ->
            val item = EquipmentCatalog.findById(layer.itemId)
            if (category == EquipmentCategory.ALL) false else item?.category != category
        }
        updateCharacterConfig(current.characterConfig.copy(equippedLayers = updated))
    }

    fun setLayerColor(slot: LayerSlot, colorHex: String) {
        val current = _uiState.value
        val existing = current.characterConfig.equippedLayers[slot] ?: return
        val cleanHex = StorageService.sanitizeHex(colorHex, existing.colorHex)
        val updated = current.characterConfig.equippedLayers.toMutableMap()
        updated[slot] = existing.copy(colorHex = cleanHex)
        updateCharacterConfig(
            newConfig = current.characterConfig.copy(equippedLayers = updated),
            selectedItemId = existing.itemId,
            selectedSlot = slot
        )
    }

    fun toggleLayerVisibility(slot: LayerSlot) {
        val current = _uiState.value
        val existing = current.characterConfig.equippedLayers[slot] ?: return
        val updated = current.characterConfig.equippedLayers.toMutableMap()
        // Hiding a layer preserves all its saved data!
        updated[slot] = existing.copy(visible = !existing.visible)
        updateCharacterConfig(
            newConfig = current.characterConfig.copy(equippedLayers = updated),
            selectedItemId = existing.itemId,
            selectedSlot = slot
        )
    }

    fun moveLayerOrder(slot: LayerSlot, moveUp: Boolean) {
        val current = _uiState.value
        val ordered = current.characterConfig.orderedLayers().toMutableList()
        val index = ordered.indexOfFirst { it.slot == slot }
        if (index == -1) return
        val targetIndex = if (moveUp) index + 1 else index - 1
        if (targetIndex !in ordered.indices) return

        // Swap the two adjacent layers in the ordered list and assign clean monotonic zOrders
        val temp = ordered[index]
        ordered[index] = ordered[targetIndex]
        ordered[targetIndex] = temp

        val updatedMap = mutableMapOf<LayerSlot, EquippedLayer>()
        ordered.forEachIndexed { idx, layer ->
            // Keep backpack/camp behind body if they haven't crossed BASE_BODY_Z, otherwise monotonic step
            val newZ = if (layer.slot == LayerSlot.CAMP_GEAR || layer.slot == LayerSlot.BACKPACK) {
                layer.zOrder + (if (layer.slot == slot) (if (moveUp) 6 else -6) else 0)
            } else {
                25 + (idx * 6)
            }
            updatedMap[layer.slot] = layer.copy(zOrder = newZ.coerceIn(5, 180))
        }
        updateCharacterConfig(
            newConfig = current.characterConfig.copy(equippedLayers = updatedMap),
            selectedSlot = slot
        )
    }

    fun moveItemToSlot(fromSlot: LayerSlot, toSlot: LayerSlot) {
        if (fromSlot == toSlot) return
        val current = _uiState.value
        val movingLayer = current.characterConfig.equippedLayers[fromSlot] ?: return
        val updated = current.characterConfig.equippedLayers.toMutableMap()
        val displacedLayer = updated[toSlot]

        updated[toSlot] = movingLayer.copy(slot = toSlot, zOrder = toSlot.defaultZOrder)
        if (displacedLayer != null) {
            updated[fromSlot] = displacedLayer.copy(slot = fromSlot, zOrder = fromSlot.defaultZOrder)
        } else {
            updated.remove(fromSlot)
        }

        updateCharacterConfig(
            newConfig = current.characterConfig.copy(equippedLayers = updated),
            selectedItemId = movingLayer.itemId,
            selectedSlot = toSlot
        )
    }

    // --- Saved Characters Management ---

    fun setCharacterSortOption(option: CharacterSortOption) {
        _uiState.update { it.copy(characterSortOption = option) }
    }

    fun setPreviewCharacter(character: SavedCharacter?) {
        _uiState.update { it.copy(previewingCharacter = character) }
    }

    fun saveCurrentCharacter(customName: String? = null) {
        val current = _uiState.value
        val strings = AppStrings.get(current.preferences.language)
        val cleanName = customName?.trim()?.takeIf { it.isNotBlank() } ?: current.activeCharacterName
        val now = System.currentTimeMillis()

        val existingIdx = current.savedCharacters.indexOfFirst { it.id == current.activeCharacterId }
        val updatedList = current.savedCharacters.toMutableList()
        if (existingIdx >= 0) {
            updatedList[existingIdx] = SavedCharacter(
                id = current.activeCharacterId,
                name = cleanName,
                updatedAt = now,
                config = current.characterConfig
            )
        } else {
            updatedList.add(
                0,
                SavedCharacter(
                    id = current.activeCharacterId,
                    name = cleanName,
                    updatedAt = now,
                    config = current.characterConfig
                )
            )
        }

        storageService.saveActiveCharacter(current.activeCharacterId, cleanName, current.characterConfig)
        storageService.saveCharactersList(updatedList)

        _uiState.update {
            it.copy(
                activeCharacterName = cleanName,
                savedCharacters = updatedList,
                statusBannerMessage = strings.msgCharacterSaved
            )
        }
    }

    fun createNewCharacter(name: String, basePreset: CharacterPreset? = null) {
        val current = _uiState.value
        val strings = AppStrings.get(current.preferences.language)
        val cleanName = name.trim().ifBlank {
            if (current.preferences.language == AppLanguage.AR) "مستكشف جديد" else "New Explorer"
        }
        val newId = "char-${System.currentTimeMillis()}-${(100..999).random()}"
        val config = basePreset?.config ?: EquipmentCatalog.defaultCharacterConfig()
        val newSaved = SavedCharacter(
            id = newId,
            name = cleanName,
            updatedAt = System.currentTimeMillis(),
            config = config
        )
        val updatedList = listOf(newSaved) + current.savedCharacters
        storageService.saveCharactersList(updatedList)
        storageService.saveActiveCharacter(newId, cleanName, config)

        _uiState.update {
            it.copy(
                activeCharacterId = newId,
                activeCharacterName = cleanName,
                characterConfig = config,
                undoStack = emptyList(),
                redoStack = emptyList(),
                savedCharacters = updatedList,
                currentSection = MainSection.STUDIO,
                statusBannerMessage = strings.msgCharacterSaved
            )
        }
    }

    fun openSavedCharacter(characterId: String) {
        val current = _uiState.value
        val strings = AppStrings.get(current.preferences.language)
        val target = current.savedCharacters.find { it.id == characterId } ?: return
        storageService.saveActiveCharacter(target.id, target.name, target.config)
        val firstSlot = target.config.orderedLayers().lastOrNull()?.slot
        val firstItem = firstSlot?.let { target.config.equippedLayers[it]?.itemId }

        _uiState.update {
            it.copy(
                activeCharacterId = target.id,
                activeCharacterName = target.name,
                characterConfig = target.config,
                undoStack = emptyList(),
                redoStack = emptyList(),
                selectedSlot = firstSlot,
                selectedItemId = firstItem ?: it.selectedItemId,
                previewingCharacter = null,
                currentSection = MainSection.STUDIO,
                statusBannerMessage = strings.msgCharacterLoaded
            )
        }
    }

    fun renameSavedCharacter(characterId: String, newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) return
        val current = _uiState.value
        val strings = AppStrings.get(current.preferences.language)
        val now = System.currentTimeMillis()
        val updatedList = current.savedCharacters.map { ch ->
            if (ch.id == characterId) ch.copy(name = cleanName, updatedAt = now) else ch
        }
        storageService.saveCharactersList(updatedList)

        val newActiveName = if (current.activeCharacterId == characterId) {
            storageService.saveActiveCharacter(current.activeCharacterId, cleanName, current.characterConfig)
            cleanName
        } else {
            current.activeCharacterName
        }

        _uiState.update {
            it.copy(
                activeCharacterName = newActiveName,
                savedCharacters = updatedList,
                statusBannerMessage = strings.msgCharacterRenamed
            )
        }
    }

    fun duplicateSavedCharacter(characterId: String) {
        val current = _uiState.value
        val strings = AppStrings.get(current.preferences.language)
        val original = current.savedCharacters.find { it.id == characterId } ?: return
        val copySuffix = if (current.preferences.language == AppLanguage.AR) " (نسخة)" else " (Copy)"
        val duplicated = SavedCharacter(
            id = "char-copy-${System.currentTimeMillis()}-${(100..999).random()}",
            name = original.name + copySuffix,
            updatedAt = System.currentTimeMillis(),
            config = original.config
        )
        val updatedList = listOf(duplicated) + current.savedCharacters
        storageService.saveCharactersList(updatedList)
        _uiState.update {
            it.copy(
                savedCharacters = updatedList,
                statusBannerMessage = strings.msgCharacterDuplicated
            )
        }
    }

    fun deleteSavedCharacter(characterId: String) {
        val current = _uiState.value
        val strings = AppStrings.get(current.preferences.language)
        val remaining = current.savedCharacters.filterNot { it.id == characterId }
        val finalSaved = if (remaining.isEmpty()) {
            val fallback = SavedCharacter(
                id = "char-default-${System.currentTimeMillis()}",
                name = if (current.preferences.language == AppLanguage.AR) "حارس الغابة" else "Forest Ranger",
                updatedAt = System.currentTimeMillis(),
                config = EquipmentCatalog.defaultCharacterConfig()
            )
            listOf(fallback)
        } else {
            remaining
        }
        storageService.saveCharactersList(finalSaved)

        val activeRemoved = current.activeCharacterId == characterId
        val nextActive = if (activeRemoved) finalSaved.first() else null
        if (nextActive != null) {
            storageService.saveActiveCharacter(nextActive.id, nextActive.name, nextActive.config)
        }

        _uiState.update {
            it.copy(
                activeCharacterId = nextActive?.id ?: it.activeCharacterId,
                activeCharacterName = nextActive?.name ?: it.activeCharacterName,
                characterConfig = nextActive?.config ?: it.characterConfig,
                savedCharacters = finalSaved,
                previewingCharacter = if (it.previewingCharacter?.id == characterId) null else it.previewingCharacter,
                statusBannerMessage = strings.msgCharacterDeleted
            )
        }
    }

    // --- Equipment Checklist ---

    fun selectTripType(tripType: TripType) {
        _uiState.update { it.copy(selectedTripType = tripType) }
    }

    fun toggleChecklistItem(entryId: String) {
        val current = _uiState.value
        val updated = current.checklistEntries.map { item ->
            if (item.id == entryId) item.copy(isPacked = !item.isPacked) else item
        }
        storageService.saveChecklist(updated)
        _uiState.update { it.copy(checklistEntries = updated) }
    }

    fun addCustomChecklistItem(title: String, weightGrams: Int = 200) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val current = _uiState.value
        val newEntry = ChecklistEntry(
            id = "chk-custom-${System.currentTimeMillis()}-${(100..999).random()}",
            tripType = current.selectedTripType,
            titleAr = cleanTitle,
            titleEn = cleanTitle,
            weightGrams = weightGrams.coerceIn(10, 20000),
            isPacked = false,
            isCustom = true
        )
        val updated = current.checklistEntries + newEntry
        storageService.saveChecklist(updated)
        _uiState.update { it.copy(checklistEntries = updated) }
    }

    fun removeChecklistItem(entryId: String) {
        val current = _uiState.value
        val updated = current.checklistEntries.filterNot { it.id == entryId }
        storageService.saveChecklist(updated)
        _uiState.update { it.copy(checklistEntries = updated) }
    }

    fun resetCurrentTripChecklist() {
        val current = _uiState.value
        val trip = current.selectedTripType
        val otherTrips = current.checklistEntries.filterNot { it.tripType == trip }
        val defaultsForTrip = EquipmentCatalog.defaultChecklistFor(trip).map { it.copy(isPacked = false) }
        val updated = otherTrips + defaultsForTrip
        storageService.saveChecklist(updated)
        _uiState.update { it.copy(checklistEntries = updated) }
    }

    // --- Export & Import ---

    fun triggerPngExport(context: Context?, useNeutralBackground: Boolean = false) {
        val current = _uiState.value
        val strings = AppStrings.get(current.preferences.language)
        when (val res = ExportService.exportCharacterPng(
            context = context,
            characterName = current.activeCharacterName,
            config = current.characterConfig,
            useNeutralBackground = useNeutralBackground
        )) {
            is PngExportResult.Success -> {
                _uiState.update {
                    it.copy(
                        exportedPngResult = res,
                        statusBannerMessage = strings.msgExportPngSuccess
                    )
                }
            }
            is PngExportResult.Error -> {
                _uiState.update {
                    it.copy(statusBannerMessage = res.message(current.preferences.language))
                }
            }
        }
    }

    fun dismissPngExportDialog() {
        _uiState.update { it.copy(exportedPngResult = null) }
    }

    fun prepareJsonExport(character: SavedCharacter? = null) {
        val current = _uiState.value
        val strings = AppStrings.get(current.preferences.language)
        val targetName = character?.name ?: current.activeCharacterName
        val targetConfig = character?.config ?: current.characterConfig
        val json = ExportService.exportCharacterJson(targetName, targetConfig)
        _uiState.update {
            it.copy(
                exportedJsonPreview = json,
                statusBannerMessage = strings.msgExportJsonSuccess
            )
        }
    }

    fun dismissJsonExportDialog() {
        _uiState.update { it.copy(exportedJsonPreview = null) }
    }

    fun clearImportError() {
        _uiState.update { it.copy(importErrorMessage = null) }
    }

    fun importCharacterFromJson(rawJson: String): Boolean {
        val current = _uiState.value
        val strings = AppStrings.get(current.preferences.language)
        return when (val result = ExportService.validateAndParseCharacterJson(rawJson, current.savedCharacters)) {
            is ImportValidationResult.Success -> {
                val imported = result.character
                val updatedList = listOf(imported) + current.savedCharacters
                storageService.saveCharactersList(updatedList)
                storageService.saveActiveCharacter(imported.id, imported.name, imported.config)
                _uiState.update {
                    it.copy(
                        activeCharacterId = imported.id,
                        activeCharacterName = imported.name,
                        characterConfig = imported.config,
                        savedCharacters = updatedList,
                        importErrorMessage = null,
                        statusBannerMessage = strings.msgImportSuccess
                    )
                }
                true
            }
            is ImportValidationResult.Error -> {
                _uiState.update {
                    it.copy(importErrorMessage = result.message(current.preferences.language))
                }
                false
            }
        }
    }

    // --- Settings & Preferences ---

    fun setLanguage(language: AppLanguage) {
        val updated = _uiState.value.preferences.copy(language = language)
        storageService.savePreferences(updated)
        _uiState.update { it.copy(preferences = updated) }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        val updated = _uiState.value.preferences.copy(themeMode = themeMode)
        storageService.savePreferences(updated)
        _uiState.update { it.copy(preferences = updated) }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        val updated = _uiState.value.preferences.copy(animationsEnabled = enabled)
        storageService.savePreferences(updated)
        _uiState.update { it.copy(preferences = updated) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        val updated = _uiState.value.preferences.copy(soundEnabled = enabled)
        storageService.savePreferences(updated)
        _uiState.update { it.copy(preferences = updated) }
    }

    fun clearAllLocalData() {
        val fresh = storageService.clearAllData()
        val strings = AppStrings.get(fresh.preferences.language)
        _uiState.value = TrailKitUiState(
            currentSection = MainSection.SETTINGS,
            activeCharacterId = fresh.activeCharacterId,
            activeCharacterName = fresh.activeCharacterName,
            characterConfig = fresh.activeConfig,
            savedCharacters = fresh.savedCharacters,
            checklistEntries = fresh.checklistEntries,
            preferences = fresh.preferences,
            expeditionWorld = ExpeditionAiEngine.createInitialWorld(existingGrid = fresh.inventoryGrid),
            progression = fresh.progression,
            statusBannerMessage = strings.msgDataCleared
        )
    }

    // --- Single-Player Expedition & Slot Grid Inventory Actions ---

    fun movePlayerInExpedition(dx: Int, dy: Int) {
        val current = _uiState.value
        val wasComplete = current.expeditionWorld.isMissionComplete
        val nextWorld = ExpeditionAiEngine.stepPlayerMove(current.expeditionWorld, dx, dy)
        storageService.saveInventoryGrid(nextWorld.inventoryGrid)

        var nextProg = current.progression
        if (!wasComplete && nextWorld.isMissionComplete) {
            nextProg = current.progression.copy(
                missionsCompleted = current.progression.missionsCompleted + 1,
                totalCachesSecured = current.progression.totalCachesSecured + nextWorld.cachesCollected,
                highestDifficultyWon = nextWorld.difficulty.id
            )
            storageService.saveProgression(nextProg)
        }

        _uiState.update {
            it.copy(
                expeditionWorld = nextWorld,
                progression = nextProg
            )
        }
    }

    fun stepTowardMapTile(target: GridPoint) {
        val currentWorld = _uiState.value.expeditionWorld
        if (currentWorld.isMissionComplete || currentWorld.isPlayerDefeated) return
        val path = ExpeditionAiEngine.findPathAStar(
            tiles = currentWorld.tiles,
            start = currentWorld.playerPos,
            goal = target
        )
        val firstStep = path.firstOrNull() ?: return
        val dx = (firstStep.x - currentWorld.playerPos.x).coerceIn(-1, 1)
        val dy = (firstStep.y - currentWorld.playerPos.y).coerceIn(-1, 1)
        movePlayerInExpedition(dx, dy)
    }

    fun throwExpeditionDecoy() {
        val nextWorld = ExpeditionAiEngine.throwAcousticDecoy(_uiState.value.expeditionWorld)
        _uiState.update { it.copy(expeditionWorld = nextWorld) }
    }

    fun triggerExpeditionStrobe() {
        val nextWorld = ExpeditionAiEngine.triggerFlashlightStrobe(_uiState.value.expeditionWorld)
        _uiState.update { it.copy(expeditionWorld = nextWorld) }
    }

    fun setAiDifficulty(difficulty: AiDifficulty) {
        val currentGrid = _uiState.value.expeditionWorld.inventoryGrid
        val resetWorld = ExpeditionAiEngine.createInitialWorld(
            difficulty = difficulty,
            existingGrid = currentGrid
        )
        _uiState.update { it.copy(expeditionWorld = resetWorld) }
    }

    fun resetExpedition() {
        val current = _uiState.value.expeditionWorld
        val freshWorld = ExpeditionAiEngine.createInitialWorld(
            difficulty = current.difficulty,
            existingGrid = current.inventoryGrid
        )
        _uiState.update { it.copy(expeditionWorld = freshWorld) }
    }

    fun selectOrSwapGridSlot(slotIndex: Int) {
        val currentWorld = _uiState.value.expeditionWorld
        val currentlySelected = currentWorld.selectedGridSlotIndex
        if (currentlySelected == null) {
            _uiState.update {
                it.copy(expeditionWorld = currentWorld.copy(selectedGridSlotIndex = slotIndex))
            }
        } else if (currentlySelected == slotIndex) {
            _uiState.update {
                it.copy(expeditionWorld = currentWorld.copy(selectedGridSlotIndex = null))
            }
        } else {
            val updatedGrid = ExpeditionAiEngine.moveOrSwapGridSlots(
                grid = currentWorld.inventoryGrid,
                fromIndex = currentlySelected,
                toIndex = slotIndex
            )
            storageService.saveInventoryGrid(updatedGrid)
            _uiState.update {
                it.copy(
                    expeditionWorld = currentWorld.copy(
                        inventoryGrid = updatedGrid,
                        selectedGridSlotIndex = null
                    )
                )
            }
        }
    }

    fun moveGridSlotDragDrop(fromIndex: Int, toIndex: Int) {
        val currentWorld = _uiState.value.expeditionWorld
        val updatedGrid = ExpeditionAiEngine.moveOrSwapGridSlots(
            grid = currentWorld.inventoryGrid,
            fromIndex = fromIndex,
            toIndex = toIndex
        )
        storageService.saveInventoryGrid(updatedGrid)
        _uiState.update {
            it.copy(
                expeditionWorld = currentWorld.copy(
                    inventoryGrid = updatedGrid,
                    selectedGridSlotIndex = null
                )
            )
        }
    }

    fun consumeGridSlotItem(slotIndex: Int) {
        val current = _uiState.value
        val cell = current.expeditionWorld.inventoryGrid.getOrNull(slotIndex)
        val nextWorld = ExpeditionAiEngine.consumeSupplyFromGrid(current.expeditionWorld, slotIndex)
        storageService.saveInventoryGrid(nextWorld.inventoryGrid)

        // Heal player in continuous survival game simulation!
        val curPlayer = current.survivalGameplay.player
        var newHealth = curPlayer.health
        var newStamina = curPlayer.stamina
        val itemId = cell?.itemId.orEmpty()
        if (itemId.startsWith("aid-")) {
            newHealth = (newHealth + 35).coerceAtMost(100)
        } else if (itemId.startsWith("water-")) {
            newStamina = (newStamina + 50f).coerceAtMost(100f)
            newHealth = (newHealth + 10).coerceAtMost(100)
        } else if (itemId.startsWith("food-")) {
            newStamina = (newStamina + 40f).coerceAtMost(100f)
            newHealth = (newHealth + 15).coerceAtMost(100)
        }

        _uiState.update {
            it.copy(
                expeditionWorld = nextWorld,
                survivalGameplay = current.survivalGameplay.copy(
                    player = curPlayer.copy(health = newHealth, stamina = newStamina)
                )
            )
        }
    }

    fun packCatalogItemIntoGrid(itemId: String) {
        val currentWorld = _uiState.value.expeditionWorld
        val updatedGrid = ExpeditionAiEngine.addItemToGrid(currentWorld.inventoryGrid, itemId, 1)
        storageService.saveInventoryGrid(updatedGrid)
        _uiState.update {
            it.copy(expeditionWorld = currentWorld.copy(inventoryGrid = updatedGrid))
        }
    }

    // --- High-Fidelity Continuous Survival Game Actions ---

    val survivalEngine = SurvivalGameEngine()

    fun updateSurvivalTick(joyX: Float, joyY: Float, isSprinting: Boolean, dt: Float) {
        val current = _uiState.value
        val wasComplete = current.survivalGameplay.isMissionComplete
        val nextState = survivalEngine.update(
            currentState = current.survivalGameplay,
            joystickX = joyX,
            joystickY = joyY,
            isSprinting = isSprinting,
            deltaSeconds = dt
        )

        var nextProg = current.progression
        if (!wasComplete && nextState.isMissionComplete) {
            nextProg = current.progression.copy(
                missionsCompleted = current.progression.missionsCompleted + 1,
                totalCachesSecured = current.progression.totalCachesSecured + nextState.cachesSecuredCount,
                highestDifficultyWon = nextState.difficulty.id
            )
            storageService.saveProgression(nextProg)
        }

        _uiState.update {
            it.copy(
                survivalGameplay = nextState,
                progression = nextProg
            )
        }
    }

    fun collectNearbySurvivalSupply() {
        val current = _uiState.value
        val (nextState, updatedGrid) = survivalEngine.interactCollectSupply(
            state = current.survivalGameplay,
            grid = current.expeditionWorld.inventoryGrid
        )
        storageService.saveInventoryGrid(updatedGrid)
        _uiState.update {
            it.copy(
                survivalGameplay = nextState,
                expeditionWorld = current.expeditionWorld.copy(inventoryGrid = updatedGrid)
            )
        }
    }

    fun triggerSurvivalExtraction() {
        val current = _uiState.value
        val nextState = survivalEngine.triggerExtraction(current.survivalGameplay)
        if (nextState.isMissionComplete && !current.survivalGameplay.isMissionComplete) {
            val nextProg = current.progression.copy(
                missionsCompleted = current.progression.missionsCompleted + 1,
                totalCachesSecured = current.progression.totalCachesSecured + nextState.cachesSecuredCount,
                highestDifficultyWon = nextState.difficulty.id
            )
            storageService.saveProgression(nextProg)
            _uiState.update {
                it.copy(
                    survivalGameplay = nextState,
                    progression = nextProg
                )
            }
        } else {
            _uiState.update { it.copy(survivalGameplay = nextState) }
        }
    }

    fun throwSurvivalDecoy() {
        val current = _uiState.value
        val nextState = survivalEngine.throwDecoy(current.survivalGameplay)
        val nextWorld = ExpeditionAiEngine.throwAcousticDecoy(current.expeditionWorld)
        _uiState.update {
            it.copy(
                survivalGameplay = nextState,
                expeditionWorld = nextWorld
            )
        }
    }

    fun triggerSurvivalStrobe() {
        val current = _uiState.value
        val nextState = survivalEngine.triggerStrobe(current.survivalGameplay)
        val nextWorld = ExpeditionAiEngine.triggerFlashlightStrobe(current.expeditionWorld)
        _uiState.update {
            it.copy(
                survivalGameplay = nextState,
                expeditionWorld = nextWorld
            )
        }
    }

    fun setSurvivalDifficulty(difficulty: AiDifficulty) {
        val resetState = survivalEngine.createInitialState(difficulty = difficulty)
        val resetWorld = ExpeditionAiEngine.createInitialWorld(
            difficulty = difficulty,
            existingGrid = _uiState.value.expeditionWorld.inventoryGrid
        )
        _uiState.update {
            it.copy(
                survivalGameplay = resetState,
                expeditionWorld = resetWorld
            )
        }
    }

    fun resetSurvivalGame() {
        val current = _uiState.value
        val resetState = survivalEngine.createInitialState(difficulty = current.survivalGameplay.difficulty)
        val resetWorld = ExpeditionAiEngine.createInitialWorld(
            difficulty = current.expeditionWorld.difficulty,
            existingGrid = current.expeditionWorld.inventoryGrid
        )
        _uiState.update {
            it.copy(
                survivalGameplay = resetState,
                expeditionWorld = resetWorld
            )
        }
    }

    fun toggleSurvivalFlashlight() {
        val current = _uiState.value
        val p = current.survivalGameplay.player
        val updatedP = p.copy(flashlightOn = !p.flashlightOn)
        _uiState.update {
            it.copy(
                survivalGameplay = current.survivalGameplay.copy(player = updatedP)
            )
        }
    }
}
