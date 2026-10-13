package com.example.game

import com.example.engine.ExpeditionAiEngine
import com.example.model.AiDifficulty
import com.example.model.BotAiState
import com.example.model.CharacterConfig
import com.example.model.InventoryGridCell
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class SurvivalGameplayState(
    val player: SurvivalPlayer = SurvivalPlayer(),
    val bots: List<BotCharacter> = emptyList(),
    val supplies: List<WorldSupplyItem> = emptyList(),
    val obstacles: List<WorldObstacle> = emptyList(),
    val extractionZone: ExtractionZone = ExtractionZone(),
    val decoyProjectile: DecoyProjectile? = null,
    val cameraX: Float = 280f,
    val cameraY: Float = 1450f,
    val zoom: Float = 1.0f,
    val cachesSecuredCount: Int = 0,
    val totalPrimaryCaches: Int = 3,
    val isMissionComplete: Boolean = false,
    val isPlayerDefeated: Boolean = false,
    val isPaused: Boolean = false,
    val difficulty: AiDifficulty = AiDifficulty.RANGER,
    val nearbySupply: WorldSupplyItem? = null,
    val canExtract: Boolean = false,
    val decoyCooldownSeconds: Float = 0f,
    val strobeCooldownSeconds: Float = 0f,
    val gameTimeSeconds: Float = 0f,
    val bannerMessageAr: String = "اعثر على 3 منارات إشارة وأمّنها، ثم اتجه لمنطقة الإخلاء الخضراء!",
    val bannerMessageEn: String = "Locate and secure 3 Signal Beacons, then proceed to the Green Extraction Pad!"
)

