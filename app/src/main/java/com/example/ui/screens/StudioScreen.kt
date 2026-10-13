package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Explore
import com.example.model.AppLanguage
import com.example.model.MainSection
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.EquipmentCatalog
import com.example.localization.AppStrings
import com.example.localization.LocalizedStrings
import com.example.model.EquipmentCategory
import com.example.model.ItemSortOption
import com.example.model.SceneBackground
import com.example.ui.canvas.CharacterStageCard
import com.example.ui.components.BaseAppearanceDialog
import com.example.ui.components.EquipmentItemCard
import com.example.ui.components.LayerManagerDialog
import com.example.ui.components.SelectedItemDetailsPanel
import com.example.viewmodel.TrailKitUiState
import com.example.viewmodel.TrailKitViewModel

@Composable
fun StudioScreen(
    uiState: TrailKitUiState,
    viewModel: TrailKitViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language = uiState.preferences.language
    val strings = AppStrings.get(language)
    val filteredItems = remember(
        uiState.selectedCategory,
        uiState.searchQuery,
        uiState.itemSortOption,
        language
    ) {
        uiState.filteredItems()
    }

    var showResetDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showLayersDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val selectedItem = remember(uiState.selectedItemId) {
        uiState.selectedItemId?.let { EquipmentCatalog.findById(it) }
    }
    val selectedEquippedLayer = remember(uiState.characterConfig, selectedItem, uiState.selectedSlot) {
        if (selectedItem != null) {
            uiState.characterConfig.equippedLayers.values.find { it.itemId == selectedItem.id }
        } else if (uiState.selectedSlot != null) {
            uiState.characterConfig.equippedLayers[uiState.selectedSlot]
        } else {
            null
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("studio_screen")
    ) {
        val isWideLayout = maxWidth >= 700.dp

        if (isWideLayout) {
            // Desktop / Tablet / Landscape 3-Panel Layout:
            // Left: Categories & Presets | Center: Character Canvas | Right: Equipment Library & Item Settings
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Panel 1: Categories & Presets (Left)
                Surface(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Text(
                                text = strings.presetsLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(EquipmentCatalog.presets.size) { idx ->
                            val preset = EquipmentCatalog.presets[idx]
                            OutlinedButton(
                                onClick = { viewModel.applyPreset(preset) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("preset_chip_${preset.id}")
                            ) {
                                Text(preset.label(language), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(EquipmentCategory.entries.size) { idx ->
                            val cat = EquipmentCategory.entries[idx]
                            FilterChip(
                                selected = uiState.selectedCategory == cat,
                                onClick = { viewModel.selectCategory(cat) },
                                label = { Text(cat.label(language)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("category_tab_${cat.id}")
                            )
                        }
                    }
                }

                // Panel 2: Character Canvas & Selected Item Settings (Center)
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CharacterStageCard(
                        characterName = uiState.activeCharacterName,
                        config = uiState.characterConfig,
                        totalWeightGrams = uiState.totalWeightGrams,
                        equippedCount = uiState.equippedCount,
                        selectedSlot = uiState.selectedSlot,
                        canUndo = uiState.canUndo,
                        canRedo = uiState.canRedo,
                        language = language,
                        strings = strings,
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onRequestReset = { showResetDialog = true },
                        onCycleScene = {
                            val scenes = SceneBackground.entries
                            val nextIdx = (scenes.indexOf(uiState.characterConfig.scene) + 1) % scenes.size
                            viewModel.setSceneBackground(scenes[nextIdx])
                        },
                        onOpenAppearanceSheet = { showAppearanceDialog = true },
                        onOpenLayersSheet = { showLayersDialog = true },
                        onSaveCharacter = { viewModel.saveCurrentCharacter() },
                        onExportPng = { viewModel.triggerPngExport(context) },
                        onSelectLayerSlot = { viewModel.selectLayerSlot(it) }
                    )

                    if (selectedItem != null) {
                        SelectedItemDetailsPanel(
                            item = selectedItem,
                            equippedLayer = selectedEquippedLayer,
                            language = language,
                            strings = strings,
                            onEquipOrUnequip = { viewModel.toggleEquipItem(selectedItem.id) },
                            onSelectColor = { hex ->
                                if (selectedEquippedLayer != null) {
                                    viewModel.setLayerColor(selectedEquippedLayer.slot, hex)
                                } else {
                                    viewModel.equipItem(selectedItem.id, hex)
                                }
                            },
                            onToggleVisibility = {
                                selectedEquippedLayer?.let { viewModel.toggleLayerVisibility(it.slot) }
                            },
                            onMoveLayerUp = {
                                selectedEquippedLayer?.let { viewModel.moveLayerOrder(it.slot, true) }
                            },
                            onMoveLayerDown = {
                                selectedEquippedLayer?.let { viewModel.moveLayerOrder(it.slot, false) }
                            },
                            onMoveToSlot = { targetSlot ->
                                selectedEquippedLayer?.let { viewModel.moveItemToSlot(it.slot, targetSlot) }
                            }
                        )
                    }
                }

                // Panel 3: Equipment Library Grid (Right)
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchAndSortBar(
                        searchQuery = uiState.searchQuery,
                        sortOption = uiState.itemSortOption,
                        selectedCategory = uiState.selectedCategory,
                        language = language,
                        strings = strings,
                        showSortMenu = showSortMenu,
                        onToggleSortMenu = { showSortMenu = it },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onSelectSort = { viewModel.setItemSortOption(it) },
                        onClearCategory = { viewModel.clearCategoryItems(uiState.selectedCategory) }
                    )

                    if (filteredItems.isEmpty()) {
                        EmptyEquipmentState(
                            strings = strings,
                            onClearFilters = { viewModel.clearFilters() }
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 145.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("equipment_grid")
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                val equippedLayer = uiState.characterConfig.equippedLayers.values
                                    .find { it.itemId == item.id }
                                EquipmentItemCard(
                                    item = item,
                                    isEquipped = equippedLayer != null,
                                    isSelected = uiState.selectedItemId == item.id,
                                    equippedColorHex = equippedLayer?.colorHex,
                                    language = language,
                                    strings = strings,
                                    onSelect = { viewModel.selectItem(item.id) },
                                    onToggleEquip = { viewModel.toggleEquipItem(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Mobile Portrait Layout:
            // Large visible CharacterStageCard + Presets & Category tabs + Search/Sort + Touch-friendly Item Cards + Compact Item Detail Panel
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CharacterStageCard(
                    characterName = uiState.activeCharacterName,
                    config = uiState.characterConfig,
                    totalWeightGrams = uiState.totalWeightGrams,
                    equippedCount = uiState.equippedCount,
                    selectedSlot = uiState.selectedSlot,
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    language = language,
                    strings = strings,
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onRequestReset = { showResetDialog = true },
                    onCycleScene = {
                        val scenes = SceneBackground.entries
                        val nextIdx = (scenes.indexOf(uiState.characterConfig.scene) + 1) % scenes.size
                        viewModel.setSceneBackground(scenes[nextIdx])
                    },
                    onOpenAppearanceSheet = { showAppearanceDialog = true },
                    onOpenLayersSheet = { showLayersDialog = true },
                    onSaveCharacter = { viewModel.saveCurrentCharacter() },
                    onExportPng = { viewModel.triggerPngExport(context) },
                    onSelectLayerSlot = { viewModel.selectLayerSlot(it) }
                )

                // Quick Launch into Wilderness Survival Adventure
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(MainSection.EXPEDITION) }
                        .testTag("launch_survival_game_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Explore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (language == AppLanguage.AR) "خوض رحلة البقاء بهذا التجهيز" else "Enter Wilderness Survival with this Loadout",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (language == AppLanguage.AR) "مغامرة 2D حية ضد دوريات ذكاء اصطناعي محلي" else "Live 2D real-time survival vs intelligent offline AI bots",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Scrollable Category Tabs + Presets strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("category_tabs_row"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EquipmentCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = uiState.selectedCategory == cat,
                            onClick = { viewModel.selectCategory(cat) },
                            label = {
                                Text(
                                    text = cat.label(language),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (uiState.selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("category_tab_${cat.id}")
                        )
                    }
                }

                SearchAndSortBar(
                    searchQuery = uiState.searchQuery,
                    sortOption = uiState.itemSortOption,
                    selectedCategory = uiState.selectedCategory,
                    language = language,
                    strings = strings,
                    showSortMenu = showSortMenu,
                    onToggleSortMenu = { showSortMenu = it },
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onSelectSort = { viewModel.setItemSortOption(it) },
                    onClearCategory = { viewModel.clearCategoryItems(uiState.selectedCategory) }
                )

                // Equipment Library Grid + Selected Item Details Panel inside smooth scrollable area
                Box(modifier = Modifier.weight(1f)) {
                    if (filteredItems.isEmpty()) {
                        EmptyEquipmentState(
                            strings = strings,
                            onClearFilters = { viewModel.clearFilters() }
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("equipment_grid")
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                val equippedLayer = uiState.characterConfig.equippedLayers.values
                                    .find { it.itemId == item.id }
                                EquipmentItemCard(
                                    item = item,
                                    isEquipped = equippedLayer != null,
                                    isSelected = uiState.selectedItemId == item.id,
                                    equippedColorHex = equippedLayer?.colorHex,
                                    language = language,
                                    strings = strings,
                                    onSelect = { viewModel.selectItem(item.id) },
                                    onToggleEquip = { viewModel.toggleEquipItem(item.id) }
                                )
                            }
                        }
                    }
                }

                // Compact Selected Item Details Bar at bottom of Studio
                if (selectedItem != null) {
                    SelectedItemDetailsPanel(
                        item = selectedItem,
                        equippedLayer = selectedEquippedLayer,
                        language = language,
                        strings = strings,
                        onEquipOrUnequip = { viewModel.toggleEquipItem(selectedItem.id) },
                        onSelectColor = { hex ->
                            if (selectedEquippedLayer != null) {
                                viewModel.setLayerColor(selectedEquippedLayer.slot, hex)
                            } else {
                                viewModel.equipItem(selectedItem.id, hex)
                            }
                        },
                        onToggleVisibility = {
                            selectedEquippedLayer?.let { viewModel.toggleLayerVisibility(it.slot) }
                        },
                        onMoveLayerUp = {
                            selectedEquippedLayer?.let { viewModel.moveLayerOrder(it.slot, true) }
                        },
                        onMoveLayerDown = {
                            selectedEquippedLayer?.let { viewModel.moveLayerOrder(it.slot, false) }
                        },
                        onMoveToSlot = { targetSlot ->
                            selectedEquippedLayer?.let { viewModel.moveItemToSlot(it.slot, targetSlot) }
                        }
                    )
                }
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(strings.resetConfirmTitle) },
            text = { Text(strings.resetConfirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetCharacterToBareDefault()
                        showResetDialog = false
                    },
                    modifier = Modifier.testTag("confirm_reset_character_btn")
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showAppearanceDialog) {
        BaseAppearanceDialog(
            config = uiState.characterConfig,
            presets = EquipmentCatalog.presets,
            language = language,
            strings = strings,
            onApplyPreset = { viewModel.applyPreset(it) },
            onSelectSkin = { viewModel.setSkinColor(it) },
            onSelectHairStyle = { viewModel.setHairStyle(it) },
            onSelectHairColor = { viewModel.setHairColor(it) },
            onSelectScene = { viewModel.setSceneBackground(it) },
            onDismiss = { showAppearanceDialog = false }
        )
    }

    if (showLayersDialog) {
        LayerManagerDialog(
            config = uiState.characterConfig,
            language = language,
            strings = strings,
            onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
            onMoveUp = { viewModel.moveLayerOrder(it, true) },
            onMoveDown = { viewModel.moveLayerOrder(it, false) },
            onRemoveSlot = { viewModel.unequipSlot(it) },
            onDismiss = { showLayersDialog = false }
        )
    }
}

@Composable
private fun SearchAndSortBar(
    searchQuery: String,
    sortOption: ItemSortOption,
    selectedCategory: EquipmentCategory,
    language: com.example.model.AppLanguage,
    strings: LocalizedStrings,
    showSortMenu: Boolean,
    onToggleSortMenu: (Boolean) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectSort: (ItemSortOption) -> Unit,
    onClearCategory: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    text = strings.searchPlaceholder,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = strings.cancel,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("equipment_search_input")
        )

        Box {
            OutlinedButton(
                onClick = { onToggleSortMenu(true) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.testTag("sort_items_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = strings.sortByLabel,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = sortOption.label(language),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { onToggleSortMenu(false) }
            ) {
                ItemSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label(language)) },
                        onClick = {
                            onSelectSort(option)
                            onToggleSortMenu(false)
                        }
                    )
                }
            }
        }

        if (selectedCategory != EquipmentCategory.ALL) {
            IconButton(
                onClick = onClearCategory,
                modifier = Modifier.testTag("clear_category_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = strings.clearCategory,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun EmptyEquipmentState(
    strings: LocalizedStrings,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp)
            .testTag("empty_equipment_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = strings.noItemsFound,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = onClearFilters,
            modifier = Modifier.testTag("reset_filters_btn")
        ) {
            Text(strings.clearFilters)
        }
    }
}
