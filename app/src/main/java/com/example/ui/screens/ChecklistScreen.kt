package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.TripType
import com.example.viewmodel.TrailKitUiState
import com.example.viewmodel.TrailKitViewModel

@Composable
fun ChecklistScreen(
    uiState: TrailKitUiState,
    viewModel: TrailKitViewModel,
    modifier: Modifier = Modifier
) {
    val language = uiState.preferences.language
    val strings = AppStrings.get(language)
    val tripItems = remember(uiState.checklistEntries, uiState.selectedTripType) {
        uiState.currentTripChecklist()
    }

    val packedCount = tripItems.count { it.isPacked }
    val remainingCount = (tripItems.size - packedCount).coerceAtLeast(0)
    val progress = if (tripItems.isNotEmpty()) packedCount.toFloat() / tripItems.size else 0f
    val packedWeight = tripItems.filter { it.isPacked }.sumOf { it.weightGrams }

    var customItemText by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("checklist_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header + Reset Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.checklistTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.testTag("reset_checklist_btn")
            ) {
                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.resetChecklistBtn, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Trip Type Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("trip_type_tabs"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TripType.entries.forEach { trip ->
                FilterChip(
                    selected = uiState.selectedTripType == trip,
                    onClick = { viewModel.selectTripType(trip) },
                    label = {
                        Text(
                            text = trip.label(language),
                            fontWeight = if (uiState.selectedTripType == trip) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("trip_tab_${trip.id}")
                )
            }
        }

        // Summary Progress Card (Packed vs Remaining counts)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checklist_progress_card")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = uiState.selectedTripType.subtitle(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.packedCountLabel}: $packedCount  •  ${strings.remainingCountLabel}: $remainingCount",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("checklist_counts_text")
                    )
                    Text(
                        text = "$packedWeight ${strings.weightUnitGrams}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
        }

        // Add Custom Checklist Item Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customItemText,
                onValueChange = { customItemText = it },
                placeholder = { Text(strings.addCustomItemPlaceholder, style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_checklist_input")
            )

            Button(
                onClick = {
                    if (customItemText.isNotBlank()) {
                        viewModel.addCustomChecklistItem(customItemText)
                        customItemText = ""
                    }
                },
                modifier = Modifier.testTag("add_custom_checklist_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.addCustomItemBtn)
            }
        }

        // Checklist Entries List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("checklist_items_list"),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(tripItems, key = { it.id }) { entry ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (entry.isPacked) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checklist_row_${entry.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = entry.isPacked,
                                onCheckedChange = { viewModel.toggleChecklistItem(entry.id) },
                                modifier = Modifier.testTag("checkbox_${entry.id}")
                            )
                            Column {
                                Text(
                                    text = entry.title(language),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (entry.isPacked) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (entry.isPacked) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    text = "${entry.weightGrams} ${strings.weightUnitGrams}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.removeChecklistItem(entry.id) },
                            modifier = Modifier.testTag("delete_checklist_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = strings.unequip,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Responsible Outdoor Preparedness Note (Non-guarantee of safety)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = strings.checklistDisclaimer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(strings.resetChecklistBtn) },
            text = { Text(strings.resetChecklistConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetCurrentTripChecklist()
                        showResetConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_reset_checklist_btn")
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