class SurvivalGameEngine(
    val worldData: SurvivalWorldData = SurvivalWorldData()
) {
    fun createInitialState(
        difficulty: AiDifficulty = AiDifficulty.RANGER
    ): SurvivalGameplayState {
        val supplies = worldData.createSupplies()
        val bots = worldData.createBots()
        val player = SurvivalPlayer(x = 280f, y = 1450f)

        return SurvivalGameplayState(
            player = player,
            bots = bots,
            supplies = supplies,
            obstacles = worldData.obstacles,
            extractionZone = worldData.extractionZone,
            cameraX = player.x,
            cameraY = player.y,
            difficulty = difficulty,
            bannerMessageAr = "اعثر على 3 منارات إشارة وأمّنها، ثم اتجه لمنطقة الإخلاء الخضراء!",
            bannerMessageEn = "Locate and secure 3 Signal Beacons, then proceed to the Green Extraction Pad!"
        )
    }

    /**
     * Advance real-time continuous gameplay simulation by deltaSeconds.
     */
    fun update(
        currentState: SurvivalGameplayState,
        joystickX: Float,
        joystickY: Float,
        isSprinting: Boolean,
        deltaSeconds: Float
    ): SurvivalGameplayState {
        if (currentState.isPaused || currentState.isMissionComplete || currentState.isPlayerDefeated) {
            return currentState
        }

        val dt = deltaSeconds.coerceIn(0.001f, 0.1f)
        val player = currentState.player
        val obstacles = currentState.obstacles

        // 1. Player Continuous Movement & Stamina
        val joyMag = sqrt(joystickX * joystickX + joystickY * joystickY).coerceIn(0f, 1f)
        val moving = joyMag > 0.08f
        val running = isSprinting && player.stamina > 5f && moving

        val baseSpeed = if (running) 240f else 145f
        var vx = if (moving) (joystickX / joyMag) * baseSpeed * joyMag else 0f
        var vy = if (moving) (joystickY / joyMag) * baseSpeed * joyMag else 0f

        // Check if player is currently standing inside shallow water stream (slows speed by 35%)
        val inWater = isPointInWater(player.x, player.y)
        if (inWater) {
            vx *= 0.65f
            vy *= 0.65f
        }

        // Compute candidate new position
        var newPx = player.x + vx * dt
        var newPy = player.y + vy * dt

        // Clamp to map boundaries
        newPx = newPx.coerceIn(40f, worldData.width - 40f)
        newPy = newPy.coerceIn(40f, worldData.height - 40f)

        // Circle collision resolution against solid obstacles
        val playerRadius = 20f
        obstacles.forEach { obs ->
            if (obs.type == ObstacleType.ROCK_BOULDER || obs.type == ObstacleType.TREE_PINE ||
                obs.type == ObstacleType.TREE_OAK || obs.type == ObstacleType.CABIN_WALL
            ) {
                val dx = newPx - obs.x
                val dy = newPy - obs.y
                val dist = sqrt(dx * dx + dy * dy)
                val minDist = playerRadius + obs.radius
                if (dist < minDist && dist > 0.001f) {
                    val overlap = minDist - dist
                    newPx += (dx / dist) * overlap
                    newPy += (dy / dist) * overlap
                }
            }
        }

        // Facing direction
        var facing = player.facingDegrees
        if (moving) {
            val angleRad = atan2(vy.toDouble(), vx.toDouble())
            facing = Math.toDegrees(angleRad).toFloat() + 90f // 0 deg is North (-Y)
        }

        // Walking animation cycle
        var animPhase = player.animPhase
        if (moving) {
            val animSpeed = if (running) 18f else 11f
            animPhase = (animPhase + dt * animSpeed) % (2f * PI.toFloat())
        } else {
            animPhase = 0f
        }

        // Stamina management
        var stamina = player.stamina
        stamina = if (running) {
            (stamina - dt * 25f).coerceAtLeast(0f)
        } else {
            (stamina + dt * 15f).coerceAtMost(100f)
        }

        // Stealth check: Is player in a dense bush?
        val inStealth = obstacles.any { obs ->
            obs.type == ObstacleType.BUSH_DENSE && obs.containsPoint(newPx, newPy)
        }

        val updatedPlayer = player.copy(
            x = newPx,
            y = newPy,
            vx = vx,
            vy = vy,
            facingDegrees = facing,
            animPhase = animPhase,
            isMoving = moving,
            isRunning = running,
            stamina = stamina,
            inStealthBush = inStealth
        )

        // 2. Decoy Projectile Simulation
        var decoy = currentState.decoyProjectile
        if (decoy != null) {
            if (!decoy.hasLanded) {
                val newProg = (decoy.progress + dt * 2.2f).coerceAtMost(1f)
                val curX = decoy.startX + (decoy.targetX - decoy.startX) * newProg
                val curY = decoy.startY + (decoy.targetY - decoy.startY) * newProg
                decoy = if (newProg >= 1f) {
                    decoy.copy(progress = 1f, currentX = decoy.targetX, currentY = decoy.targetY, hasLanded = true, rippleRadius = 10f, rippleAlpha = 1f)
                } else {
                    decoy.copy(progress = newProg, currentX = curX, currentY = curY)
                }
            } else {
                val newRad = decoy.rippleRadius + dt * 110f
                val newAlpha = (decoy.rippleAlpha - dt * 0.7f).coerceAtLeast(0f)
                decoy = if (newAlpha > 0.05f) {
                    decoy.copy(rippleRadius = newRad, rippleAlpha = newAlpha)
                } else {
                    null
                }
            }
        }

        // 3. AI Opponents Sensing, Decisions & Path Following
        val diff = currentState.difficulty
        val visionRadius = when (diff) {
            AiDifficulty.RECRUIT -> 310f
            AiDifficulty.RANGER -> 440f
            AiDifficulty.TACTICIAN -> 590f
        }

        var damageTakenThisTick = 0
        var newBannerAr = currentState.bannerMessageAr
        var newBannerEn = currentState.bannerMessageEn

        val updatedBots = currentState.bots.mapIndexed { index, bot ->
            var bx = bot.x
            var by = bot.y
            var state = bot.state
            var stun = (bot.stunSeconds - dt).coerceAtLeast(0f)
            var lkpX = bot.lastKnownPlayerX
            var lkpY = bot.lastKnownPlayerY
            var searchTimer = (bot.searchTimerSeconds - dt).coerceAtLeast(0f)
            var wpIdx = bot.currentWaypointIndex

            if (stun > 0f) {
                return@mapIndexed bot.copy(stunSeconds = stun, state = BotAiState.RETREAT_COVER)
            }

            // Raycast Line-of-Sight from bot to player
            val distToPlayer = sqrt((newPx - bx) * (newPx - bx) + (newPy - by) * (newPy - by))
            val effectiveRange = if (inStealth) visionRadius * 0.45f else visionRadius
            val canSeePlayer = distToPlayer <= effectiveRange && hasLineOfSight(obstacles, bx, by, newPx, newPy)

            var targetX = bx
            var targetY = by
            val botSpeed = when (state) {
                BotAiState.PURSUE -> 140f
                BotAiState.FLANK_COORDINATE -> 135f
                BotAiState.SEARCH_LAST_KNOWN -> 100f
                else -> 75f
            }

            when {
                canSeePlayer -> {
                    lkpX = newPx
                    lkpY = newPy
                    searchTimer = when (diff) {
                        AiDifficulty.RECRUIT -> 5f
                        AiDifficulty.RANGER -> 9f
                        AiDifficulty.TACTICIAN -> 15f
                    }

                    if (diff == AiDifficulty.TACTICIAN && index == 1) {
                        // Flanker Bot takes an offset flanking angle!
                        state = BotAiState.FLANK_COORDINATE
                        targetX = newPx + (if (index % 2 == 0) 120f else -120f)
                        targetY = newPy + (if (index % 2 == 0) -80f else 80f)
                        newBannerAr = "${bot.nameAr}: يرصدك وينسق الالتفاف حولك!"
                        newBannerEn = "${bot.nameEn}: Flanking your position!"
                    } else {
                        state = BotAiState.PURSUE
                        targetX = newPx
                        targetY = newPy
                        newBannerAr = "${bot.nameAr}: رصد موقعك ويقترب مباشرة!"
                        newBannerEn = "${bot.nameEn}: Target spotted, pursuing!"
                    }
                }

                // If decoy ripple is active within hearing range (700f)
                decoy != null && decoy.hasLanded && sqrt((decoy.currentX - bx) * (decoy.currentX - bx) + (decoy.currentY - by) * (decoy.currentY - by)) < 700f -> {
                    state = BotAiState.SEARCH_LAST_KNOWN
                    lkpX = decoy.currentX
                    lkpY = decoy.currentY
                    searchTimer = 7f
                    targetX = decoy.currentX
                    targetY = decoy.currentY
                    newBannerAr = "${bot.nameAr}: سمع صوت التمويه ويتحرك لفحصه!"
                    newBannerEn = "${bot.nameEn}: Investigating acoustic decoy!"
                }

                // Searching Last Known Position
                lkpX != null && lkpY != null && searchTimer > 0f -> {
                    state = BotAiState.SEARCH_LAST_KNOWN
                    targetX = lkpX
                    targetY = lkpY
                    val distToLkp = sqrt((targetX - bx) * (targetX - bx) + (targetY - by) * (targetY - by))
                    if (distToLkp < 35f) {
                        // Reached LKP, search in slight spiral
                        lkpX += cos(currentState.gameTimeSeconds * 2f + index) * 60f
                        lkpY += sin(currentState.gameTimeSeconds * 2f + index) * 60f
                    }
                }

                // Normal Waypoint Patrol
                else -> {
                    state = BotAiState.PATROL
                    lkpX = null
                    lkpY = null
                    if (bot.waypoints.isNotEmpty()) {
                        val (wx, wy) = bot.waypoints[wpIdx % bot.waypoints.size]
                        val distToWp = sqrt((wx - bx) * (wx - bx) + (wy - by) * (wy - by))
                        if (distToWp < 40f) {
                            wpIdx = (wpIdx + 1) % bot.waypoints.size
                        }
                        targetX = bot.waypoints[wpIdx % bot.waypoints.size].first
                        targetY = bot.waypoints[wpIdx % bot.waypoints.size].second
                    }
                }
            }

            // Move bot toward targetX, targetY with obstacle avoidance
            val tdx = targetX - bx
            val tdy = targetY - by
            val tdist = sqrt(tdx * tdx + tdy * tdy)
            var bvx = 0f
            var bvy = 0f
            var botMoving = false
            var bFacing = bot.facingDegrees

            if (tdist > 15f) {
                botMoving = true
                bvx = (tdx / tdist) * botSpeed
                bvy = (tdy / tdist) * botSpeed
                val angleRad = atan2(bvy.toDouble(), bvx.toDouble())
                bFacing = Math.toDegrees(angleRad).toFloat() + 90f
            }

            var nextBx = bx + bvx * dt
            var nextBy = by + bvy * dt

            // Bot obstacle avoidance & repulsion
            obstacles.forEach { obs ->
                if (obs.type == ObstacleType.ROCK_BOULDER || obs.type == ObstacleType.TREE_PINE ||
                    obs.type == ObstacleType.TREE_OAK || obs.type == ObstacleType.CABIN_WALL
                ) {
                    val ox = nextBx - obs.x
                    val oy = nextBy - obs.y
                    val odist = sqrt(ox * ox + oy * oy)
                    val minDist = 22f + obs.radius
                    if (odist < minDist && odist > 0.001f) {
                        val push = minDist - odist
                        nextBx += (ox / odist) * push
                        nextBy += (oy / odist) * push
                    }
                }
            }

            // Check proximity to player for damage
            val distToP = sqrt((newPx - nextBx) * (newPx - nextBx) + (newPy - nextBy) * (newPy - nextBy))
            if (distToP < 34f) {
                damageTakenThisTick += (dt * 30f).toInt().coerceAtLeast(1)
            }

            val bAnim = if (botMoving) (bot.animPhase + dt * 10f) % (2f * PI.toFloat()) else 0f

            bot.copy(
                x = nextBx,
                y = nextBy,
                vx = bvx,
                vy = bvy,
                facingDegrees = bFacing,
                animPhase = bAnim,
                isMoving = botMoving,
                state = state,
                stunSeconds = stun,
                lastKnownPlayerX = lkpX,
                lastKnownPlayerY = lkpY,
                searchTimerSeconds = searchTimer,
                currentWaypointIndex = wpIdx
            )
        }

        // Apply damage to player
        val newHealth = (player.health - damageTakenThisTick).coerceIn(0, 100)
        val isDefeated = newHealth <= 0
        if (isDefeated) {
            newBannerAr = "تمت محاصرتك في الغابة! اضغط على إعادة المحاولة."
            newBannerEn = "You were captured by scavengers! Tap Restart to try again."
        }

        // 4. Check Nearby Collectible Supply
        var nearestSupply: WorldSupplyItem? = null
        var nearestDist = 58f
        currentState.supplies.filter { !it.collected }.forEach { sup ->
            val dist = sqrt((newPx - sup.x) * (newPx - sup.x) + (newPy - sup.y) * (newPy - sup.y))
            if (dist < nearestDist) {
                nearestDist = dist
                nearestSupply = sup
            }
        }

        // 5. Check Extraction Zone Proximity
        val distToExtract = sqrt((newPx - currentState.extractionZone.x) * (newPx - currentState.extractionZone.x) +
                (newPy - currentState.extractionZone.y) * (newPy - currentState.extractionZone.y))
        val canExtract = distToExtract <= currentState.extractionZone.radius

        // 6. Smooth Camera Tracking (Lerp toward player with boundary clamping)
        val viewportW = 900f
        val viewportH = 1600f
        val targetCamX = newPx.coerceIn(viewportW / 2f, worldData.width - viewportW / 2f)
        val targetCamY = newPy.coerceIn(viewportH / 2f, worldData.height - viewportH / 2f)
        val camX = currentState.cameraX + (targetCamX - currentState.cameraX) * (dt * 6.5f).coerceAtMost(1f)
        val camY = currentState.cameraY + (targetCamY - currentState.cameraY) * (dt * 6.5f).coerceAtMost(1f)

        return currentState.copy(
            player = updatedPlayer.copy(health = newHealth),
            bots = updatedBots,
            decoyProjectile = decoy,
            cameraX = camX,
            cameraY = camY,
            isPlayerDefeated = isDefeated,
            nearbySupply = nearestSupply,
            canExtract = canExtract,
            decoyCooldownSeconds = (currentState.decoyCooldownSeconds - dt).coerceAtLeast(0f),
            strobeCooldownSeconds = (currentState.strobeCooldownSeconds - dt).coerceAtLeast(0f),
            gameTimeSeconds = currentState.gameTimeSeconds + dt,
            bannerMessageAr = newBannerAr,
            bannerMessageEn = newBannerEn
        )
    }

    /**
     * Collect nearby supply item into inventory.
     */
    fun interactCollectSupply(
        state: SurvivalGameplayState,
        grid: List<InventoryGridCell>
    ): Pair<SurvivalGameplayState, List<InventoryGridCell>> {
        val target = state.nearbySupply ?: return Pair(state, grid)
        val updatedSupplies = state.supplies.map { if (it.id == target.id) it.copy(collected = true) else it }
        val securedCount = updatedSupplies.count { it.isPrimaryObjective && it.collected }
        val updatedGrid = ExpeditionAiEngine.addItemToGrid(grid, target.itemId, 1)

        val bannerAr = if (target.isPrimaryObjective) {
            "تم تأمين ${target.nameAr}! ($securedCount/3)"
        } else {
            "تم جمع ${target.nameAr} وإضافتها للحقيبة!"
        }
        val bannerEn = if (target.isPrimaryObjective) {
            "Secured ${target.nameEn}! ($securedCount/3)"
        } else {
            "Collected ${target.nameEn} into backpack!"
        }

        return Pair(
            state.copy(
                supplies = updatedSupplies,
                cachesSecuredCount = securedCount,
                nearbySupply = null,
                bannerMessageAr = bannerAr,
                bannerMessageEn = bannerEn
            ),
            updatedGrid
        )
    }

    /**
     * Trigger extraction if all primary beacons are collected.
     */
    fun triggerExtraction(state: SurvivalGameplayState): SurvivalGameplayState {
        if (state.cachesSecuredCount >= state.totalPrimaryCaches && state.canExtract) {
            return state.copy(
                isMissionComplete = true,
                bannerMessageAr = "تم الإخلاء بنجاح! اكتملت المهمة وتم تأمين جميع الصناديق.",
                bannerMessageEn = "Mission Accomplished! You safely extracted with all signal caches."
            )
        }
        return state
    }

    /**
     * Throw acoustic decoy stone forward.
     */
    fun throwDecoy(state: SurvivalGameplayState): SurvivalGameplayState {
        if (state.decoyCooldownSeconds > 0f || state.isMissionComplete || state.isPlayerDefeated) return state
        val p = state.player
        val angleRad = Math.toRadians((p.facingDegrees - 90f).toDouble())
        val throwDist = 260f
        val targetX = (p.x + cos(angleRad).toFloat() * throwDist).coerceIn(60f, worldData.width - 60f)
        val targetY = (p.y + sin(angleRad).toFloat() * throwDist).coerceIn(60f, worldData.height - 60f)

        val proj = DecoyProjectile(
            startX = p.x,
            startY = p.y,
            targetX = targetX,
            targetY = targetY
        )
        return state.copy(
            decoyProjectile = proj,
            decoyCooldownSeconds = 5.5f,
            bannerMessageAr = "رميت حجر تمويه صوتي لتشتيت حراس الغابة!",
            bannerMessageEn = "Threw acoustic decoy stone to draw scavengers away!"
        )
    }

    /**
     * Trigger tactical strobe flare forward.
     */
    fun triggerStrobe(state: SurvivalGameplayState): SurvivalGameplayState {
        if (state.strobeCooldownSeconds > 0f || state.isMissionComplete || state.isPlayerDefeated) return state
        val p = state.player
        var stunnedCount = 0

        val updatedBots = state.bots.map { bot ->
            val dx = bot.x - p.x
            val dy = bot.y - p.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < 380f) {
                stunnedCount++
                bot.copy(stunSeconds = 3.5f, state = BotAiState.RETREAT_COVER)
            } else {
                bot
            }
        }

        val msgAr = if (stunnedCount > 0) {
            "أطلقت وميض المصباح الباهر! أصيب $stunnedCount روبوت بالعمى المؤقت وتراجعوا."
        } else {
            "أطلقت وميض المصباح الباهر (لا توجد روبوتات قريبة)."
        }
        val msgEn = if (stunnedCount > 0) {
            "Triggered Strobe! Blinded $stunnedCount scavenger(s) into retreat."
        } else {
            "Triggered Strobe (no targets in flash cone)."
        }

        return state.copy(
            bots = updatedBots,
            strobeCooldownSeconds = 6.0f,
            bannerMessageAr = msgAr,
            bannerMessageEn = msgEn
        )
    }

    private fun isPointInWater(x: Float, y: Float): Boolean {
        // Stream crosses through the central corridor (around X=1300..1650)
        return (x in 1360f..1480f && y in 600f..1200f)
    }

    private fun hasLineOfSight(
        obstacles: List<WorldObstacle>,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ): Boolean {
        // Check line-segment intersection with solid obstacle circles (rocks, trees, cabin walls)
        obstacles.forEach { obs ->
            if (obs.type == ObstacleType.ROCK_BOULDER || obs.type == ObstacleType.CABIN_WALL) {
                if (circleIntersectsSegment(obs.x, obs.y, obs.radius * 0.9f, x1, y1, x2, y2)) {
                    return false
                }
            }
        }
        return true
    }

    private fun circleIntersectsSegment(
        cx: Float,
        cy: Float,
        r: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) {
            val d = (cx - x1) * (cx - x1) + (cy - y1) * (cy - y1)
            return d <= r * r
        }
        val t = (((cx - x1) * dx + (cy - y1) * dy) / lenSq).coerceIn(0f, 1f)
        val nearestX = x1 + t * dx
        val nearestY = y1 + t * dy
        val distSq = (cx - nearestX) * (cx - nearestX) + (cy - nearestY) * (cy - nearestY)
        return distSq <= r * r
    }
}
