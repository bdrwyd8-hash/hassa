package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.EquipmentCatalog
import com.example.localization.AppStrings
import com.example.model.CharacterPreset
import com.example.model.CharacterSortOption
import com.example.model.SavedCharacter
import com.example.ui.canvas.CharacterPreviewCanvas
import com.example.ui.components.JsonImportDialog
import com.example.viewmodel.TrailKitUiState
import com.example.viewmodel.TrailKitViewModel

@Composable
fun SavedCharactersScreen(
    uiState: TrailKitUiState,
    viewModel: TrailKitViewModel,
    modifier: Modifier = Modifier
) {
    val language = uiState.preferences.language
    val strings = AppStrings.get(language)
    val sortedCharacters = remember(uiState.savedCharacters, uiState.characterSortOption) {
        uiState.sortedSavedCharacters()
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newCharName by remember { mutableStateOf("") }
    var selectedPresetForNew by remember { mutableStateOf<CharacterPreset?>(EquipmentCatalog.presets.first()) }

    var renamingCharacter by remember { mutableStateOf<SavedCharacter?>(null) }
    var renameText by remember { mutableStateOf("") }

    var deletingCharacter by remember { mutableStateOf<SavedCharacter?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("saved_characters_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Primary Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.myCharactersTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        viewModel.clearImportError()
                        showImportDialog = true
                    },
                    modifier = Modifier.testTag("open_import_json_btn")
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.importJsonBtn, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        newCharName = ""
                        showCreateDialog = true
                    },
                    modifier = Modifier.testTag("create_new_character_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.createNewCharacter, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Sort & Save Current Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.sortByLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CharacterSortOption.entries.forEach { sortOpt ->
                    FilterChip(
                        selected = uiState.characterSortOption == sortOpt,
                        onClick = { viewModel.setCharacterSortOption(sortOpt) },
                        label = { Text(sortOpt.label(language), style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.testTag("char_sort_${sortOpt.name}")
                    )
                }
            }

            OutlinedButton(
                onClick = { viewModel.saveCurrentCharacter() },
                modifier = Modifier.testTag("save_active_character_btn")
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.saveCharacterBtn, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Saved Characters Grid
        if (sortedCharacters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.noSavedCharacters,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 240.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("saved_characters_grid")
            ) {
                items(sortedCharacters, key = { it.id }) { character ->
                    val isActive = character.id == uiState.activeCharacterId
                    val weight = character.config.equippedLayers.values
                        .filter { it.visible }
                        .sumOf { EquipmentCatalog.findById(it.itemId)?.weightGrams ?: 0 }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isActive) 2.dp else 1.dp,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .testTag("saved_char_card_${character.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Live 2D Thumbnail Preview
                                CharacterPreviewCanvas(
                                    config = character.config,
                                    modifier = Modifier
                                        .size(width = 86.dp, height = 108.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = character.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${character.config.equippedLayers.size} ${strings.equippedCountLabel} • $weight ${strings.weightUnitGrams}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = character.config.scene.label(language),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { viewModel.openSavedCharacter(character.id) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("open_char_${character.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.openCharacter, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }

                            // Secondary Actions: Preview, Rename, Duplicate, Export JSON, Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.setPreviewCharacter(character) },
                                    modifier = Modifier.testTag("preview_char_${character.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.previewCharacter, style = MaterialTheme.typography.bodySmall)
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            renamingCharacter = character
                                            renameText = character.name
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("rename_char_${character.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = strings.renameCharacter,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.duplicateSavedCharacter(character.id) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("duplicate_char_${character.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = strings.duplicateCharacter,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.prepareJsonExport(character) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("export_json_char_${character.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FileDownload,
                                            contentDescription = strings.exportJsonBtn,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { deletingCharacter = character },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_char_${character.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = strings.deleteCharacter,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create New Character Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(strings.createNewCharacter) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCharName,
                        onValueChange = { newCharName = it },
                        label = { Text(strings.characterNameLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_character_name_input")
                    )
                    Text(
                        text = strings.presetsLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    EquipmentCatalog.presets.forEach { preset ->
                        FilterChip(
                            selected = selectedPresetForNew?.id == preset.id,
                            onClick = { selectedPresetForNew = preset },
                            label = { Text(preset.label(language)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewCharacter(newCharName, selectedPresetForNew)
                        showCreateDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_character_btn")
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Rename Dialog
    val charToRename = renamingCharacter
    if (charToRename != null) {
        AlertDialog(
            onDismissRequest = { renamingCharacter = null },
            title = { Text(strings.renameCharacter) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(strings.characterNameLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_character_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameSavedCharacter(charToRename.id, renameText)
                        renamingCharacter = null
                    },
                    modifier = Modifier.testTag("confirm_rename_character_btn")
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingCharacter = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    val charToDelete = deletingCharacter
    if (charToDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingCharacter = null },
            title = { Text(strings.deleteCharacter) },
            text = { Text("${strings.deleteCharacterConfirm}\n(${charToDelete.name})") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSavedCharacter(charToDelete.id)
                        deletingCharacter = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_character_btn")
                ) {
                    Text(strings.deleteCharacter)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCharacter = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Preview Modal before opening
    val previewChar = uiState.previewingCharacter
    if (previewChar != null) {
        AlertDialog(
            onDismissRequest = { viewModel.setPreviewCharacter(null) },
            title = { Text(previewChar.name) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CharacterPreviewCanvas(
                        config = previewChar.config,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("preview_dialog_canvas")
                    )
                    Text(
                        text = "${previewChar.config.equippedLayers.size} ${strings.equippedCountLabel} • ${previewChar.config.scene.label(language)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.openSavedCharacter(previewChar.id) },
                    modifier = Modifier.testTag("preview_open_in_studio_btn")
                ) {
                    Text(strings.openCharacter)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setPreviewCharacter(null) }) {
                    Text(strings.closeBtn)
                }
            }
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        JsonImportDialog(
            errorMessage = uiState.importErrorMessage,
            strings = strings,
            onValidateAndImport = { raw -> viewModel.importCharacterFromJson(raw) },
            onCancel = {
                viewModel.clearImportError()
                showImportDialog = false
            }
        )
    }
}
