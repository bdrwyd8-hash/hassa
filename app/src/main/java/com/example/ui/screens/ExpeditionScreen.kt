package com.example.ui.screens

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EquipmentCatalog
import com.example.game.GameCharacterRenderer
import com.example.game.SurvivalEnvironmentRenderer
import com.example.game.SurvivalGameplayState
import com.example.model.AiDifficulty
import com.example.model.AppLanguage
import com.example.model.BotAiState
import com.example.model.InventoryGridCell
import com.example.ui.canvas.CharacterBitmapRenderer
import com.example.ui.canvas.EquipmentVectorIcon
import com.example.viewmodel.TrailKitUiState
import com.example.viewmodel.TrailKitViewModel
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * High-Fidelity 2D Wilderness Survival Game & Innawoods-Style Slot Grid Inventory.
 * Features:
 * - Real-time 60fps continuous simulation with realistic natural wilderness environments.
 * - Recognizable 2.5D animated human explorer using custom Studio equipped loadout.
 * - Intelligent offline AI scavengers with patrol, line-of-sight sensing, squad flanking, and decoy investigation.
 * - Dynamic touch virtual analog thumbstick & sprint/gadget mobile controls.
 * - Integrated 5x4 Slot Grid Inventory with drag-and-drop, field supply usage (+HP / +Stamina).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpeditionScreen(
    uiState: TrailKitUiState,
    viewModel: TrailKitViewModel,
    modifier: Modifier = Modifier
) {
    val lang = uiState.preferences.language
    val isAr = lang == AppLanguage.AR
    val game = uiState.survivalGameplay
    val worldGrid = uiState.expeditionWorld.inventoryGrid
    val selectedCell = uiState.expeditionWorld.selectedGridSlotIndex?.let { worldGrid.getOrNull(it) }
    val selectedItem = selectedCell?.itemId?.let { EquipmentCatalog.findById(it) }

    var isInventorySheetOpen by remember { mutableStateOf(false) }
    var joystickX by remember { mutableFloatStateOf(0f) }
    var joystickY by remember { mutableFloatStateOf(0f) }
    var isSprinting by remember { mutableStateOf(false) }

    val isUnderRobolectric = remember {
        try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (_: Throwable) {
            false
        }
    }

    // Real-Time 60 FPS Game Loop (active on device / emulator, skipped during Robolectric Compose tests to allow idling)
    if (!isUnderRobolectric) {
        LaunchedEffect(Unit) {
            var lastTimeNanos = withFrameNanos { it }
            while (true) {
                val currentTimeNanos = withFrameNanos { it }
                val dt = ((currentTimeNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                lastTimeNanos = currentTimeNanos
                viewModel.updateSurvivalTick(joystickX, joystickY, isSprinting, dt)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1411))
            .testTag("expedition_screen")
    ) {
        // --- 1. Top Tactical Mission & Difficulty Bar ---
        Surface(
            color = Color(0xFF161F1A),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isAr) "رحلة البقاء في البرية" else "Wilderness Survival",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE4EDE7)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF28392F)
                            ) {
                                Text(
                                    text = if (isAr) "طور فردي أوفلاين" else "Offline AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE5A93C),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isAr) {
                                "المنارات: ${game.cachesSecuredCount}/${game.totalPrimaryCaches} • المهمات المنجزة: ${uiState.progression.missionsCompleted}"
                            } else {
                                "Beacons: ${game.cachesSecuredCount}/${game.totalPrimaryCaches} • Won: ${uiState.progression.missionsCompleted}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA6B7AC),
                            modifier = Modifier.testTag("expedition_objectives_counter")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Restart / New Round Button
                        OutlinedButton(
                            onClick = { viewModel.resetSurvivalGame() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("reset_expedition_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isAr) "إعادة" else "Restart", style = MaterialTheme.typography.labelMedium)
                        }

                        // Open Innawoods Inventory Sheet Button
                        Button(
                            onClick = { isInventorySheetOpen = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3D6B52),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("toggle_inventory_btn")
                        ) {
                            Icon(Icons.Default.Backpack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isAr) "المخزون" else "Gear Grid", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // AI Difficulty Selector Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AiDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = game.difficulty == diff,
                            onClick = { viewModel.setSurvivalDifficulty(diff) },
                            label = {
                                Text(
                                    text = diff.label(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (game.difficulty == diff) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("difficulty_chip_${diff.id}")
                        )
                    }
                }
            }
        }

        // Live AI Tactical Log Banner
        Surface(
            color = when {
                game.isMissionComplete -> Color(0xFF1E462E)
                game.isPlayerDefeated -> Color(0xFF4D1A18)
                else -> Color(0xFF1D2621)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_status_log_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        game.isMissionComplete -> Icons.Default.CheckCircle
                        game.isPlayerDefeated -> Icons.Default.Warning
                        else -> Icons.Default.Security
                    },
                    contentDescription = null,
                    tint = when {
                        game.isMissionComplete -> Color(0xFF5CB85C)
                        game.isPlayerDefeated -> Color(0xFFD9534F)
                        else -> Color(0xFFE5A93C)
                    },
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isAr) game.bannerMessageAr else game.bannerMessageEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // --- 2. High-Fidelity 2D Survival Game Viewport (Real-time Canvas + HUD + Mobile Controls) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Main Natural Wilderness Real-Time Game Canvas
            SurvivalGameCanvas(
                gameState = game,
                uiState = uiState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("expedition_map_canvas")
            )

            // Top-Left Survival Telemetry (HP, Stamina, Stealth)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Health Bar
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC111814),
                    modifier = Modifier.width(160.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Healing, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(13.dp))
                                Text("HP", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text("${game.player.health}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { game.player.health / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (game.player.health > 35) Color(0xFF5CB85C) else Color(0xFFD9534F),
                            trackColor = Color(0x55FFFFFF)
                        )
                    }
                }

                // Stamina Bar
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC111814),
                    modifier = Modifier.width(160.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFFE5A93C), modifier = Modifier.size(13.dp))
                                Text("ST", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text("${game.player.stamina.roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { game.player.stamina / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFE5A93C),
                            trackColor = Color(0x55FFFFFF)
                        )
                    }
                }

                // Stealth Status Indicator
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (game.player.inStealthBush) Color(0xDD2D5E40) else Color(0xBB233029)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (game.player.inStealthBush) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (game.player.inStealthBush) Color(0xFF67B786) else Color(0xFFA0B5A7),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (game.player.inStealthBush) {
                                if (isAr) "مختبئ في الشجيرات" else "Hidden in Bush"
                            } else {
                                if (isAr) "مكشوف" else "Visible"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            // Top-Right Live Bots Status Strip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xCC111814),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    game.bots.forEach { bot ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(CharacterBitmapRenderer.parseColorSafe(bot.state.badgeColorHex)))
                            )
                            Text(
                                text = "${bot.name(lang)}: ${bot.state.label(lang)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2EBE5)
                            )
                        }
                    }
                }
            }

            // Contextual Interaction Prompt: Collect Supply
            val nearby = game.nearbySupply
            if (nearby != null && !game.isMissionComplete && !game.isPlayerDefeated) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xF0E5A93C),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 100.dp)
                        .clickable { viewModel.collectNearbySurvivalSupply() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Text(
                            text = if (isAr) "جمع: ${nearby.name(lang)}" else "Collect: ${nearby.name(lang)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            // Contextual Interaction Prompt: Evacuate Pad
            if (game.canExtract && !game.isMissionComplete && !game.isPlayerDefeated) {
                val ready = game.cachesSecuredCount >= game.totalPrimaryCaches
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (ready) Color(0xF05CB85C) else Color(0xF0C97228),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 100.dp)
                        .clickable { if (ready) viewModel.triggerSurvivalExtraction() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (ready) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (ready) {
                                if (isAr) "إخلاء فوري للمروحية! اضغط هنا" else "EXTRACT NOW! Tap here"
                            } else {
                                if (isAr) "أمّن الـ 3 منارات أولاً للإخلاء!" else "Secure 3 Beacons to Evacuate!"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // --- Mobile On-Screen Controls Overlay ---
            // Bottom-Left: Virtual Analog Thumbstick
            VirtualAnalogJoystick(
                onMove = { x, y ->
                    joystickX = x
                    joystickY = y
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            )

            // Bottom-Right: Tactical Action Button Cluster
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Flashlight Toggle Button
                    ActionButtonCircle(
                        icon = Icons.Default.FlashlightOn,
                        isActive = game.player.flashlightOn,
                        activeColor = Color(0xFFE5A93C),
                        onClick = { viewModel.toggleSurvivalFlashlight() },
                        modifier = Modifier.testTag("toggle_flashlight_btn")
                    )

                    // Acoustic Decoy Stone
                    ActionButtonCircle(
                        icon = Icons.Default.VolumeUp,
                        cooldownSeconds = game.decoyCooldownSeconds,
                        activeColor = Color(0xFF5D8AA8),
                        onClick = { viewModel.throwSurvivalDecoy() },
                        modifier = Modifier.testTag("throw_decoy_btn")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Flashlight Strobe Flare
                    ActionButtonCircle(
                        icon = Icons.Default.FlashOn,
                        cooldownSeconds = game.strobeCooldownSeconds,
                        activeColor = Color(0xFFE5A93C),
                        onClick = { viewModel.triggerSurvivalStrobe() },
                        modifier = Modifier.testTag("trigger_strobe_btn")
                    )

                    // Sprint Toggle
                    ActionButtonCircle(
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        isActive = isSprinting,
                        activeColor = Color(0xFF4CAF50),
                        onClick = { isSprinting = !isSprinting },
                        modifier = Modifier.testTag("sprint_toggle_btn")
                    )
                }
            }
        }
    }

    // --- 3. Innawoods-Style 5x4 Grid Inventory Bottom Sheet ---
    if (isInventorySheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isInventorySheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF161F1B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("slot_grid_inventory_card"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val gridWeight = worldGrid.sumOf { cell ->
                    val item = cell.itemId?.let { EquipmentCatalog.findById(it) }
                    (item?.weightGrams ?: 0) * cell.quantity
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAr) "مخزون الحقيبة المرتب بخانات (5×4)" else "Tactical Slot Grid Inventory (5×4)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE4EDE7)
                        )
                        Text(
                            text = if (isAr) {
                                "اسحب وأفلت أو انقر للتبديل • الوزن الإجمالي: ${gridWeight}غ"
                            } else {
                                "Drag & drop or tap to swap • Weight: ${gridWeight}g"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA6B7AC)
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.saveCurrentCharacter() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("save_loadout_grid_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isAr) "حفظ التجهيز" else "Save Loadout", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // 5x4 Grid Composable
                SlotInventoryGrid(
                    cells = worldGrid,
                    selectedSlotIndex = uiState.expeditionWorld.selectedGridSlotIndex,
                    onTapSlot = { idx -> viewModel.selectOrSwapGridSlot(idx) },
                    onDragDropSlot = { from, to -> viewModel.moveGridSlotDragDrop(from, to) }
                )

                // Selected Grid Item Details & "Use Field Supply" (+HP / +Stamina)
                if (selectedCell != null && selectedItem != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF202A24),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("selected_grid_slot_panel")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                EquipmentVectorIcon(
                                    iconKey = selectedItem.icon,
                                    colorHex = selectedItem.colors.firstOrNull() ?: "#4E8768",
                                    modifier = Modifier.size(38.dp)
                                )
                                Column {
                                    Text(
                                        text = "${selectedItem.name(lang)} (×${selectedCell.quantity})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${selectedItem.weightGrams * selectedCell.quantity}g • ${selectedItem.description(lang)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA6B7AC),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.consumeGridSlotItem(selectedCell.index) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4E8768),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("consume_grid_item_btn")
                            ) {
                                Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAr) "استخدام (+صحة/طاقة)" else "Use (+HP/ST)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Quick-Pack Field Supplies Strip
                Text(
                    text = if (isAr) "تجهيز مؤن سريعة من الكتالوج:" else "Quick-Pack Supplies from Catalog:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFA6B7AC)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickSupplies = listOf(
                        "aid-field-pouch-red",
                        "food-trail-mix-pouch",
                        "water-steel-flask-1l",
                        "nav-brass-compass",
                        "light-aluminum-torch"
                    )
                    quickSupplies.forEach { itemId ->
                        val item = EquipmentCatalog.findById(itemId) ?: return@forEach
                        OutlinedButton(
                            onClick = { viewModel.packCatalogItemIntoGrid(item.id) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("quick_pack_${item.id}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(item.name(lang), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- 4. Game Over Dialog (Defeat) ---
    if (game.isPlayerDefeated) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD9534F), modifier = Modifier.size(36.dp)) },
            title = {
                Text(
                    text = if (isAr) "تمت محاصرتك في البرية!" else "Captured in Wilderness!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = if (isAr) {
                        "رصدتك دوريات كشافة الغابة وانتهت نقاط صحتك. استخدم الشجيرات للتخفي أو ارمِ أحجار التمويه لتضليلهم."
                    } else {
                        "Scavenger bots flanked and subdued you. Use bush stealth cover and acoustic decoy stones to distract them."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetSurvivalGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E8768))
                ) {
                    Text(if (isAr) "إعادة المحاولة" else "Try Again")
                }
            }
        )
    }

    // --- 5. Mission Accomplished Dialog (Victory) ---
    if (game.isMissionComplete) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF5CB85C), modifier = Modifier.size(40.dp)) },
            title = {
                Text(
                    text = if (isAr) "تم الإخلاء بنجاح!" else "Mission Accomplished!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = if (isAr) {
                        "أمّنت جميع منارات الإشارة الثلاث ووصلت إلى مهبط المروحية بأمان! تم تسجيل فوزك وإضافته إلى إحصائياتك."
                    } else {
                        "You secured all 3 Signal Beacons and safely reached the Extraction Pad! Your victory has been logged into your survival progression."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetSurvivalGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D6B52))
                ) {
                    Text(if (isAr) "جولة جديدة" else "Play Again")
                }
            }
        )
    }
}

