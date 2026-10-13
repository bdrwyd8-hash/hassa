package com.example.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.EquipmentCatalog
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.model.CharacterConfig
import com.example.model.LayerSlot

@Composable
fun CharacterStageCard(
    characterName: String,
    config: CharacterConfig,
    totalWeightGrams: Int,
    equippedCount: Int,
    selectedSlot: LayerSlot?,
    canUndo: Boolean,
    canRedo: Boolean,
    language: AppLanguage,
    strings: LocalizedStrings,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onRequestReset: () -> Unit,
    onCycleScene: () -> Unit,
    onOpenAppearanceSheet: () -> Unit,
    onOpenLayersSheet: () -> Unit,
    onSaveCharacter: () -> Unit,
    onExportPng: () -> Unit,
    onSelectLayerSlot: (LayerSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("character_stage_card"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top bar inside canvas card: Explorer Name, Weight badge, Save & PNG Export
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = characterName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = strings.savedStatusAuto,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${strings.totalWeightLabel}: $totalWeightGrams ${strings.weightUnitGrams} • ${strings.equippedCountLabel}: $equippedCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onSaveCharacter,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("studio_save_btn"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = strings.saveCharacterBtn,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onExportPng,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("studio_export_png_btn"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = strings.exportPngBtn,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Central 2D Interactive Character Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .testTag("character_vector_canvas")
            ) {
                CharacterPreviewCanvas(
                    config = config,
                    selectedSlot = selectedSlot,
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Left Controls: Undo, Redo, Reset
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CanvasControlButton(
                        icon = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = strings.undo,
                        enabled = canUndo,
                        onClick = onUndo,
                        testTag = "undo_button"
                    )
                    CanvasControlButton(
                        icon = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = strings.redo,
                        enabled = canRedo,
                        onClick = onRedo,
                        testTag = "redo_button"
                    )
                    CanvasControlButton(
                        icon = Icons.Default.Refresh,
                        contentDescription = strings.resetCharacter,
                        enabled = true,
                        onClick = onRequestReset,
                        testTag = "reset_button"
                    )
                }

                // Floating Right Controls: Scene Switcher, Body Appearance, Layer Order Sheet
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CanvasControlButton(
                        icon = Icons.Default.Landscape,
                        contentDescription = strings.sceneLabel,
                        enabled = true,
                        onClick = onCycleScene,
                        testTag = "scene_switch_button"
                    )
                    CanvasControlButton(
                        icon = Icons.Default.Face,
                        contentDescription = strings.bodyAppearanceLabel,
                        enabled = true,
                        onClick = onOpenAppearanceSheet,
                        testTag = "body_appearance_button"
                    )
                    CanvasControlButton(
                        icon = Icons.Default.Layers,
                        contentDescription = strings.layersPanelTitle,
                        enabled = true,
                        onClick = onOpenLayersSheet,
                        testTag = "layers_sheet_button"
                    )
                }

                // Bottom-left scene badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clickable { onCycleScene() },
                    color = Color.Black.copy(alpha = 0.58f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = config.scene.label(language),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Touch-friendly Equipped Layer Chips Bar (Tap any equipped layer to select/inspect it)
            val orderedLayers = config.orderedLayers()
            if (orderedLayers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("equipped_layers_strip"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    orderedLayers.forEach { layer ->
                        val item = EquipmentCatalog.findById(layer.itemId)
                        val isSelected = layer.slot == selectedSlot
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectLayerSlot(layer.slot) }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .testTag("equipped_layer_chip_${layer.slot.id}"),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(CharacterBitmapRenderer.parseColorSafe(layer.colorHex)))
                                )
                                Text(
                                    text = item?.name(language) ?: layer.slot.label(language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1
                                )
                                if (!layer.visible) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = strings.hiddenBadge,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(13.dp)
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

@Composable
fun CharacterPreviewCanvas(
    config: CharacterConfig,
    selectedSlot: LayerSlot? = null,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        CharacterBitmapRenderer.drawCharacterScene(
            canvas = drawContext.canvas.nativeCanvas,
            width = size.width,
            height = size.height,
            config = config,
            selectedSlot = selectedSlot
        )
    }
}

@Composable
private fun CanvasControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .testTag(testTag),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color(0xCC141A17),
            contentColor = Color(0xFFECF2EE),
            disabledContainerColor = Color(0x66141A17),
            disabledContentColor = Color(0x66ECF2EE)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}
