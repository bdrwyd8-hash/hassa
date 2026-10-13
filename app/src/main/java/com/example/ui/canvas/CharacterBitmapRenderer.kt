package com.example.ui.canvas

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.example.data.EquipmentCatalog
import com.example.model.CharacterConfig
import com.example.model.EquippedLayer
import com.example.model.LayerSlot
import com.example.model.SceneBackground

/**
 * Deterministic 2D Vector Explorer & Outdoor Gear Renderer.
 * Used by both the live Jetpack Compose CharacterCanvas and the PNG Bitmap Exporter.
 * Guarantees the base explorer character always renders and never loses body parts.
 */
object CharacterBitmapRenderer {

    private const val BASE_BODY_Z = 20
    private const val BASE_HEAD_Z = 52

    fun parseColorSafe(hex: String?, fallback: Int = Color.parseColor("#3D6B52")): Int {
        if (hex.isNullOrBlank()) return fallback
        return try {
            Color.parseColor(hex.trim())
        } catch (_: Exception) {
            fallback
        }
    }

    private fun darken(color: Int, factor: Float = 0.78f): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(color), r, g, b)
    }

    private fun lighten(color: Int, factor: Float = 0.22f): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(color), r, g, b)
    }

    fun drawCharacterScene(
        canvas: Canvas,
        width: Float,
        height: Float,
        config: CharacterConfig,
        characterName: String = "",
        selectedSlot: LayerSlot? = null
    ) {
        drawSceneBackground(canvas, width, height, config.scene)

        // Normalize coordinate system to a 400 x 500 virtual stage centered in canvas
        val scale = minOf(width / 400f, height / 500f)
        val offsetX = (width - 400f * scale) / 2f
        val offsetY = (height - 500f * scale) / 2f

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        // Ground shadow under explorer
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(95, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(120f, 436f, 280f, 464f), shadowPaint)

        val visibleLayers = config.orderedLayers().filter { it.visible }

        // 1. Layers behind base body (zOrder < BASE_BODY_Z, e.g. Backpack main body & Camp Gear)
        visibleLayers.filter { it.zOrder < BASE_BODY_Z }.forEach { layer ->
            drawEquippedLayer(canvas, layer, isHighlighted = (layer.slot == selectedSlot))
        }

        // 2. Always render Base Explorer Body (Legs, Torso, Neck, Arms, Hands, Default Underwear/Shorts)
        drawBaseExplorerBody(canvas, config)

        // 3. Layers between Base Body and Base Head (e.g. Pants, Boots, Shirts, Jackets, Weather Gear)
        visibleLayers.filter { it.zOrder in BASE_BODY_Z until BASE_HEAD_Z }.forEach { layer ->
            drawEquippedLayer(canvas, layer, isHighlighted = (layer.slot == selectedSlot))
        }

        // If backpack is equipped and visible, draw its front shoulder straps over the torso
        val backpackLayer = visibleLayers.find { it.slot == LayerSlot.BACKPACK }
        if (backpackLayer != null) {
            drawBackpackFrontStraps(canvas, parseColorSafe(backpackLayer.colorHex))
        }

        // 4. Always render Base Explorer Head, Face, and Hair
        drawBaseExplorerHeadAndHair(canvas, config)

        // 5. Layers at or above Head (e.g. Headwear, Neck/Chest Gear, Left/Right Hand Gear, Belt Pouches, Patches)
        visibleLayers.filter { it.zOrder >= BASE_HEAD_Z }.forEach { layer ->
            drawEquippedLayer(canvas, layer, isHighlighted = (layer.slot == selectedSlot))
        }

        canvas.restore()
    }

    private fun drawSceneBackground(
        canvas: Canvas,
        width: Float,
        height: Float,
        scene: SceneBackground
    ) {
        val skyTop = parseColorSafe(scene.skyTopHex)
        val skyBottom = parseColorSafe(scene.skyBottomHex)
        val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, height, skyTop, skyBottom, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width, height, skyPaint)

        if (scene.isNeutral) {
            // Subtle studio grid for clean neutral exports
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(24, 255, 255, 255)
                strokeWidth = 1.5f
                style = Paint.Style.STROKE
            }
            val step = width / 10f
            var x = step
            while (x < width) {
                canvas.drawLine(x, 0f, x, height, gridPaint)
                x += step
            }
            var y = step
            while (y < height) {
                canvas.drawLine(0f, y, width, y, gridPaint)
                y += step
            }
            return
        }

        // Sun / Moon orb
        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when (scene) {
                SceneBackground.DESERT_CANYON -> Color.argb(90, 242, 196, 109)
                SceneBackground.STARRY_CAMP -> Color.argb(140, 230, 240, 255)
                else -> Color.argb(65, 229, 169, 60)
            }
        }
        canvas.drawCircle(width * 0.78f, height * 0.18f, width * 0.09f, orbPaint)

        // Stars if Starry Camp
        if (scene == SceneBackground.STARRY_CAMP) {
            val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(190, 255, 250, 220)
            }
            val stars = listOf(
                0.15f to 0.10f, 0.28f to 0.18f, 0.44f to 0.08f,
                0.62f to 0.14f, 0.85f to 0.09f, 0.12f to 0.28f,
                0.52f to 0.24f, 0.88f to 0.27f
            )
            stars.forEach { (sx, sy) ->
                canvas.drawCircle(width * sx, height * sy, 2.5f, starPaint)
            }
        }

        // Far mountain range
        val farMountainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColorSafe(scene.mountainFarHex)
            style = Paint.Style.FILL
        }
        val farPath = Path().apply {
            moveTo(0f, height * 0.72f)
            lineTo(width * 0.22f, height * 0.46f)
            lineTo(width * 0.45f, height * 0.60f)
            lineTo(width * 0.72f, height * 0.42f)
            lineTo(width, height * 0.62f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        canvas.drawPath(farPath, farMountainPaint)

        // Near mountain / pine ridge
        val nearMountainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColorSafe(scene.mountainNearHex)
            style = Paint.Style.FILL
        }
        val nearPath = Path().apply {
            moveTo(0f, height * 0.76f)
            lineTo(width * 0.32f, height * 0.58f)
            lineTo(width * 0.60f, height * 0.70f)
            lineTo(width * 0.88f, height * 0.55f)
            lineTo(width, height * 0.68f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        canvas.drawPath(nearPath, nearMountainPaint)

        // Foreground trail ground
        val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColorSafe(scene.groundHex)
            style = Paint.Style.FILL
        }
        canvas.drawOval(
            RectF(-width * 0.2f, height * 0.78f, width * 1.2f, height * 1.15f),
            groundPaint
        )
    }

    private fun drawBaseExplorerBody(canvas: Canvas, config: CharacterConfig) {
        val skinColor = parseColorSafe(config.skinColorHex, Color.parseColor("#E0B187"))
        val skinShadow = darken(skinColor, 0.84f)
        val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = skinColor }
        val skinShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = skinShadow }
        val baseClothPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColorSafe(config.baseUnderwearColorHex, Color.parseColor("#3A4E43"))
        }

        // Neck
        canvas.drawRoundRect(RectF(184f, 148f, 216f, 182f), 8f, 8f, skinShadowPaint)

        // Left & Right Arms (Base skin)
        canvas.drawRoundRect(RectF(118f, 178f, 148f, 302f), 14f, 14f, skinPaint)
        canvas.drawRoundRect(RectF(252f, 178f, 282f, 302f), 14f, 14f, skinPaint)

        // Hands
        canvas.drawCircle(133f, 308f, 13f, skinShadowPaint)
        canvas.drawCircle(267f, 308f, 13f, skinShadowPaint)

        // Legs (Base skin)
        canvas.drawRoundRect(RectF(156f, 290f, 192f, 428f), 14f, 14f, skinPaint)
        canvas.drawRoundRect(RectF(208f, 290f, 244f, 428f), 14f, 14f, skinPaint)

        // Default base athletic shorts & tank top so character is always modestly dressed even with 0 items
        canvas.drawRoundRect(RectF(146f, 172f, 254f, 296f), 18f, 18f, baseClothPaint)
        canvas.drawRoundRect(RectF(150f, 276f, 195f, 344f), 10f, 10f, baseClothPaint)
        canvas.drawRoundRect(RectF(205f, 276f, 250f, 344f), 10f, 10f, baseClothPaint)

        // Base default trail socks/ankles
        val sockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9AA8A0") }
        canvas.drawRoundRect(RectF(154f, 405f, 194f, 444f), 10f, 10f, sockPaint)
        canvas.drawRoundRect(RectF(206f, 405f, 246f, 444f), 10f, 10f, sockPaint)
    }

    private fun drawBaseExplorerHeadAndHair(canvas: Canvas, config: CharacterConfig) {
        val skinColor = parseColorSafe(config.skinColorHex, Color.parseColor("#E0B187"))
        val skinShadow = darken(skinColor, 0.84f)
        val hairColor = parseColorSafe(config.hairColorHex, Color.parseColor("#3B271A"))
        val eyeColor = parseColorSafe(config.eyeColorHex, Color.parseColor("#2B221E"))

        val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = skinColor }
        val earPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = skinShadow }
        val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = hairColor }

        // Ears
        canvas.drawCircle(150f, 114f, 11f, earPaint)
        canvas.drawCircle(250f, 114f, 11f, earPaint)

        // Head oval
        canvas.drawRoundRect(RectF(154f, 68f, 246f, 160f), 38f, 38f, skinPaint)

        // Eyes
        val eyeWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = eyeColor }
        canvas.drawOval(RectF(172f, 106f, 190f, 120f), eyeWhitePaint)
        canvas.drawOval(RectF(210f, 106f, 228f, 120f), eyeWhitePaint)
        canvas.drawCircle(181f, 113f, 5.2f, pupilPaint)
        canvas.drawCircle(219f, 113f, 5.2f, pupilPaint)

        // Eye shine
        canvas.drawCircle(183f, 111f, 1.8f, eyeWhitePaint)
        canvas.drawCircle(221f, 111f, 1.8f, eyeWhitePaint)

        // Eyebrows
        val browPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darken(hairColor, 0.85f)
            strokeWidth = 4f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(171f, 99f, 190f, 97f, browPaint)
        canvas.drawLine(210f, 97f, 229f, 99f, browPaint)

        // Nose & Confident Explorer Smile
        val featurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darken(skinColor, 0.72f)
            strokeWidth = 3.2f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(200f, 114f, 197f, 127f, featurePaint)
        canvas.drawArc(RectF(186f, 128f, 214f, 144f), 15f, 150f, false, featurePaint)

        // Hair style variations (0..3)
        when (config.hairStyle % 4) {
            0 -> {
                // Short Explorer Crest
                val path = Path().apply {
                    moveTo(152f, 100f)
                    quadTo(155f, 56f, 200f, 58f)
                    quadTo(245f, 56f, 248f, 100f)
                    quadTo(222f, 80f, 200f, 84f)
                    quadTo(178f, 80f, 152f, 100f)
                    close()
                }
                canvas.drawPath(path, hairPaint)
            }
            1 -> {
                // Wavy Side-Swept Trail Hair
                val path = Path().apply {
                    moveTo(148f, 112f)
                    quadTo(150f, 54f, 204f, 56f)
                    quadTo(250f, 58f, 252f, 114f)
                    lineTo(244f, 92f)
                    quadTo(205f, 76f, 160f, 94f)
                    close()
                }
                canvas.drawPath(path, hairPaint)
            }
            2 -> {
                // Adventurer Top Bun + Sidelocks
                canvas.drawCircle(200f, 52f, 16f, hairPaint)
                canvas.drawArc(RectF(152f, 62f, 248f, 118f), 180f, 180f, true, hairPaint)
            }
            else -> {
                // Cropped Buzz Cut
                canvas.drawArc(RectF(154f, 64f, 246f, 112f), 180f, 180f, true, hairPaint)
            }
        }
    }

    private fun drawBackpackFrontStraps(canvas: Canvas, packColor: Int) {
        val strapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darken(packColor, 0.72f)
            style = Paint.Style.FILL
        }
        val bucklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5A93C")
            style = Paint.Style.FILL
        }
        // Left & Right shoulder straps
        canvas.drawRoundRect(RectF(152f, 174f, 168f, 266f), 6f, 6f, strapPaint)
        canvas.drawRoundRect(RectF(232f, 174f, 248f, 266f), 6f, 6f, strapPaint)
        // Sternum chest strap
        canvas.drawRect(164f, 212f, 236f, 220f, strapPaint)
        canvas.drawRoundRect(RectF(194f, 210f, 206f, 222f), 3f, 3f, bucklePaint)
    }

    private fun drawEquippedLayer(
        canvas: Canvas,
        layer: EquippedLayer,
        isHighlighted: Boolean
    ) {
        val item = EquipmentCatalog.findById(layer.itemId) ?: return
        val primaryColor = parseColorSafe(layer.colorHex, parseColorSafe(item.colors.firstOrNull()))
        val darkShade = darken(primaryColor, 0.76f)
        val lightShade = lighten(primaryColor, 0.22f)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            style = Paint.Style.FILL
        }
        val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkShade
            style = Paint.Style.FILL
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lightShade
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darken(primaryColor, 0.55f)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }

        when (layer.slot) {
            LayerSlot.CAMP_GEAR -> {
                // Rolled tent / sleeping bag / foam pad strapped above/below backpack
                canvas.drawRoundRect(RectF(122f, 128f, 278f, 164f), 16f, 16f, fillPaint)
                canvas.drawRoundRect(RectF(122f, 128f, 278f, 164f), 16f, 16f, strokePaint)
                canvas.drawRect(156f, 128f, 166f, 164f, darkPaint)
                canvas.drawRect(234f, 128f, 244f, 164f, darkPaint)
            }
            LayerSlot.BACKPACK -> {
                // Main Backpack behind torso
                canvas.drawRoundRect(RectF(112f, 152f, 288f, 316f), 26f, 26f, darkPaint)
                canvas.drawRoundRect(RectF(120f, 160f, 280f, 306f), 22f, 22f, fillPaint)
                // Side pockets
                canvas.drawRoundRect(RectF(104f, 224f, 124f, 294f), 10f, 10f, accentPaint)
                canvas.drawRoundRect(RectF(276f, 224f, 296f, 294f), 10f, 10f, accentPaint)
            }
            LayerSlot.PANTS -> {
                // Full trekking pants covering waist & both legs
                canvas.drawRoundRect(RectF(146f, 282f, 254f, 326f), 10f, 10f, darkPaint)
                canvas.drawRoundRect(RectF(148f, 304f, 196f, 418f), 10f, 10f, fillPaint)
                canvas.drawRoundRect(RectF(204f, 304f, 252f, 418f), 10f, 10f, fillPaint)
                // Cargo pockets & knee pads
                canvas.drawRoundRect(RectF(144f, 336f, 168f, 372f), 6f, 6f, darkPaint)
                canvas.drawRoundRect(RectF(232f, 336f, 256f, 372f), 6f, 6f, darkPaint)
                canvas.drawRoundRect(RectF(154f, 368f, 190f, 390f), 6f, 6f, accentPaint)
                canvas.drawRoundRect(RectF(210f, 368f, 246f, 390f), 6f, 6f, accentPaint)
            }
            LayerSlot.FOOTWEAR -> {
                // Left & Right Outdoor Boots
                canvas.drawRoundRect(RectF(144f, 408f, 196f, 452f), 12f, 12f, fillPaint)
                canvas.drawRoundRect(RectF(204f, 408f, 256f, 452f), 12f, 12f, fillPaint)
                // Lugged rubber soles
                val solePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B211E") }
                canvas.drawRoundRect(RectF(140f, 442f, 198f, 456f), 6f, 6f, solePaint)
                canvas.drawRoundRect(RectF(202f, 442f, 260f, 456f), 6f, 6f, solePaint)
                // Boot laces
                val lacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E5A93C")
                    strokeWidth = 3f
                }
                canvas.drawLine(162f, 418f, 182f, 418f, lacePaint)
                canvas.drawLine(162f, 426f, 182f, 426f, lacePaint)
                canvas.drawLine(218f, 418f, 238f, 418f, lacePaint)
                canvas.drawLine(218f, 426f, 238f, 426f, lacePaint)
            }
            LayerSlot.UPPER_BODY -> {
                // Long-sleeve trail shirt
                canvas.drawRoundRect(RectF(116f, 174f, 150f, 294f), 14f, 14f, darkPaint)
                canvas.drawRoundRect(RectF(250f, 174f, 284f, 294f), 14f, 14f, darkPaint)
                canvas.drawRoundRect(RectF(144f, 170f, 256f, 294f), 16f, 16f, fillPaint)
                // Collar detail
                canvas.drawRoundRect(RectF(178f, 166f, 222f, 184f), 8f, 8f, accentPaint)
            }
            LayerSlot.OUTERWEAR -> {
                val isVest = item.icon == "vest_utility"
                if (!isVest) {
                    // Jacket sleeves
                    canvas.drawRoundRect(RectF(112f, 172f, 150f, 298f), 16f, 16f, darkPaint)
                    canvas.drawRoundRect(RectF(250f, 172f, 288f, 298f), 16f, 16f, darkPaint)
                }
                // Jacket / Vest body
                canvas.drawRoundRect(RectF(138f, 168f, 262f, 300f), 18f, 18f, fillPaint)
                canvas.drawRoundRect(RectF(138f, 168f, 262f, 300f), 18f, 18f, strokePaint)
                // Center zipper line
                val zipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E5A93C")
                    strokeWidth = 3.5f
                }
                canvas.drawLine(200f, 168f, 200f, 300f, zipPaint)
                // Side zippered pockets
                canvas.drawRoundRect(RectF(150f, 240f, 184f, 278f), 6f, 6f, darkPaint)
                canvas.drawRoundRect(RectF(216f, 240f, 250f, 278f), 6f, 6f, darkPaint)
            }
            LayerSlot.WEATHER_GEAR -> {
                if (item.icon == "neck_gaiter") {
                    canvas.drawRoundRect(RectF(168f, 146f, 232f, 182f), 12f, 12f, fillPaint)
                    canvas.drawRoundRect(RectF(168f, 146f, 232f, 182f), 12f, 12f, strokePaint)
                } else {
                    // Rain poncho cape over shoulders
                    val ponchoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(215, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
                        style = Paint.Style.FILL
                    }
                    val path = Path().apply {
                        moveTo(140f, 168f)
                        lineTo(260f, 168f)
                        lineTo(302f, 326f)
                        lineTo(98f, 326f)
                        close()
                    }
                    canvas.drawPath(path, ponchoPaint)
                    canvas.drawPath(path, strokePaint)
                }
            }
            LayerSlot.HEADWEAR -> {
                when (item.icon) {
                    "hat_brim" -> {
                        // Wide-brim sun hat
                        canvas.drawOval(RectF(122f, 70f, 278f, 98f), darkPaint)
                        canvas.drawRoundRect(RectF(154f, 44f, 246f, 86f), 20f, 20f, fillPaint)
                        canvas.drawRect(154f, 74f, 246f, 82f, accentPaint)
                    }
                    "beanie" -> {
                        // Merino beanie + pom
                        canvas.drawArc(RectF(148f, 42f, 252f, 118f), 180f, 180f, true, fillPaint)
                        canvas.drawRoundRect(RectF(146f, 72f, 254f, 92f), 10f, 10f, darkPaint)
                        canvas.drawCircle(200f, 40f, 10f, accentPaint)
                    }
                    "head_wrap" -> {
                        // Desert protective headscarf
                        canvas.drawRoundRect(RectF(144f, 50f, 256f, 104f), 26f, 26f, fillPaint)
                        canvas.drawRoundRect(RectF(148f, 68f, 252f, 78f), 4f, 4f, darkPaint)
                    }
                    else -> {
                        // Trail visor cap
                        canvas.drawArc(RectF(150f, 46f, 250f, 112f), 180f, 180f, true, fillPaint)
                        canvas.drawRoundRect(RectF(148f, 74f, 268f, 86f), 8f, 8f, darkPaint)
                    }
                }
            }
            LayerSlot.BELT_POUCH -> {
                // Utility belt + supply pouch at waist
                val beltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2B2623") }
                canvas.drawRoundRect(RectF(142f, 282f, 258f, 296f), 6f, 6f, beltPaint)
                canvas.drawRoundRect(RectF(176f, 276f, 224f, 314f), 8f, 8f, fillPaint)
                canvas.drawRoundRect(RectF(176f, 276f, 224f, 314f), 8f, 8f, strokePaint)
                if (item.category.id == "first_aid") {
                    val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                    canvas.drawRect(197f, 285f, 203f, 305f, crossPaint)
                    canvas.drawRect(190f, 292f, 210f, 298f, crossPaint)
                } else {
                    canvas.drawCircle(200f, 295f, 5f, accentPaint)
                }
            }
            LayerSlot.LEFT_HAND_GEAR -> {
                // Left hand / side gear (Water bottle, lantern, flashlight)
                if (item.category.id == "flashlights") {
                    // Glowing camp lantern / torch held in left hand
                    canvas.drawRoundRect(RectF(96f, 306f, 134f, 360f), 10f, 10f, fillPaint)
                    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#FFF2B2")
                    }
                    canvas.drawRoundRect(RectF(103f, 318f, 127f, 346f), 6f, 6f, glowPaint)
                } else {
                    // Insulated bottle / canteen
                    canvas.drawRoundRect(RectF(98f, 298f, 132f, 362f), 12f, 12f, fillPaint)
                    canvas.drawRoundRect(RectF(106f, 286f, 124f, 300f), 4f, 4f, darkPaint)
                    canvas.drawRect(98f, 322f, 132f, 332f, accentPaint)
                }
            }
            LayerSlot.RIGHT_HAND_GEAR -> {
                if (item.icon == "trekking_pole") {
                    // Trekking pole held in right hand down to ground
                    val shaftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = primaryColor
                        strokeWidth = 6f
                        strokeCap = Paint.Cap.ROUND
                    }
                    canvas.drawLine(276f, 240f, 292f, 452f, shaftPaint)
                    val gripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#C88422")
                        strokeWidth = 10f
                        strokeCap = Paint.Cap.ROUND
                    }
                    canvas.drawLine(276f, 244f, 279f, 280f, gripPaint)
                } else {
                    // Rolled topographic map or accessory in right hand
                    canvas.drawRoundRect(RectF(264f, 298f, 304f, 354f), 8f, 8f, fillPaint)
                    canvas.drawRoundRect(RectF(264f, 298f, 304f, 354f), 8f, 8f, strokePaint)
                    canvas.drawLine(270f, 316f, 298f, 316f, strokePaint)
                    canvas.drawLine(270f, 332f, 298f, 332f, strokePaint)
                }
            }
            LayerSlot.NECK_CHEST_GEAR -> {
                // Lanyard + Compass / Binoculars / Headlamp / Glasses on chest
                val cordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = darkShade
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                }
                canvas.drawLine(178f, 168f, 200f, 224f, cordPaint)
                canvas.drawLine(222f, 168f, 200f, 224f, cordPaint)
                canvas.drawCircle(200f, 232f, 16f, fillPaint)
                canvas.drawCircle(200f, 232f, 16f, strokePaint)
                canvas.drawCircle(200f, 232f, 8f, accentPaint)
            }
            LayerSlot.DECORATIVE_PATCH -> {
                // Chest merit badge / patch
                canvas.drawRoundRect(RectF(214f, 192f, 244f, 222f), 8f, 8f, fillPaint)
                canvas.drawRoundRect(RectF(214f, 192f, 244f, 222f), 8f, 8f, strokePaint)
                canvas.drawCircle(229f, 207f, 6f, accentPaint)
            }
        }

        // Optional selection ring if the user tapped this layer
        if (isHighlighted) {
            val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E5A93C")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawRoundRect(RectF(92f, 36f, 308f, 460f), 20f, 20f, highlightPaint)
        }
    }
}