/**
 * Renders the full 2D wilderness environment, dynamic camera tracking, human characters, and atmospheric overlays.
 */
@Composable
private fun SurvivalGameCanvas(
    gameState: SurvivalGameplayState,
    uiState: TrailKitUiState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val vw = size.width
            val vh = size.height
            val zoom = 1.0f
            val camX = gameState.cameraX
            val camY = gameState.cameraY
            val time = gameState.gameTimeSeconds

            // 1. Natural Forest Grass, Flowing River Stream & Winding Dirt Hiking Trails
            SurvivalEnvironmentRenderer.drawTerrainAndPaths(
                nativeCanvas, camX, camY, vw, vh, zoom,
                worldWidth = 2400f, worldHeight = 1800f, windTime = time
            )

            // 2. Solid Rocks, Abandoned Ranger Cabin, Campfire Embers, Dense Stealth Bushes
            SurvivalEnvironmentRenderer.drawSolidObstaclesAndCampsite(
                nativeCanvas, gameState.obstacles, camX, camY, vw, vh, zoom, time
            )

            // 3. Extraction Helipad Landing Pad with rotating beacons
            SurvivalEnvironmentRenderer.drawExtractionZone(
                nativeCanvas, gameState.extractionZone,
                allCachesSecured = gameState.cachesSecuredCount >= gameState.totalPrimaryCaches,
                camX, camY, vw, vh, zoom, time
            )

            // 4. Military Signal Beacons & Secondary Collectible Supply Crates
            SurvivalEnvironmentRenderer.drawSupplyCaches(
                nativeCanvas, gameState.supplies, camX, camY, vw, vh, zoom, time
            )

            // 5. Intelligent AI Bots with animated swinging limbs, vests, hats & tactical status badges
            gameState.bots.forEach { bot ->
                val screenX = (bot.x - camX) * zoom + vw / 2f
                val screenY = (bot.y - camY) * zoom + vh / 2f
                GameCharacterRenderer.drawBot(nativeCanvas, bot, screenX, screenY, scale = zoom)
            }

            // 6. Player Character with Studio custom loadout, animated walk/run cycle & flashlight beam
            val playerScreenX = (gameState.player.x - camX) * zoom + vw / 2f
            val playerScreenY = (gameState.player.y - camY) * zoom + vh / 2f
            GameCharacterRenderer.drawPlayer(
                nativeCanvas, gameState.player, uiState.characterConfig,
                playerScreenX, playerScreenY, scale = zoom
            )

            // 7. Overhead Oak & Pine Canopies (fading dynamically when player walks underneath)
            SurvivalEnvironmentRenderer.drawOverheadCanopies(
                nativeCanvas, gameState.obstacles, gameState.player.x, gameState.player.y,
                camX, camY, vw, vh, zoom
            )

            // 8. Acoustic Decoy Projectile Arc, Expanding Sound Shockwaves & Forest Edge Vignette
            SurvivalEnvironmentRenderer.drawAtmosphericVignetteAndDecoyRipples(
                nativeCanvas, vw, vh, gameState.decoyProjectile, camX, camY, zoom
            )
        }
    }
}

