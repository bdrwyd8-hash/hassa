package com.example.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.data.EquipmentCatalog
import com.example.model.CharacterConfig
import com.example.model.LayerSlot
import com.example.ui.canvas.CharacterBitmapRenderer
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity top-down / 2.5D angled human character renderer.
 * Draws recognizable human explorers (player and AI opponents) with animated swinging limbs,
 * equipped backpacks, jackets, pants, boots, hairstyles, headwear, and cast shadows.
 */
object GameCharacterRenderer {

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 10, 15, 12)
        style = Paint.Style.FILL
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
    }

    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun drawPlayer(
        canvas: Canvas,
        player: SurvivalPlayer,
        config: CharacterConfig,
        screenX: Float,
        screenY: Float,
        scale: Float = 1.0f
    ) {
        canvas.save()
        canvas.translate(screenX, screenY)
        canvas.scale(scale, scale)

        // 1. Soft Ground Drop Shadow
        canvas.drawOval(RectF(-20f, 16f, 24f, 32f), shadowPaint)

        // Rotate character body toward facing angle
        canvas.rotate(player.facingDegrees)

        val skinColor = CharacterBitmapRenderer.parseColorSafe(config.skinColorHex, Color.parseColor("#E0B187"))
        val hairColor = CharacterBitmapRenderer.parseColorSafe(config.hairColorHex, Color.parseColor("#3B271A"))

        // Equipped loadout colors from Studio
        val upperLayer = config.equippedLayers[LayerSlot.OUTERWEAR] ?: config.equippedLayers[LayerSlot.UPPER_BODY]
        val shirtColor = upperLayer?.let { CharacterBitmapRenderer.parseColorSafe(it.colorHex) }
            ?: Color.parseColor("#4E8768")

        val pantsLayer = config.equippedLayers[LayerSlot.PANTS]
        val pantsColor = pantsLayer?.let { CharacterBitmapRenderer.parseColorSafe(it.colorHex) }
            ?: Color.parseColor("#5A4D41")

        val bootsLayer = config.equippedLayers[LayerSlot.FOOTWEAR]
        val bootsColor = bootsLayer?.let { CharacterBitmapRenderer.parseColorSafe(it.colorHex) }
            ?: Color.parseColor("#3A2618")

        val packLayer = config.equippedLayers[LayerSlot.BACKPACK]
        val packColor = packLayer?.let { CharacterBitmapRenderer.parseColorSafe(it.colorHex) }
            ?: Color.parseColor("#2D543F")

        val headwearLayer = config.equippedLayers[LayerSlot.HEADWEAR]
        val hatColor = headwearLayer?.let { CharacterBitmapRenderer.parseColorSafe(it.colorHex) }

        // Walk cycle animation offsets
        val stride = if (player.isMoving) (if (player.isRunning) 14f else 9f) else 0f
        val leftLegY = sin(player.animPhase.toDouble()).toFloat() * stride
        val rightLegY = -leftLegY
        val armSwing = if (player.isMoving) sin(player.animPhase.toDouble()).toFloat() * (stride * 0.9f) else 0f

        // 2. Legs & Boots (drawn first, below torso)
        drawLegAndBoot(canvas, isLeft = true, offsetY = leftLegY, pantsColor = pantsColor, bootsColor = bootsColor)
        drawLegAndBoot(canvas, isLeft = false, offsetY = rightLegY, pantsColor = pantsColor, bootsColor = bootsColor)

        // 3. Torso & Upper Clothing
        fillPaint.color = shirtColor
        canvas.drawRoundRect(RectF(-16f, -14f, 16f, 14f), 8f, 8f, fillPaint)

        // Zipper / collar trim
        strokePaint.color = Color.argb(160, 229, 169, 60)
        strokePaint.strokeWidth = 2.5f
        canvas.drawLine(0f, -14f, 0f, 14f, strokePaint)

        // 4. Backpack on the back (upper Y is back in top-down orientation)
        fillPaint.color = packColor
        canvas.drawRoundRect(RectF(-14f, 6f, 14f, 22f), 6f, 6f, fillPaint)
        strokePaint.color = Color.argb(180, 20, 30, 25)
        strokePaint.strokeWidth = 2f
        canvas.drawRoundRect(RectF(-14f, 6f, 14f, 22f), 6f, 6f, strokePaint)

        // Bedroll / sleeping bag strapped to pack
        detailPaint.color = Color.parseColor("#E5A93C")
        canvas.drawRoundRect(RectF(-16f, 18f, 16f, 26f), 4f, 4f, detailPaint)

        // Shoulder straps
        detailPaint.color = Color.argb(180, 40, 50, 45)
        canvas.drawRect(-12f, -14f, -7f, 12f, detailPaint)
        canvas.drawRect(7f, -14f, 12f, 12f, detailPaint)

        // 5. Arms & Hands
        drawArmAndHand(canvas, isLeft = true, offsetY = -armSwing, skinColor = skinColor, sleeveColor = shirtColor)
        drawArmAndHand(canvas, isLeft = false, offsetY = armSwing, skinColor = skinColor, sleeveColor = shirtColor)

        // 6. Flashlight held in right hand casting beam forward
        if (player.flashlightOn) {
            drawHandheldFlashlight(canvas, handX = 18f, handY = -12f + armSwing)
        }

        // 7. Head, Ears, and Face
        fillPaint.color = skinColor
        canvas.drawCircle(0f, -6f, 13f, fillPaint) // Head circle

        // Ears
        fillPaint.color = Color.argb(200, Color.red(skinColor) * 4 / 5, Color.green(skinColor) * 4 / 5, Color.blue(skinColor) * 4 / 5)
        canvas.drawCircle(-13f, -6f, 3.5f, fillPaint)
        canvas.drawCircle(13f, -6f, 3.5f, fillPaint)

        // Nose pointing in facing direction (towards -Y, which is front)
        fillPaint.color = skinColor
        canvas.drawCircle(0f, -17f, 4.5f, fillPaint)

        // Eyes
        detailPaint.color = Color.parseColor("#222824")
        canvas.drawCircle(-5f, -13f, 2f, detailPaint)
        canvas.drawCircle(5f, -13f, 2f, detailPaint)

        // 8. Hair & Headwear
        if (hatColor != null) {
            // Equipped Hat
            fillPaint.color = hatColor
            canvas.drawCircle(0f, -7f, 14f, fillPaint)
            // Visor / brim
            val brimPath = Path().apply {
                moveTo(-13f, -9f)
                quadTo(0f, -22f, 13f, -9f)
                close()
            }
            canvas.drawPath(brimPath, fillPaint)
        } else {
            // Custom Hair Style
            fillPaint.color = hairColor
            when (config.hairStyle % 4) {
                0 -> {
                    // Short Explorer crop
                    canvas.drawArc(RectF(-13f, -18f, 13f, 2f), 170f, 200f, true, fillPaint)
                }
                1 -> {
                    // Wavy trail hair
                    canvas.drawArc(RectF(-14f, -17f, 14f, 5f), 160f, 220f, true, fillPaint)
                }
                2 -> {
                    // Tied Bun
                    canvas.drawCircle(0f, 2f, 6f, fillPaint)
                    canvas.drawArc(RectF(-12f, -18f, 12f, 0f), 180f, 180f, true, fillPaint)
                }
                else -> {
                    // Buzz cut
                    canvas.drawArc(RectF(-12f, -16f, 12f, -2f), 180f, 180f, true, fillPaint)
                }
            }
        }

        canvas.restore()
    }

    fun drawBot(
        canvas: Canvas,
        bot: BotCharacter,
        screenX: Float,
        screenY: Float,
        scale: Float = 1.0f
    ) {
        canvas.save()
        canvas.translate(screenX, screenY)
        canvas.scale(scale, scale)

        // 1. Soft Ground Drop Shadow
        canvas.drawOval(RectF(-19f, 14f, 23f, 30f), shadowPaint)

        // Rotate bot body toward facing angle
        canvas.rotate(bot.facingDegrees)

        val primaryColor = Color.parseColor(bot.primaryColorHex)
        val vestColor = Color.parseColor(bot.vestColorHex)
        val hatColor = Color.parseColor(bot.hatColorHex)
        val skinColor = Color.parseColor(bot.skinColorHex)
        val bootsColor = Color.parseColor("#2A2420")

        // Walk cycle animation
        val stride = if (bot.isMoving) 9f else 0f
        val leftLegY = sin(bot.animPhase.toDouble()).toFloat() * stride
        val rightLegY = -leftLegY
        val armSwing = if (bot.isMoving) sin(bot.animPhase.toDouble()).toFloat() * (stride * 0.8f) else 0f

        // 2. Legs & Boots
        drawLegAndBoot(canvas, isLeft = true, offsetY = leftLegY, pantsColor = primaryColor, bootsColor = bootsColor)
        drawLegAndBoot(canvas, isLeft = false, offsetY = rightLegY, pantsColor = primaryColor, bootsColor = bootsColor)

        // 3. Torso & Scavenger Vest
        fillPaint.color = primaryColor
        canvas.drawRoundRect(RectF(-15f, -13f, 15f, 13f), 7f, 7f, fillPaint)

        // Tactical Vest
        fillPaint.color = vestColor
        canvas.drawRoundRect(RectF(-13f, -10f, 13f, 10f), 5f, 5f, fillPaint)

        // Small utility pack on back
        fillPaint.color = Color.parseColor("#28221D")
        canvas.drawRoundRect(RectF(-11f, 7f, 11f, 18f), 4f, 4f, fillPaint)

        // 4. Arms & Hands
        drawArmAndHand(canvas, isLeft = true, offsetY = -armSwing, skinColor = skinColor, sleeveColor = primaryColor)
        drawArmAndHand(canvas, isLeft = false, offsetY = armSwing, skinColor = skinColor, sleeveColor = primaryColor)

        // 5. Head
        fillPaint.color = skinColor
        canvas.drawCircle(0f, -6f, 12f, fillPaint)

        // Nose
        fillPaint.color = skinColor
        canvas.drawCircle(0f, -16f, 4f, fillPaint)

        // Eyes
        detailPaint.color = Color.parseColor("#1C1E1B")
        canvas.drawCircle(-4.5f, -12f, 1.8f, detailPaint)
        canvas.drawCircle(4.5f, -12f, 1.8f, detailPaint)

        // 6. Hat / Patrol Cap
        fillPaint.color = hatColor
        canvas.drawCircle(0f, -7f, 13f, fillPaint)
        val brimPath = Path().apply {
            moveTo(-12f, -8f)
            quadTo(0f, -20f, 12f, -8f)
            close()
        }
        canvas.drawPath(brimPath, fillPaint)

        canvas.restore()

        // 7. Tactical AI Overhead Status Badge (rendered unrotated above the bot's head)
        drawBotStateBadge(canvas, bot, screenX, screenY - 34f)
    }

    private fun drawLegAndBoot(
        canvas: Canvas,
        isLeft: Boolean,
        offsetY: Float,
        pantsColor: Int,
        bootsColor: Int
    ) {
        val x = if (isLeft) -8f else 8f

        // Leg / Pants fabric
        fillPaint.color = pantsColor
        canvas.drawRoundRect(RectF(x - 4.5f, -4f + offsetY, x + 4.5f, 14f + offsetY), 4f, 4f, fillPaint)

        // Boot
        fillPaint.color = bootsColor
        canvas.drawRoundRect(RectF(x - 5f, 10f + offsetY, x + 5f, 21f + offsetY), 4.5f, 4.5f, fillPaint)

        // Boot tread sole
        detailPaint.color = Color.parseColor("#151816")
        canvas.drawRect(x - 4.5f, 19f + offsetY, x + 4.5f, 22f + offsetY, detailPaint)
    }

    private fun drawArmAndHand(
        canvas: Canvas,
        isLeft: Boolean,
        offsetY: Float,
        skinColor: Int,
        sleeveColor: Int
    ) {
        val x = if (isLeft) -18f else 18f

        // Sleeve
        fillPaint.color = sleeveColor
        canvas.drawRoundRect(RectF(x - 4f, -12f + offsetY, x + 4f, 3f + offsetY), 4f, 4f, fillPaint)

        // Bare forearm / hand
        fillPaint.color = skinColor
        canvas.drawRoundRect(RectF(x - 3.5f, 2f + offsetY, x + 3.5f, 12f + offsetY), 3.5f, 3.5f, fillPaint)
    }

    private fun drawHandheldFlashlight(canvas: Canvas, handX: Float, handY: Float) {
        // Flashlight body in hand
        detailPaint.color = Color.parseColor("#2A312D")
        canvas.drawRoundRect(RectF(handX - 3f, handY - 14f, handX + 3f, handY), 2.5f, 2.5f, detailPaint)

        // Brass lens ring
        detailPaint.color = Color.parseColor("#E5A93C")
        canvas.drawRect(handX - 3.5f, handY - 16f, handX + 3.5f, handY - 13f, detailPaint)

        // Soft Forward Light Cone
        val beamPath = Path().apply {
            moveTo(handX, handY - 16f)
            lineTo(handX - 45f, handY - 210f)
            lineTo(handX + 45f, handY - 210f)
            close()
        }
        val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 248, 200)
            style = Paint.Style.FILL
        }
        canvas.drawPath(beamPath, beamPaint)
    }

    private fun drawBotStateBadge(canvas: Canvas, bot: BotCharacter, x: Float, y: Float) {
        val badgeColor = Color.parseColor(bot.state.badgeColorHex)

        // Small tactical pill
        fillPaint.color = Color.argb(220, 20, 26, 23)
        canvas.drawRoundRect(RectF(x - 22f, y - 9f, x + 22f, y + 9f), 6f, 6f, fillPaint)

        strokePaint.color = badgeColor
        strokePaint.strokeWidth = 2f
        canvas.drawRoundRect(RectF(x - 22f, y - 9f, x + 22f, y + 9f), 6f, 6f, strokePaint)

        // Icon indicator inside badge
        fillPaint.color = badgeColor
        when (bot.state) {
            com.example.model.BotAiState.PURSUE -> {
                // Exclamation / Alert icon
                canvas.drawRect(x - 1.5f, y - 5f, x + 1.5f, y + 1f, fillPaint)
                canvas.drawCircle(x, y + 4.5f, 1.6f, fillPaint)
            }
            com.example.model.BotAiState.SEARCH_LAST_KNOWN -> {
                // Question mark
                canvas.drawCircle(x, y - 2f, 3.5f, strokePaint)
                canvas.drawCircle(x, y + 4.5f, 1.6f, fillPaint)
            }
            com.example.model.BotAiState.FLANK_COORDINATE -> {
                // Two flank arrows
                canvas.drawCircle(x - 6f, y, 2.5f, fillPaint)
                canvas.drawCircle(x + 6f, y, 2.5f, fillPaint)
            }
            com.example.model.BotAiState.RETREAT_COVER -> {
                // Shield / Cover
                canvas.drawRoundRect(RectF(x - 4f, y - 5f, x + 4f, y + 5f), 2f, 2f, strokePaint)
            }
            else -> {
                // Patrol dot
                canvas.drawCircle(x, y, 3f, fillPaint)
            }
        }
    }
}
