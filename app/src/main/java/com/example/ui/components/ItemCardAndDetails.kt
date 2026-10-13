package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.EquipmentCatalog
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.model.CharacterConfig
import com.example.model.CharacterPreset
import com.example.model.EquipmentItem
import com.example.model.EquippedLayer
import com.example.model.LayerSlot
import com.example.model.SceneBackground
import com.example.ui.canvas.CharacterBitmapRenderer
import com.example.ui.canvas.EquipmentVectorIcon

@Composable
fun EquipmentItemCard(
    item: EquipmentItem,
    isEquipped: Boolean,
    isSelected: Boolean,
    equippedColorHex: String?,
    language: AppLanguage,
    strings: LocalizedStrings,
    onSelect: () -> Unit,
    onToggleEquip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = equippedColorHex ?: item.colors.firstOrNull() ?: "#3D6B52"
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.secondary
        isEquipped -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                onSelect()
                onToggleEquip()
            }
            .border(
                width = if (isSelected || isEquipped) 1.8.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("item_card_${item.id}"),
        color = if (isEquipped) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                EquipmentVectorIcon(
                    iconKey = item.icon,
                    colorHex = activeColor,
                    modifier = Modifier.size(44.dp)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${item.weightGrams}${strings.weightUnitGrams}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    if (isEquipped) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = strings.equippedBadge,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = item.name(language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Color dots preview + Details trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.colors.take(4).forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(CharacterBitmapRenderer.parseColorSafe(hex)))
                                .border(0.8.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        )
                    }
                }

                Text(
                    text = if (isEquipped) strings.equippedBadge else strings.equip,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isEquipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onSelect() }
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedItemDetailsPanel(
    item: EquipmentItem,
    equippedLayer: EquippedLayer?,
    language: AppLanguage,
    strings: LocalizedStrings,
    onEquipOrUnequip: () -> Unit,
    onSelectColor: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onMoveLayerUp: () -> Unit,
    onMoveLayerDown: () -> Unit,
    onMoveToSlot: (LayerSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomPalette by remember { mutableStateOf(false) }
    val activeColor = equippedLayer?.colorHex ?: item.colors.firstOrNull() ?: "#3D6B52"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("selected_item_details_panel"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EquipmentVectorIcon(
                    iconKey = item.icon,
                    colorHex = activeColor,
                    modifier = Modifier.size(48.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name(language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.category.label(language)} • ${item.weightGrams} ${strings.weightUnitGrams}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Button(
                    onClick = onEquipOrUnequip,
                    colors = if (equippedLayer != null) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    modifier = Modifier.testTag("detail_equip_toggle_btn")
                ) {
                    Text(
                        text = if (equippedLayer != null) strings.unequip else strings.equip,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Text(
                text = item.description(language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Color Picker Row (Item colors + Custom color picker toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = strings.itemColorLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    item.colors.forEach { hex ->
                        ColorSwatchCircle(
                            hex = hex,
                            isSelected = activeColor.equals(hex, ignoreCase = true),
                            onClick = { onSelectColor(hex) },
                            testTag = "color_swatch_$hex"
                        )
                    }
                }

                TextButton(
                    onClick = { showCustomPalette = !showCustomPalette },
                    modifier = Modifier.testTag("toggle_custom_palette_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = strings.customColorLabel,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = strings.customColorLabel, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (showCustomPalette) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("custom_color_picker_grid"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EquipmentCatalog.customPickerPalette.forEach { hex ->
                        ColorSwatchCircle(
                            hex = hex,
                            isSelected = activeColor.equals(hex, ignoreCase = true),
                            onClick = { onSelectColor(hex) },
                            testTag = "custom_color_$hex"
                        )
                    }
                }
            }

            // Layer controls when item is equipped: Hide/Show, Move Up, Move Down, Move Slot
            if (equippedLayer != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier.testTag("layer_visibility_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (equippedLayer.visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (equippedLayer.visible) strings.hideLayer else strings.showLayer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedButton(
                        onClick = onMoveLayerUp,
                        modifier = Modifier.testTag("layer_move_up_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = strings.moveLayerUp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = strings.moveLayerUp, style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedButton(
                        onClick = onMoveLayerDown,
                        modifier = Modifier.testTag("layer_move_down_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = strings.moveLayerDown,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = strings.moveLayerDown, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (item.compatibleSlots.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.moveToSlot,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        item.compatibleSlots.forEach { slot ->
                            FilterChip(
                                selected = equippedLayer.slot == slot,
                                onClick = { onMoveToSlot(slot) },
                                label = {
                                    Text(
                                        text = slot.label(language),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                modifier = Modifier.testTag("move_to_slot_${slot.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorSwatchCircle(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val parsed = Color(CharacterBitmapRenderer.parseColorSafe(hex))
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(parsed)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
fun BaseAppearanceDialog(
    config: CharacterConfig,
    presets: List<CharacterPreset>,
    language: AppLanguage,
    strings: LocalizedStrings,
    onApplyPreset: (CharacterPreset) -> Unit,
    onSelectSkin: (String) -> Unit,
    onSelectHairStyle: (Int) -> Unit,
    onSelectHairColor: (String) -> Unit,
    onSelectScene: (SceneBackground) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.bodyAppearanceLabel,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Presets
                Text(
                    text = strings.presetsLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        OutlinedButton(
                            onClick = { onApplyPreset(preset) },
                            modifier = Modifier.testTag("preset_btn_${preset.id}")
                        ) {
                            Text(text = preset.label(language), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Skin tone
                Text(
                    text = strings.skinToneLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EquipmentCatalog.skinTones.forEach { hex ->
                        ColorSwatchCircle(
                            hex = hex,
                            isSelected = config.skinColorHex.equals(hex, ignoreCase = true),
                            onClick = { onSelectSkin(hex) },
                            testTag = "skin_swatch_$hex"
                        )
                    }
                }

                // Hair Style (0..3)
                Text(
                    text = strings.hairStyleLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..3).forEach { styleIdx ->
                        FilterChip(
                            selected = config.hairStyle == styleIdx,
                            onClick = { onSelectHairStyle(styleIdx) },
                            label = { Text("#${styleIdx + 1}") },
                            modifier = Modifier.testTag("hair_style_chip_$styleIdx")
                        )
                    }
                }

                // Hair Color
                Text(
                    text = strings.hairColorLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EquipmentCatalog.hairColors.forEach { hex ->
                        ColorSwatchCircle(
                            hex = hex,
                            isSelected = config.hairColorHex.equals(hex, ignoreCase = true),
                            onClick = { onSelectHairColor(hex) },
                            testTag = "hair_color_$hex"
                        )
                    }
                }

                // Scene Background
                Text(
                    text = strings.sceneLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SceneBackground.entries.forEach { scene ->
                        FilterChip(
                            selected = config.scene == scene,
                            onClick = { onSelectScene(scene) },
                            label = { Text(scene.label(language)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scene_option_${scene.id}")
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.closeBtn)
            }
        }
    )
}

@Composable
fun LayerManagerDialog(
    config: CharacterConfig,
    language: AppLanguage,
    strings: LocalizedStrings,
    onToggleVisibility: (LayerSlot) -> Unit,
    onMoveUp: (LayerSlot) -> Unit,
    onMoveDown: (LayerSlot) -> Unit,
    onRemoveSlot: (LayerSlot) -> Unit,
    onDismiss: () -> Unit
) {
    val ordered = config.orderedLayers().reversed() // Top-most layer first

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.layersPanelTitle,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (ordered.isEmpty()) {
                Text(
                    text = strings.noEquippedLayers,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ordered.forEach { layer ->
                        val item = EquipmentCatalog.findById(layer.itemId)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item?.name(language) ?: layer.itemId,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${layer.slot.label(language)} (Z:${layer.zOrder})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { onToggleVisibility(layer.slot) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = strings.hideLayer,
                                            tint = if (layer.visible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMoveUp(layer.slot) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = strings.moveLayerUp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMoveDown(layer.slot) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = strings.moveLayerDown,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveSlot(layer.slot) },
                                        modifier = Modifier.size(34.dp)
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
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.closeBtn)
            }
        }
    )
}