/**
 * Virtual Analog Touch Joystick for smooth 360-degree mobile controls.
 */
@Composable
private fun VirtualAnalogJoystick(
    onMove: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxRadiusPx = 130f
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(130.dp)
            .clip(CircleShape)
            .background(Color(0x55000000))
            .border(2.dp, Color(0x66FFFFFF), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {},
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val raw = knobOffset + dragAmount
                        val dist = sqrt(raw.x * raw.x + raw.y * raw.y)
                        val clamped = if (dist > maxRadiusPx) {
                            Offset(raw.x / dist * maxRadiusPx, raw.y / dist * maxRadiusPx)
                        } else {
                            raw
                        }
                        knobOffset = clamped
                        onMove(clamped.x / maxRadiusPx, clamped.y / maxRadiusPx)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner thumbstick knob
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xDD3D6B52))
                .border(2.dp, Color(0xFFE5A93C), CircleShape)
        )
    }
}

/**
 * Floating Action Button with optional cooldown or active state ring.
 */
@Composable
private fun ActionButtonCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    cooldownSeconds: Float = 0f
) {
    val enabled = cooldownSeconds <= 0f

    Surface(
        shape = CircleShape,
        color = if (isActive) activeColor else Color(0xCC18241E),
        shadowElevation = 6.dp,
        modifier = modifier
            .size(54.dp)
            .border(2.dp, if (isActive) Color.White else Color(0x66FFFFFF), CircleShape)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (cooldownSeconds > 0f) {
                Text(
                    text = "${cooldownSeconds.toInt() + 1}s",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE5A93C)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) Color.Black else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 5x4 Slot Grid with Drag-and-Drop and Tap-to-Swap for Innawoods inventory management.
 */
@Composable
private fun SlotInventoryGrid(
    cells: List<InventoryGridCell>,
    selectedSlotIndex: Int?,
    onTapSlot: (Int) -> Unit,
    onDragDropSlot: (Int, Int) -> Unit
) {
    val cols = 5
    val rows = 4
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    var draggingFromIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { gridSize = it }
            .pointerInput(cells) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        if (gridSize.width > 0 && gridSize.height > 0) {
                            val col = (startOffset.x / (gridSize.width.toFloat() / cols)).toInt().coerceIn(0, cols - 1)
                            val row = (startOffset.y / (gridSize.height.toFloat() / rows)).toInt().coerceIn(0, rows - 1)
                            draggingFromIndex = row * cols + col
                        }
                    },
                    onDragEnd = { draggingFromIndex = null },
                    onDragCancel = { draggingFromIndex = null },
                    onDrag = { change, _ ->
                        change.consume()
                        val fromIdx = draggingFromIndex
                        if (fromIdx != null && gridSize.width > 0 && gridSize.height > 0) {
                            val pos = change.position
                            val col = (pos.x / (gridSize.width.toFloat() / cols)).toInt().coerceIn(0, cols - 1)
                            val row = (pos.y / (gridSize.height.toFloat() / rows)).toInt().coerceIn(0, rows - 1)
                            val toIdx = row * cols + col
                            if (toIdx != fromIdx) {
                                onDragDropSlot(fromIdx, toIdx)
                                draggingFromIndex = toIdx
                            }
                        }
                    }
                )
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (c in 0 until cols) {
                    val index = r * cols + c
                    val cell = cells.getOrNull(index) ?: InventoryGridCell(index)
                    val item = cell.itemId?.let { EquipmentCatalog.findById(it) }
                    val isSelected = selectedSlotIndex == index

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isSelected -> Color(0xFF385E46)
                            cell.itemId != null -> Color(0xFF233027)
                            else -> Color(0xFF1B241E)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFFE5A93C) else Color(0x33FFFFFF),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onTapSlot(index) }
                            .testTag("inventory_slot_$index")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (item != null) {
                                EquipmentVectorIcon(
                                    iconKey = item.icon,
                                    colorHex = item.colors.firstOrNull() ?: "#4E8768",
                                    modifier = Modifier.size(26.dp)
                                )
                                if (cell.quantity > 1) {
                                    Text(
                                        text = "${cell.quantity}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(2.dp)
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
