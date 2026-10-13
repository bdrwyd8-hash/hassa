package com.example.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Self-contained 2D vector icon illustration for each equipment item in the library.
 * Includes a safe fallback badge so missing/unknown icon keys never fail or crash.
 */
@Composable
fun EquipmentVectorIcon(
    iconKey: String,
    colorHex: String,
    modifier: Modifier = Modifier
) {
    val primaryInt = CharacterBitmapRenderer.parseColorSafe(colorHex)
    val primary = Color(primaryInt)
    val accent = Color(0xFFE5A93C)
    val light = Color(0xFFECF2EE)
    val outline = Color(0xFF141A17)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Subtle circular badge backdrop
        drawCircle(
            color = primary.copy(alpha = 0.18f),
            radius = minOf(w, h) * 0.44f,
            center = Offset(cx, cy)
        )

        when {
            iconKey.startsWith("hat") || iconKey == "beanie" || iconKey == "cap" || iconKey == "head_wrap" -> {
                // Hat / Headwear illustration
                drawOval(
                    color = primary.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.14f, h * 0.54f),
                    size = Size(w * 0.72f, h * 0.20f)
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.26f, h * 0.28f),
                    size = Size(w * 0.48f, h * 0.34f),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
                )
                drawRect(
                    color = accent,
                    topLeft = Offset(w * 0.26f, h * 0.52f),
                    size = Size(w * 0.48f, h * 0.06f)
                )
            }

            iconKey.startsWith("shirt") || iconKey.startsWith("jacket") || iconKey.startsWith("vest") -> {
                // Shirt / Jacket torso + sleeves
                if (!iconKey.startsWith("vest")) {
                    drawRoundRect(
                        color = primary.copy(alpha = 0.8f),
                        topLeft = Offset(w * 0.15f, h * 0.26f),
                        size = Size(w * 0.18f, h * 0.44f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = primary.copy(alpha = 0.8f),
                        topLeft = Offset(w * 0.67f, h * 0.26f),
                        size = Size(w * 0.18f, h * 0.44f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.28f, h * 0.22f),
                    size = Size(w * 0.44f, h * 0.54f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawLine(
                    color = accent,
                    start = Offset(cx, h * 0.22f),
                    end = Offset(cx, h * 0.76f),
                    strokeWidth = 3f
                )
            }

            iconKey.startsWith("pants") -> {
                // Trekking trousers
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.28f, h * 0.20f),
                    size = Size(w * 0.44f, h * 0.18f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.28f, h * 0.32f),
                    size = Size(w * 0.20f, h * 0.48f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.52f, h * 0.32f),
                    size = Size(w * 0.20f, h * 0.48f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }

            iconKey.startsWith("boots") || iconKey.startsWith("shoes") -> {
                // Hiking boot silhouette
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.26f, h * 0.24f),
                    size = Size(w * 0.30f, h * 0.44f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.26f, h * 0.50f),
                    size = Size(w * 0.50f, h * 0.22f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = outline,
                    topLeft = Offset(w * 0.24f, h * 0.68f),
                    size = Size(w * 0.54f, h * 0.09f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            iconKey.startsWith("pack") -> {
                // Outdoor backpack
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.24f, h * 0.22f),
                    size = Size(w * 0.52f, h * 0.56f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(w * 0.34f, h * 0.30f),
                    size = Size(w * 0.06f, h * 0.36f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(w * 0.60f, h * 0.30f),
                    size = Size(w * 0.06f, h * 0.36f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            iconKey.startsWith("bottle") -> {
                // Water bottle / flask
                drawRoundRect(
                    color = outline,
                    topLeft = Offset(w * 0.40f, h * 0.16f),
                    size = Size(w * 0.20f, h * 0.10f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.30f, h * 0.26f),
                    size = Size(w * 0.40f, h * 0.54f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRect(
                    color = accent,
                    topLeft = Offset(w * 0.30f, h * 0.48f),
                    size = Size(w * 0.40f, h * 0.08f)
                )
            }

            iconKey.startsWith("food") -> {
                // Ration pouch / energy bar
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.24f, h * 0.22f),
                    size = Size(w * 0.52f, h * 0.56f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawCircle(
                    color = accent,
                    radius = w * 0.12f,
                    center = Offset(cx, cy)
                )
            }

            iconKey.startsWith("lantern") || iconKey.startsWith("flashlight") || iconKey == "headlamp" -> {
                // Lantern / Light
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.28f, h * 0.24f),
                    size = Size(w * 0.44f, h * 0.52f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawCircle(
                    color = Color(0xFFFFF2B2),
                    radius = w * 0.14f,
                    center = Offset(cx, cy)
                )
            }

            iconKey.startsWith("compass") || iconKey.startsWith("map") || iconKey == "altimeter" -> {
                // Compass / Navigation
                drawCircle(
                    color = primary,
                    radius = w * 0.30f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = accent,
                    radius = w * 0.30f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4f)
                )
                val needle = Path().apply {
                    moveTo(cx, h * 0.26f)
                    lineTo(w * 0.58f, cy)
                    lineTo(cx, h * 0.74f)
                    lineTo(w * 0.42f, cy)
                    close()
                }
                drawPath(needle, color = light)
            }

            iconKey.startsWith("first_aid") || iconKey == "thermal_blanket" -> {
                // First-aid medical cross pouch
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.22f, h * 0.26f),
                    size = Size(w * 0.56f, h * 0.48f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRect(
                    color = light,
                    topLeft = Offset(w * 0.45f, h * 0.36f),
                    size = Size(w * 0.10f, h * 0.28f)
                )
                drawRect(
                    color = light,
                    topLeft = Offset(w * 0.36f, h * 0.45f),
                    size = Size(w * 0.28f, h * 0.10f)
                )
            }

            iconKey.startsWith("tent") || iconKey.startsWith("sleeping") -> {
                // Tent triangle & roll
                val tentPath = Path().apply {
                    moveTo(cx, h * 0.22f)
                    lineTo(w * 0.80f, h * 0.74f)
                    lineTo(w * 0.20f, h * 0.74f)
                    close()
                }
                drawPath(tentPath, color = primary)
                val doorPath = Path().apply {
                    moveTo(cx, h * 0.44f)
                    lineTo(w * 0.62f, h * 0.74f)
                    lineTo(w * 0.38f, h * 0.74f)
                    close()
                }
                drawPath(doorPath, color = accent)
            }

            defaultSafeBranch(iconKey) -> {
                // Safe outdoor gear / patch / accessory emblem
                drawCircle(
                    color = primary,
                    radius = w * 0.28f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = accent,
                    radius = w * 0.14f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

private fun defaultSafeBranch(iconKey: String): Boolean = iconKey.isNotEmpty() || iconKey.isEmpty()
