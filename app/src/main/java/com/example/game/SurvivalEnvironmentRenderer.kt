package com.example.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.example.data.EquipmentCatalog
import com.example.ui.canvas.CharacterBitmapRenderer
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the rich natural wilderness environment:
 * Layered terrain, winding dirt hiking paths, flowing streams, boulders,
 * trees with realistic trunks and overlapping canopies (with player occlusion fading),
 * abandoned ranger cabin, campsite, supply caches, and illuminated extraction pad.
 */
object SurvivalEnvironmentRenderer {

    private val terrainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 8, 14, 10)
        style = Paint.Style.FILL
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun drawTerrainAndPaths(
        canvas: Canvas,
        cameraX: Float,
        cameraY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        zoom: Float,
        worldWidth: Float,
        worldHeight: Float,
        windTime: Float = 0f
    ) {
        // Base Forest Grass Background
        terrainPaint.color = Color.parseColor("#1B2921")
        canvas.drawRect(0f, 0f, viewportWidth, viewportHeight, terrainPaint)

        // Subtle large grassy color patches
        val grassPatches = listOf(
            Triple(300f, 400f, 500f),
            Triple(1100f, 600f, 650f),
            Triple(1800f, 1100f, 600f),
            Triple(700f, 1300f, 550f)
        )
        terrainPaint.color = Color.parseColor("#223329")
        grassPatches.forEach { (wx, wy, rad) ->
            val sx = (wx - cameraX) * zoom + viewportWidth / 2f
            val sy = (wy - cameraY) * zoom + viewportHeight / 2f
            canvas.drawCircle(sx, sy, rad * zoom, terrainPaint)
        }

        // Winding Dirt Hiking Trails (from entrance through forest to cabin and extraction)
        drawWindingDirtPath(canvas, cameraX, cameraY, viewportWidth, viewportHeight, zoom)

        // Flowing Stream & Water Ponds
        drawWildernessWaterStream(canvas, cameraX, cameraY, viewportWidth, viewportHeight, zoom, windTime)
    }

    private fun drawWindingDirtPath(
        canvas: Canvas,
        cameraX: Float,
        cameraY: Float,
        vw: Float,
        vh: Float,
        zoom: Float
    ) {
        val pathWaypoints = listOf(
            Pair(240f, 1550f),
            Pair(420f, 1380f),
            Pair(680f, 1300f),
            Pair(940f, 1120f),
            Pair(1200f, 960f), // Cabin clearing
            Pair(1460f, 820f),
            Pair(1720f, 640f),
            Pair(1950f, 440f),
            Pair(2100f, 320f)  // Extraction Zone
        )

        // Dirt path stroke (wider brown underlayer)
        val trailPath = Path()
        pathWaypoints.forEachIndexed { index, (wx, wy) ->
            val sx = (wx - cameraX) * zoom + vw / 2f
            val sy = (wy - cameraY) * zoom + vh / 2f
            if (index == 0) trailPath.moveTo(sx, sy) else trailPath.lineTo(sx, sy)
        }

        // Broad dirt path base
        val dirtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#443A30")
            style = Paint.Style.STROKE
            strokeWidth = 68f * zoom
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(trailPath, dirtPaint)

        // Narrower packed trail center
        val packDirtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#584B3E")
            style = Paint.Style.STROKE
            strokeWidth = 44f * zoom
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(trailPath, packDirtPaint)

        // Small pebbles along trail
        detailPaint.color = Color.parseColor("#74675A")
        val pebblePoints = listOf(
            Pair(350f, 1420f), Pair(520f, 1350f), Pair(800f, 1220f),
            Pair(1080f, 1040f), Pair(1350f, 880f), Pair(1600f, 720f), Pair(1850f, 520f)
        )
        pebblePoints.forEach { (wx, wy) ->
            val sx = (wx - cameraX) * zoom + vw / 2f
            val sy = (wy - cameraY) * zoom + vh / 2f
            canvas.drawCircle(sx, sy, 4f * zoom, detailPaint)
        }
    }

    private fun drawWildernessWaterStream(
        canvas: Canvas,
        cameraX: Float,
        cameraY: Float,
        vw: Float,
        vh: Float,
        zoom: Float,
        time: Float
    ) {
        val streamPoints = listOf(
            Pair(1300f, 1800f),
            Pair(1380f, 1400f),
            Pair(1420f, 1050f),
            Pair(1520f, 750f),
            Pair(1600f, 400f),
            Pair(1650f, 0f)
        )
        val streamPath = Path()
        streamPoints.forEachIndexed { index, (wx, wy) ->
            val sx = (wx - cameraX) * zoom + vw / 2f
            val sy = (wy - cameraY) * zoom + vh / 2f
            if (index == 0) streamPath.moveTo(sx, sy) else streamPath.lineTo(sx, sy)
        }

        // Muddy riverbank edge
        val bankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#322B24")
            style = Paint.Style.STROKE
            strokeWidth = 72f * zoom
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(streamPath, bankPaint)

        // Flowing translucent blue stream
        val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#264A62")
            style = Paint.Style.STROKE
            strokeWidth = 46f * zoom
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(streamPath, waterPaint)

        // Water foam & ripples
        val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 160, 205, 230)
            style = Paint.Style.STROKE
            strokeWidth = 4f * zoom
        }
        val rippleOffset = sin(time * 3f) * 6f * zoom
        canvas.drawPath(streamPath, ripplePaint)

        // Wooden Footbridge over stream near cabin (1420f, 1050f)
        val bridgeX = (1420f - cameraX) * zoom + vw / 2f
        val bridgeY = (1050f - cameraY) * zoom + vh / 2f
        val bridgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5A432F")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(bridgeX - 32f * zoom, bridgeY - 24f * zoom, bridgeX + 32f * zoom, bridgeY + 24f * zoom),
            4f * zoom,
            4f * zoom,
            bridgePaint
        )
        detailPaint.color = Color.parseColor("#3B291C")
        for (i in -3..3) {
            val plankX = bridgeX + i * 8f * zoom
            canvas.drawLine(plankX, bridgeY - 22f * zoom, plankX, bridgeY + 22f * zoom, detailPaint)
        }
    }

    fun drawSolidObstaclesAndCampsite(
        canvas: Canvas,
        obstacles: List<WorldObstacle>,
        cameraX: Float,
        cameraY: Float,
        vw: Float,
        vh: Float,
        zoom: Float,
        time: Float = 0f
    ) {
        obstacles.forEach { obs ->
            val sx = (obs.x - cameraX) * zoom + vw / 2f
            val sy = (obs.y - cameraY) * zoom + vh / 2f

            when (obs.type) {
                ObstacleType.ROCK_BOULDER -> {
                    // Soft cast shadow
                    canvas.drawOval(
                        RectF(sx - obs.radius * 0.9f * zoom, sy + obs.radius * 0.3f * zoom, sx + obs.radius * 1.3f * zoom, sy + obs.radius * 1.1f * zoom),
                        shadowPaint
                    )
                    // Shaded Rock Body
                    terrainPaint.color = Color.parseColor("#48534C")
                    canvas.drawRoundRect(
                        RectF(sx - obs.radius * zoom, sy - obs.radius * 0.85f * zoom, sx + obs.radius * zoom, sy + obs.radius * 0.85f * zoom),
                        obs.radius * 0.6f * zoom,
                        obs.radius * 0.6f * zoom,
                        terrainPaint
                    )
                    // Rock highlighted facet
                    detailPaint.color = Color.parseColor("#626F67")
                    canvas.drawOval(
                        RectF(sx - obs.radius * 0.6f * zoom, sy - obs.radius * 0.65f * zoom, sx + obs.radius * 0.4f * zoom, sy),
                        detailPaint
                    )
                    // Moss patch
                    detailPaint.color = Color.parseColor("#38533F")
                    canvas.drawCircle(sx + obs.radius * 0.3f * zoom, sy + obs.radius * 0.2f * zoom, 10f * zoom, detailPaint)
                }

                ObstacleType.CABIN_WALL -> {
                    // Abandoned Ranger Cabin Base & Porch
                    canvas.drawRoundRect(
                        RectF(sx - 80f * zoom, sy - 50f * zoom + 12f * zoom, sx + 85f * zoom, sy + 65f * zoom),
                        8f * zoom, 8f * zoom, shadowPaint
                    )
                    // Wooden Wall Log construction
                    terrainPaint.color = Color.parseColor("#4E3A29")
                    canvas.drawRoundRect(
                        RectF(sx - 75f * zoom, sy - 50f * zoom, sx + 75f * zoom, sy + 50f * zoom),
                        6f * zoom, 6f * zoom, terrainPaint
                    )
                    // Cabin floor planks
                    detailPaint.color = Color.parseColor("#654C37")
                    canvas.drawRect(
                        RectF(sx - 68f * zoom, sy - 42f * zoom, sx + 68f * zoom, sy + 42f * zoom),
                        detailPaint
                    )
                    // Open door entrance at front
                    detailPaint.color = Color.parseColor("#261911")
                    canvas.drawRect(
                        RectF(sx - 16f * zoom, sy + 40f * zoom, sx + 16f * zoom, sy + 52f * zoom),
                        detailPaint
                    )
                }

                ObstacleType.CAMPFIRE -> {
                    // Stones ring
                    detailPaint.color = Color.parseColor("#444B46")
                    canvas.drawCircle(sx, sy, 22f * zoom, detailPaint)
                    // Ash pit
                    detailPaint.color = Color.parseColor("#1B1E1C")
                    canvas.drawCircle(sx, sy, 15f * zoom, detailPaint)
                    // Fire embers with flame flicker
                    val flicker = (sin(time * 8f) + 1f) * 0.5f
                    val flameColor = if (flicker > 0.5f) Color.parseColor("#E5A93C") else Color.parseColor("#C84E29")
                    detailPaint.color = flameColor
                    canvas.drawCircle(sx, sy, (7f + flicker * 5f) * zoom, detailPaint)
                }

                ObstacleType.BUSH_DENSE -> {
                    // Dense Camouflage Bush (Player enters for stealth)
                    canvas.drawOval(
                        RectF(sx - obs.radius * zoom, sy + obs.radius * 0.2f * zoom, sx + obs.radius * 1.1f * zoom, sy + obs.radius * 0.9f * zoom),
                        shadowPaint
                    )
                    terrainPaint.color = Color.parseColor("#264532")
                    canvas.drawCircle(sx, sy, obs.radius * zoom, terrainPaint)
                    // Leaf clumps
                    detailPaint.color = Color.parseColor("#366247")
                    canvas.drawCircle(sx - obs.radius * 0.3f * zoom, sy - obs.radius * 0.2f * zoom, obs.radius * 0.55f * zoom, detailPaint)
                    canvas.drawCircle(sx + obs.radius * 0.35f * zoom, sy + obs.radius * 0.15f * zoom, obs.radius * 0.5f * zoom, detailPaint)
                    // Red trail berries
                    detailPaint.color = Color.parseColor("#D9534F")
                    canvas.drawCircle(sx - 8f * zoom, sy - 6f * zoom, 3f * zoom, detailPaint)
                    canvas.drawCircle(sx + 10f * zoom, sy - 2f * zoom, 3f * zoom, detailPaint)
                    canvas.drawCircle(sx + 2f * zoom, sy + 10f * zoom, 3f * zoom, detailPaint)
                }

                ObstacleType.TREE_PINE, ObstacleType.TREE_OAK -> {
                    // Draw Tree Trunk Base and shadow on the ground
                    canvas.drawOval(
                        RectF(sx - obs.radius * 1.1f * zoom, sy + obs.radius * 0.3f * zoom, sx + obs.radius * 1.8f * zoom, sy + obs.radius * 1.3f * zoom),
                        shadowPaint
                    )
                    // Visible Tree Trunk
                    terrainPaint.color = Color.parseColor("#3E2D20")
                    canvas.drawCircle(sx, sy, obs.radius * zoom, terrainPaint)
                    // Roots spreading out
                    detailPaint.color = Color.parseColor("#2D2016")
                    canvas.drawCircle(sx, sy, obs.radius * 0.65f * zoom, detailPaint)
                }

                else -> {}
            }
        }
    }

    fun drawSupplyCaches(
        canvas: Canvas,
        supplies: List<WorldSupplyItem>,
        cameraX: Float,
        cameraY: Float,
        vw: Float,
        vh: Float,
        zoom: Float,
        time: Float = 0f
    ) {
        supplies.filter { !it.collected }.forEach { item ->
            val sx = (item.x - cameraX) * zoom + vw / 2f
            val sy = (item.y - cameraY) * zoom + vh / 2f

            // Ground drop shadow
            canvas.drawOval(
                RectF(sx - 18f * zoom, sy + 10f * zoom, sx + 20f * zoom, sy + 22f * zoom),
                shadowPaint
            )

            if (item.isPrimaryObjective) {
                // Primary Objective: Military Beacon Supply Crate (Amber / Olive with blinking LED)
                val pulse = (sin(time * 5f) + 1f) * 0.5f

                // Outer pulsating beacon ring
                outlinePaint.color = Color.argb((110 + pulse * 100).toInt(), 229, 169, 60)
                outlinePaint.strokeWidth = (2.5f + pulse * 1.5f) * zoom
                canvas.drawCircle(sx, sy, (26f + pulse * 8f) * zoom, outlinePaint)

                // Heavy Waterproof Military Pelican Crate
                terrainPaint.color = Color.parseColor("#28392F")
                canvas.drawRoundRect(
                    RectF(sx - 18f * zoom, sy - 14f * zoom, sx + 18f * zoom, sy + 14f * zoom),
                    5f * zoom, 5f * zoom, terrainPaint
                )
                // Crate reinforced corner bumpers
                detailPaint.color = Color.parseColor("#E5A93C")
                canvas.drawRect(sx - 18f * zoom, sy - 14f * zoom, sx - 12f * zoom, sy - 8f * zoom, detailPaint)
                canvas.drawRect(sx + 12f * zoom, sy - 14f * zoom, sx + 18f * zoom, sy - 8f * zoom, detailPaint)
                canvas.drawRect(sx - 18f * zoom, sy + 8f * zoom, sx - 12f * zoom, sy + 14f * zoom, detailPaint)
                canvas.drawRect(sx + 12f * zoom, sy + 8f * zoom, sx + 18f * zoom, sy + 14f * zoom, detailPaint)

                // Stenciled star/compass emblem in center
                detailPaint.color = Color.parseColor("#E5A93C")
                canvas.drawCircle(sx, sy, 4.5f * zoom, detailPaint)

                // Blinking Beacon Antenna LED
                detailPaint.color = if (pulse > 0.4f) Color.parseColor("#FFD54F") else Color.parseColor("#C68422")
                canvas.drawCircle(sx + 12f * zoom, sy - 10f * zoom, 3f * zoom, detailPaint)
            } else {
                // Secondary Supply: Medical Pouch / Water Jug / Ration Pack
                when {
                    item.itemId.startsWith("aid-") -> {
                        // Red Cross Medical Kit
                        terrainPaint.color = Color.parseColor("#D9534F")
                        canvas.drawRoundRect(
                            RectF(sx - 14f * zoom, sy - 11f * zoom, sx + 14f * zoom, sy + 11f * zoom),
                            4f * zoom, 4f * zoom, terrainPaint
                        )
                        detailPaint.color = Color.WHITE
                        canvas.drawRect(sx - 2f * zoom, sy - 7f * zoom, sx + 2f * zoom, sy + 7f * zoom, detailPaint)
                        canvas.drawRect(sx - 7f * zoom, sy - 2f * zoom, sx + 7f * zoom, sy + 2f * zoom, detailPaint)
                    }
                    item.itemId.startsWith("water-") -> {
                        // Steel Flask Canteen
                        terrainPaint.color = Color.parseColor("#3B5978")
                        canvas.drawRoundRect(
                            RectF(sx - 11f * zoom, sy - 14f * zoom, sx + 11f * zoom, sy + 14f * zoom),
                            6f * zoom, 6f * zoom, terrainPaint
                        )
                        detailPaint.color = Color.parseColor("#E5A93C")
                        canvas.drawRect(sx - 5f * zoom, sy - 17f * zoom, sx + 5f * zoom, sy - 13f * zoom, detailPaint)
                    }
                    else -> {
                        // Trail Ration pack
                        terrainPaint.color = Color.parseColor("#C88422")
                        canvas.drawRoundRect(
                            RectF(sx - 13f * zoom, sy - 10f * zoom, sx + 13f * zoom, sy + 10f * zoom),
                            3f * zoom, 3f * zoom, terrainPaint
                        )
                    }
                }
            }
        }
    }

    fun drawExtractionZone(
        canvas: Canvas,
        zone: ExtractionZone,
        allCachesSecured: Boolean,
        cameraX: Float,
        cameraY: Float,
        vw: Float,
        vh: Float,
        zoom: Float,
        time: Float = 0f
    ) {
        val sx = (zone.x - cameraX) * zoom + vw / 2f
        val sy = (zone.y - cameraY) * zoom + vh / 2f

        val pulse = (sin(time * 4f) + 1f) * 0.5f
        val activeColor = if (allCachesSecured) Color.parseColor("#5CB85C") else Color.parseColor("#E5A93C")

        // Outer helipad / extraction ring
        outlinePaint.color = Color.argb(120, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor))
        outlinePaint.strokeWidth = 4f * zoom
        canvas.drawCircle(sx, sy, zone.radius * zoom, outlinePaint)

        // Inner landing circle with pulse
        outlinePaint.color = activeColor
        outlinePaint.strokeWidth = 3f * zoom
        canvas.drawCircle(sx, sy, (zone.radius * 0.7f + pulse * 6f) * zoom, outlinePaint)

        // Center Extraction "H" Landing Mark
        val hSize = 28f * zoom
        val hThick = 6f * zoom
        detailPaint.color = activeColor
        // Left bar
        canvas.drawRect(sx - hSize / 2, sy - hSize / 2, sx - hSize / 2 + hThick, sy + hSize / 2, detailPaint)
        // Right bar
        canvas.drawRect(sx + hSize / 2 - hThick, sy - hSize / 2, sx + hSize / 2, sy + hSize / 2, detailPaint)
        // Cross bar
        canvas.drawRect(sx - hSize / 2, sy - hThick / 2, sx + hSize / 2, sy + hThick / 2, detailPaint)

        // Four illuminated boundary corner beacons
        val cornerDist = zone.radius * 0.85f * zoom
        val beaconColors = if (allCachesSecured) Color.parseColor("#63A681") else Color.parseColor("#E5A93C")
        detailPaint.color = beaconColors
        canvas.drawCircle(sx - cornerDist, sy - cornerDist, 5f * zoom, detailPaint)
        canvas.drawCircle(sx + cornerDist, sy - cornerDist, 5f * zoom, detailPaint)
        canvas.drawCircle(sx - cornerDist, sy + cornerDist, 5f * zoom, detailPaint)
        canvas.drawCircle(sx + cornerDist, sy + cornerDist, 5f * zoom, detailPaint)
    }

    /**
     * Draws overhead objects (Tree Canopies, Cabin Roof) that appear ABOVE characters.
     * When the player walks underneath, canopy smoothly fades to 45% alpha for full visibility!
     */
    fun drawOverheadCanopies(
        canvas: Canvas,
        obstacles: List<WorldObstacle>,
        playerX: Float,
        playerY: Float,
        cameraX: Float,
        cameraY: Float,
        vw: Float,
        vh: Float,
        zoom: Float
    ) {
        obstacles.forEach { obs ->
            if (obs.canopyRadius <= 0f) return@forEach
            val sx = (obs.x - cameraX) * zoom + vw / 2f
            val sy = (obs.y - 30f - cameraY) * zoom + vh / 2f // Canopy offset slightly north in 2.5D

            val isPlayerUnderneath = obs.isUnderCanopy(playerX, playerY)
            val alpha = if (isPlayerUnderneath) 115 else 255

            when (obs.type) {
                ObstacleType.TREE_PINE -> {
                    // Layered Pine Needle Canopy
                    terrainPaint.color = Color.argb(alpha, 28, 54, 40)
                    canvas.drawCircle(sx, sy, obs.canopyRadius * zoom, terrainPaint)
                    // Inner lighter pine tier
                    detailPaint.color = Color.argb(alpha, 42, 78, 58)
                    canvas.drawCircle(sx, sy - 12f * zoom, obs.canopyRadius * 0.72f * zoom, detailPaint)
                    // Crown top
                    detailPaint.color = Color.argb(alpha, 58, 104, 78)
                    canvas.drawCircle(sx, sy - 24f * zoom, obs.canopyRadius * 0.44f * zoom, detailPaint)
                }

                ObstacleType.TREE_OAK -> {
                    // Full Leafy Oak Canopy with layered foliage lobes
                    terrainPaint.color = Color.argb(alpha, 34, 62, 44)
                    canvas.drawCircle(sx, sy, obs.canopyRadius * zoom, terrainPaint)
                    detailPaint.color = Color.argb(alpha, 48, 88, 62)
                    canvas.drawCircle(sx - obs.canopyRadius * 0.28f * zoom, sy - obs.canopyRadius * 0.2f * zoom, obs.canopyRadius * 0.6f * zoom, detailPaint)
                    canvas.drawCircle(sx + obs.canopyRadius * 0.32f * zoom, sy + obs.canopyRadius * 0.15f * zoom, obs.canopyRadius * 0.55f * zoom, detailPaint)
                    canvas.drawCircle(sx + 5f * zoom, sy - obs.canopyRadius * 0.32f * zoom, obs.canopyRadius * 0.5f * zoom, detailPaint)
                }

                ObstacleType.CABIN_WALL -> {
                    // Sloped Wooden Cabin Roof with overhang
                    terrainPaint.color = Color.argb(alpha, 78, 56, 40)
                    canvas.drawRoundRect(
                        RectF(sx - 88f * zoom, sy - 52f * zoom, sx + 88f * zoom, sy + 58f * zoom),
                        10f * zoom, 10f * zoom, terrainPaint
                    )
                    // Roof ridge line
                    detailPaint.color = Color.argb(alpha, 102, 74, 52)
                    canvas.drawRect(
                        RectF(sx - 86f * zoom, sy - 5f * zoom, sx + 86f * zoom, sy + 5f * zoom),
                        detailPaint
                    )
                }

                else -> {}
            }
        }
    }

    fun drawAtmosphericVignetteAndDecoyRipples(
        canvas: Canvas,
        vw: Float,
        vh: Float,
        decoy: DecoyProjectile?,
        cameraX: Float,
        cameraY: Float,
        zoom: Float
    ) {
        // Draw Acoustic Decoy Projectile & Sound Wave Ripples
        if (decoy != null) {
            val sx = (decoy.currentX - cameraX) * zoom + vw / 2f
            val sy = (decoy.currentY - cameraY) * zoom + vh / 2f

            if (!decoy.hasLanded) {
                // Flying stone with shadow
                shadowPaint.color = Color.argb(100, 10, 10, 10)
                canvas.drawCircle(sx + 8f * zoom, sy + 14f * zoom, 4f * zoom, shadowPaint)
                detailPaint.color = Color.parseColor("#A89E92")
                canvas.drawCircle(sx, sy - 15f * sin(decoy.progress * Math.PI.toFloat()) * zoom, 5f * zoom, detailPaint)
            } else if (decoy.rippleAlpha > 0.05f) {
                // Expanding Acoustic Sound Shockwave
                outlinePaint.color = Color.argb((decoy.rippleAlpha * 220).toInt(), 229, 169, 60)
                outlinePaint.strokeWidth = 3.5f * zoom
                canvas.drawCircle(sx, sy, decoy.rippleRadius * zoom, outlinePaint)
                // Second outer faint ripple
                outlinePaint.color = Color.argb((decoy.rippleAlpha * 110).toInt(), 242, 196, 109)
                outlinePaint.strokeWidth = 2f * zoom
                canvas.drawCircle(sx, sy, (decoy.rippleRadius * 1.5f) * zoom, outlinePaint)
            }
        }

        // Soft Forest Ambient Vignette (Corners slightly darkened)
        val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(35, 10, 18, 14)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, vw, 24f, cornerPaint)
        canvas.drawRect(0f, vh - 24f, vw, vh, cornerPaint)
    }
}
