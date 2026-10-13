package com.example.engine

import com.example.model.AiDifficulty
import com.example.model.BotAgent
import com.example.model.BotAiState
import com.example.model.GridPoint
import com.example.model.InventoryGridCell
import com.example.model.ResourceNode
import com.example.model.TileType
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.max

data class ExpeditionWorldState(
    val mapWidth: Int = 16,
    val mapHeight: Int = 12,
    val tiles: List<List<TileType>> = emptyList(),
    val playerPos: GridPoint = GridPoint(1, 10),
    val lastPlayerVelocity: GridPoint = GridPoint(0, 0),
    val playerHealth: Int = 100,
    val playerStamina: Int = 100,
    val isPlayerStealthInBrush: Boolean = false,
    val activeDecoyPos: GridPoint? = null,
    val decoyTicksRemaining: Int = 0,
    val flareCooldownTicks: Int = 0,
    val decoyCooldownTicks: Int = 0,
    val bots: List<BotAgent> = emptyList(),
    val resources: List<ResourceNode> = emptyList(),
    val inventoryGrid: List<InventoryGridCell> = emptyList(),
    val selectedGridSlotIndex: Int? = null,
    val difficulty: AiDifficulty = AiDifficulty.RANGER,
    val tickCount: Int = 0,
    val cachesCollected: Int = 0,
    val totalPrimaryCaches: Int = 3,
    val isMissionComplete: Boolean = false,
    val isPlayerDefeated: Boolean = false,
    val aiStatusLogAr: String = "استكشف المنطقة، اجمع 3 صناديق إمداد، ثم اتجه لنقطة الإخلاء الخضراء!",
    val aiStatusLogEn: String = "Explore the sector, secure 3 Supply Caches, then reach the Green Extraction Zone!"
)

/**
 * Pure, deterministic, offline single-player Survival & Tactical AI Engine.
 * Features:
 * 1. Bresenham Line-of-Sight (LOS) with Rock Wall occlusion and Bush Cover stealth reduction.
 * 2. Allocation-light A* Pathfinding with terrain & obstacle avoidance + flanking penalty around allied lines.
 * 3. Multi-state Autonomous Bot AI (PATROL, HARVEST_OBJECTIVE, SEARCH_LAST_KNOWN, PURSUE, FLANK_COORDINATE, RETREAT_COVER).
 * 4. Difficulty scaling driven by decision quality, reaction latency, LKP search persistence, and squad coordination (NOT HP inflation).
 */
object ExpeditionAiEngine {

    const val MAP_W = 16
    const val MAP_H = 12
    const val GRID_SLOTS_COUNT = 20 // 5x4 Innawoods-style grid backpack

    fun createInitialWorld(
        difficulty: AiDifficulty = AiDifficulty.RANGER,
        existingGrid: List<InventoryGridCell>? = null
    ): ExpeditionWorldState {
        val tiles = buildDefaultMap()
        val starterGrid = existingGrid?.takeIf { it.size == GRID_SLOTS_COUNT } ?: createStarterInventoryGrid()

        val resources = listOf(
            ResourceNode("res-cache-1", GridPoint(4, 2), "nav-brass-compass", isPrimaryObjective = true),
            ResourceNode("res-cache-2", GridPoint(13, 3), "aid-field-pouch-red", isPrimaryObjective = true),
            ResourceNode("res-cache-3", GridPoint(12, 9), "food-freeze-dried-meal", isPrimaryObjective = true),
            ResourceNode("res-supply-4", GridPoint(7, 6), "water-steel-flask-1l", isPrimaryObjective = false),
            ResourceNode("res-supply-5", GridPoint(2, 5), "food-oat-energy-bars", isPrimaryObjective = false),
            ResourceNode("res-supply-6", GridPoint(9, 10), "light-aluminum-torch", isPrimaryObjective = false)
        )

        val bots = listOf(
            BotAgent(
                id = "bot-alpha",
                nameAr = "كشاف ألفا (Scout)",
                nameEn = "Scout Alpha",
                pos = GridPoint(6, 2),
                facing = GridPoint(0, 1),
                patrolWaypoints = listOf(GridPoint(4, 2), GridPoint(8, 2), GridPoint(8, 5), GridPoint(4, 5))
            ),
            BotAgent(
                id = "bot-bravo",
                nameAr = "متعقب برافو (Tracker)",
                nameEn = "Tracker Bravo",
                pos = GridPoint(13, 5),
                facing = GridPoint(-1, 0),
                patrolWaypoints = listOf(GridPoint(13, 2), GridPoint(10, 4), GridPoint(13, 8), GridPoint(11, 6))
            ),
            BotAgent(
                id = "bot-charlie",
                nameAr = "مطوّق تشارلي (Flanker)",
                nameEn = "Flanker Charlie",
                pos = GridPoint(10, 9),
                facing = GridPoint(-1, -1),
                patrolWaypoints = listOf(GridPoint(12, 9), GridPoint(7, 9), GridPoint(7, 6), GridPoint(11, 7))
            )
        )

        return ExpeditionWorldState(
            mapWidth = MAP_W,
            mapHeight = MAP_H,
            tiles = tiles,
            playerPos = GridPoint(1, 10),
            playerHealth = 100,
            playerStamina = 100,
            isPlayerStealthInBrush = tiles[10][1] == TileType.BUSH_COVER,
            bots = bots,
            resources = resources,
            inventoryGrid = starterGrid,
            difficulty = difficulty
        )
    }

    fun createStarterInventoryGrid(): List<InventoryGridCell> {
        val cells = MutableList(GRID_SLOTS_COUNT) { idx -> InventoryGridCell(index = idx) }
        cells[0] = InventoryGridCell(0, "water-steel-flask-1l", 1)
        cells[1] = InventoryGridCell(1, "food-trail-mix-pouch", 2)
        cells[2] = InventoryGridCell(2, "aid-field-pouch-red", 1)
        cells[3] = InventoryGridCell(3, "light-camp-lantern", 1)
        cells[4] = InventoryGridCell(4, "nav-topo-map-case", 1)
        cells[5] = InventoryGridCell(5, "acc-trekking-poles", 1)
        return cells
    }

    private fun buildDefaultMap(): List<List<TileType>> {
        val grid = MutableList(MAP_H) { MutableList(MAP_W) { TileType.GROUND } }

        // Extraction zone at top-right corner (14,1) & (15,1)
        grid[1][14] = TileType.EXTRACTION_ZONE
        grid[1][15] = TileType.EXTRACTION_ZONE

        // Rock Wall obstacles (block movement AND line of sight)
        val rocks = listOf(
            GridPoint(3, 3), GridPoint(3, 4), GridPoint(3, 7), GridPoint(3, 8),
            GridPoint(6, 4), GridPoint(7, 4), GridPoint(6, 8),
            GridPoint(9, 2), GridPoint(9, 3), GridPoint(10, 7), GridPoint(11, 7),
            GridPoint(12, 4), GridPoint(5, 10), GridPoint(13, 10)
        )
        rocks.forEach { p ->
            if (p.y in 0 until MAP_H && p.x in 0 until MAP_W) {
                grid[p.y][p.x] = TileType.WALL_ROCK
            }
        }

        // Bush Cover tiles (grant stealth concealment from distant bots)
        val bushes = listOf(
            GridPoint(2, 9), GridPoint(2, 8), GridPoint(4, 6), GridPoint(5, 6),
            GridPoint(5, 2), GridPoint(8, 3), GridPoint(8, 7), GridPoint(9, 8),
            GridPoint(11, 3), GridPoint(12, 8), GridPoint(14, 6)
        )
        bushes.forEach { p ->
            if (p.y in 0 until MAP_H && p.x in 0 until MAP_W) {
                grid[p.y][p.x] = TileType.BUSH_COVER
            }
        }

        // Shallow Water stream tiles
        val water = listOf(
            GridPoint(7, 0), GridPoint(7, 1), GridPoint(6, 6), GridPoint(6, 7)
        )
        water.forEach { p ->
            if (p.y in 0 until MAP_H && p.x in 0 until MAP_W) {
                grid[p.y][p.x] = TileType.WATER_STREAM
            }
        }

        return grid
    }

    fun isWalkable(tiles: List<List<TileType>>, p: GridPoint): Boolean {
        if (p.x !in 0 until MAP_W || p.y !in 0 until MAP_H) return false
        return tiles[p.y][p.x] != TileType.WALL_ROCK
    }

    /**
     * Bresenham Line-of-Sight check between two grid coordinates.
     * Rock walls block vision completely.
     * If the player is standing in BUSH_COVER, effective vision range is cut by 45% (unless bot is adjacent <= 1.8f).
     */
    fun hasLineOfSight(
        tiles: List<List<TileType>>,
        from: GridPoint,
        to: GridPoint,
        maxRange: Float,
        targetInBrush: Boolean
    ): Boolean {
        val dist = from.distanceTo(to)
        val effectiveRange = if (targetInBrush) max(1.8f, maxRange * 0.52f) else maxRange
        if (dist > effectiveRange) return false

        var x0 = from.x
        var y0 = from.y
        val x1 = to.x
        val y1 = to.y
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy

        while (true) {
            if (x0 == x1 && y0 == y1) return true
            if (!(x0 == from.x && y0 == from.y)) {
                if (tiles[y0][x0] == TileType.WALL_ROCK) return false
            }
            val e2 = 2 * err
            if (e2 >= dy) {
                if (x0 == x1) break
                err += dy
                x0 += sx
            }
            if (e2 <= dx) {
                if (y0 == y1) break
                err += dx
                y0 += sy
            }
        }
        return true
    }

    /**
     * A* Pathfinding on the 16x12 grid.
     * Supports `avoidPoints` (e.g., allied bot positions or direct line-of-fire) so flanking bots
     * automatically pick an alternate route around obstacles to pincer the target!
     */
    fun findPathAStar(
        tiles: List<List<TileType>>,
        start: GridPoint,
        goal: GridPoint,
        avoidPoints: Set<GridPoint> = emptySet(),
        flankPenaltyCenter: GridPoint? = null
    ): List<GridPoint> {
        if (start == goal) return emptyList()
        val effectiveGoal = if (isWalkable(tiles, goal)) {
            goal
        } else {
            neighbors(tiles, goal).firstOrNull() ?: return emptyList()
        }

        data class Node(val pt: GridPoint, val g: Int, val f: Int)

        val open = PriorityQueue<Node>(compareBy { it.f })
        val gScore = HashMap<GridPoint, Int>()
        val cameFrom = HashMap<GridPoint, GridPoint>()

        gScore[start] = 0
        open.add(Node(start, 0, start.manhattanTo(effectiveGoal) * 10))

        var iterations = 0
        while (open.isNotEmpty() && iterations < 240) {
            iterations++
            val current = open.poll().pt
            if (current == effectiveGoal) {
                val path = ArrayList<GridPoint>()
                var curr: GridPoint? = current
                while (curr != null && curr != start) {
                    path.add(curr)
                    curr = cameFrom[curr]
                }
                path.reverse()
                return path
            }

            val currentG = gScore[current] ?: Int.MAX_VALUE
            for (next in neighbors(tiles, current)) {
                val tileType = tiles[next.y][next.x]
                var stepCost = when (tileType) {
                    TileType.WATER_STREAM -> 18
                    TileType.BUSH_COVER -> 12
                    else -> 10
                }
                if (next in avoidPoints && next != effectiveGoal) {
                    stepCost += 35
                }
                if (flankPenaltyCenter != null && next.manhattanTo(flankPenaltyCenter) <= 2) {
                    stepCost += 25 // Encourage flanking around the primary pursuer!
                }

                val tentativeG = currentG + stepCost
                if (tentativeG < (gScore[next] ?: Int.MAX_VALUE)) {
                    cameFrom[next] = current
                    gScore[next] = tentativeG
                    val f = tentativeG + next.manhattanTo(effectiveGoal) * 10
                    open.add(Node(next, tentativeG, f))
                }
            }
        }
        return emptyList()
    }

    private fun neighbors(tiles: List<List<TileType>>, p: GridPoint): List<GridPoint> {
        val dirs = listOf(
            GridPoint(p.x + 1, p.y),
            GridPoint(p.x - 1, p.y),
            GridPoint(p.x, p.y + 1),
            GridPoint(p.x, p.y - 1)
        )
        return dirs.filter { isWalkable(tiles, it) }
    }

    /**
     * Moves the player by (dx, dy) or processes a wait/tactical step, then advances all AI bots by 1 tick.
     */
    fun stepPlayerMove(state: ExpeditionWorldState, dx: Int, dy: Int): ExpeditionWorldState {
        if (state.isMissionComplete || state.isPlayerDefeated) return state

        val candidate = GridPoint(
            (state.playerPos.x + dx).coerceIn(0, state.mapWidth - 1),
            (state.playerPos.y + dy).coerceIn(0, state.mapHeight - 1)
        )
        val actualPos = if (isWalkable(state.tiles, candidate)) candidate else state.playerPos
        val velocity = GridPoint(actualPos.x - state.playerPos.x, actualPos.y - state.playerPos.y)
        val inBrush = state.tiles[actualPos.y][actualPos.x] == TileType.BUSH_COVER
        val staminaCost = if (dx == 0 && dy == 0) -6 else if (state.tiles[actualPos.y][actualPos.x] == TileType.WATER_STREAM) 5 else 2
        val updatedStamina = (state.playerStamina - staminaCost).coerceIn(0, 100)

        // Check resource collection at actualPos
        var collectedItem: String? = null
        var updatedGrid = state.inventoryGrid
        val updatedResources = state.resources.map { node ->
            if (!node.collected && node.pos == actualPos) {
                collectedItem = node.itemId
                updatedGrid = addItemToGrid(updatedGrid, node.itemId, 1)
                node.copy(collected = true)
            } else {
                node
            }
        }

        val cachesDone = updatedResources.count { it.isPrimaryObjective && it.collected }
        val onExtraction = state.tiles[actualPos.y][actualPos.x] == TileType.EXTRACTION_ZONE
        if (cachesDone >= state.totalPrimaryCaches && onExtraction) {
            return state.copy(
                playerPos = actualPos,
                lastPlayerVelocity = velocity,
                playerStamina = updatedStamina,
                isPlayerStealthInBrush = inBrush,
                resources = updatedResources,
                inventoryGrid = updatedGrid,
                cachesCollected = cachesDone,
                isMissionComplete = true,
                tickCount = state.tickCount + 1,
                aiStatusLogAr = "اكتملت المهمة بنجاح! وصلت إلى نقطة الإخلاء بكامل الصناديق.",
                aiStatusLogEn = "Expedition Complete! You secured all Supply Caches and reached Extraction."
            )
        }

        val afterPlayer = state.copy(
            playerPos = actualPos,
            lastPlayerVelocity = velocity,
            playerStamina = updatedStamina,
            isPlayerStealthInBrush = inBrush,
            resources = updatedResources,
            inventoryGrid = updatedGrid,
            cachesCollected = cachesDone,
            tickCount = state.tickCount + 1,
            decoyTicksRemaining = (state.decoyTicksRemaining - 1).coerceAtLeast(0),
            activeDecoyPos = if (state.decoyTicksRemaining > 1) state.activeDecoyPos else null,
            flareCooldownTicks = (state.flareCooldownTicks - 1).coerceAtLeast(0),
            decoyCooldownTicks = (state.decoyCooldownTicks - 1).coerceAtLeast(0)
        )

        return stepBotsAi(afterPlayer, collectedItem)
    }

    /**
     * Advances all AI bots by 1 decision + movement tick.
     */
    fun stepBotsAi(state: ExpeditionWorldState, newlyCollectedItemId: String? = null): ExpeditionWorldState {
        val diff = state.difficulty
        val tiles = state.tiles
        val playerPos = state.playerPos

        // 1. Determine which bots have direct Line-of-Sight to the player
        val directSpotters = state.bots.filter { bot ->
            bot.stunTicks == 0 && hasLineOfSight(
                tiles = tiles,
                from = bot.pos,
                to = playerPos,
                maxRange = diff.visionRange,
                targetInBrush = state.isPlayerStealthInBrush
            )
        }

        val squadAlertedPlayerPos: GridPoint? = if (directSpotters.isNotEmpty() && diff.enablesSquadFlanking) {
            playerPos
        } else {
            null
        }

        val primaryPursuerId = directSpotters.minByOrNull { it.pos.distanceTo(playerPos) }?.id
        val occupiedByBots = state.bots.map { it.pos }.toMutableSet()
        var playerDamageTaken = 0
        var latestAiLogAr = if (newlyCollectedItemId != null) {
            "تم جمع مورد ميداني وإضافته إلى خانتك في الحقيبة!"
        } else {
            state.aiStatusLogAr
        }
        var latestAiLogEn = if (newlyCollectedItemId != null) {
            "Collected field supply into your backpack grid!"
        } else {
            state.aiStatusLogEn
        }

        val updatedBots = state.bots.mapIndexed { index, bot ->
            // Handle stun / flashbang recovery
            if (bot.stunTicks > 0) {
                // Adaptive retreat to cover when stunned/disoriented!
                val coverSpot = findNearestCoverTile(tiles, bot.pos, playerPos)
                val stepPath = findPathAStar(tiles, bot.pos, coverSpot, occupiedByBots - bot.pos)
                val nextPos = stepPath.firstOrNull() ?: bot.pos
                occupiedByBots.remove(bot.pos)
                occupiedByBots.add(nextPos)
                return@mapIndexed bot.copy(
                    pos = nextPos,
                    state = BotAiState.RETREAT_COVER,
                    stunTicks = bot.stunTicks - 1,
                    targetPos = coverSpot,
                    currentPath = stepPath
                )
            }

            val seesPlayer = directSpotters.any { it.id == bot.id }

            // Decide next BotAiState and target coordinate
            var nextState = bot.state
            var nextLastKnown = bot.lastKnownPlayerPos
            var nextVelocity = bot.lastKnownPlayerVelocity
            var nextSearchTimer = bot.searchTimerTicks
            var nextReaction = bot.reactionCounter
            var nextWaypointIdx = bot.currentWaypointIndex
            var goalPos = bot.targetPos
            var flankCenter: GridPoint? = null

            when {
                // Case A: Bot directly sees the player
                seesPlayer -> {
                    nextLastKnown = playerPos
                    nextVelocity = state.lastPlayerVelocity
                    nextSearchTimer = diff.searchDurationTicks

                    if (bot.state != BotAiState.PURSUE && bot.state != BotAiState.FLANK_COORDINATE && nextReaction < diff.reactionDelayTicks) {
                        // Difficulty-based reaction latency before locking on
                        nextReaction++
                        nextState = BotAiState.SEARCH_LAST_KNOWN
                        goalPos = playerPos
                    } else {
                        nextReaction = 0
                        if (diff.enablesSquadFlanking && primaryPursuerId != null && primaryPursuerId != bot.id) {
                            // Cooperating bot flanks the player from an offset side rather than walking single-file!
                            nextState = BotAiState.FLANK_COORDINATE
                            goalPos = computeFlankTarget(tiles, playerPos, state.lastPlayerVelocity, index)
                            flankCenter = state.bots.find { it.id == primaryPursuerId }?.pos
                            latestAiLogAr = "${bot.nameAr} ينسّق مع الفريق للالتفاف حولك!"
                            latestAiLogEn = "${bot.nameEn} is coordinating a pincer flank!"
                        } else {
                            nextState = BotAiState.PURSUE
                            goalPos = playerPos
                            latestAiLogAr = "${bot.nameAr} رصد موقعك ويطاردك مباشرة!"
                            latestAiLogEn = "${bot.nameEn} spotted you and is pursuing!"
                        }
                    }
                }

                // Case B: Acoustic Decoy active within hearing range (8 tiles)
                state.activeDecoyPos != null && bot.pos.distanceTo(state.activeDecoyPos) <= 8.5f -> {
                    nextState = BotAiState.SEARCH_LAST_KNOWN
                    nextLastKnown = state.activeDecoyPos
                    nextSearchTimer = max(4, diff.searchDurationTicks / 2)
                    goalPos = state.activeDecoyPos
                    latestAiLogAr = "${bot.nameAr} سمع صوت التمويه ويتحرك للتحقق منه!"
                    latestAiLogEn = "${bot.nameEn} heard your acoustic decoy and is investigating!"
                }

                // Case C: Allied bot spotted the player over radio (within 8 tiles)
                squadAlertedPlayerPos != null && bot.pos.distanceTo(squadAlertedPlayerPos) <= 8.5f -> {
                    nextState = BotAiState.FLANK_COORDINATE
                    nextLastKnown = squadAlertedPlayerPos
                    nextSearchTimer = diff.searchDurationTicks
                    goalPos = computeFlankTarget(tiles, squadAlertedPlayerPos, state.lastPlayerVelocity, index)
                    flankCenter = directSpotters.firstOrNull()?.pos
                }

                // Case D: Lost Line-of-Sight -> Search Last Known Position (LKP) & predicted path
                nextLastKnown != null && nextSearchTimer > 0 -> {
                    nextState = BotAiState.SEARCH_LAST_KNOWN
                    nextSearchTimer--
                    if (bot.pos == nextLastKnown || bot.pos.manhattanTo(nextLastKnown) <= 1) {
                        // Reached LKP! On TACTICIAN difficulty, project along player's last velocity vector;
                        // otherwise spiral-search adjacent walkable tiles around LKP.
                        val predicted = if (diff.enablesPredictiveSearch && (nextVelocity.x != 0 || nextVelocity.y != 0)) {
                            GridPoint(
                                (nextLastKnown.x + nextVelocity.x * 2).coerceIn(0, MAP_W - 1),
                                (nextLastKnown.y + nextVelocity.y * 2).coerceIn(0, MAP_H - 1)
                            )
                        } else {
                            null
                        }
                        val spiralCandidates = neighbors(tiles, nextLastKnown)
                        goalPos = if (predicted != null && isWalkable(tiles, predicted)) {
                            predicted
                        } else if (spiralCandidates.isNotEmpty()) {
                            spiralCandidates[(state.tickCount + index) % spiralCandidates.size]
                        } else {
                            nextLastKnown
                        }
                        nextLastKnown = goalPos
                    } else {
                        goalPos = nextLastKnown
                    }
                    latestAiLogAr = "${bot.nameAr} فقد الرؤية ويبحث في آخر موقع معروف لك..."
                    latestAiLogEn = "${bot.nameEn} lost line-of-sight and is searching your last known position..."
                }

                // Case E: Contest uncollected objective cache on RANGER/TACTICIAN or Patrol waypoints
                else -> {
                    nextLastKnown = null
                    nextSearchTimer = 0
                    nextReaction = 0
                    val uncollectedCaches = state.resources.filter { it.isPrimaryObjective && !it.collected }
                    if (diff == AiDifficulty.TACTICIAN && uncollectedCaches.isNotEmpty() && index == 1) {
                        nextState = BotAiState.HARVEST_OBJECTIVE
                        val nearestCache = uncollectedCaches.minByOrNull { it.pos.distanceTo(bot.pos) }!!
                        goalPos = nearestCache.pos
                    } else {
                        nextState = BotAiState.PATROL
                        val waypoints = bot.patrolWaypoints
                        if (waypoints.isNotEmpty()) {
                            val currentWp = waypoints[nextWaypointIdx % waypoints.size]
                            if (bot.pos == currentWp) {
                                nextWaypointIdx = (nextWaypointIdx + 1) % waypoints.size
                            }
                            goalPos = waypoints[nextWaypointIdx % waypoints.size]
                        }
                    }
                }
            }

            // Compute A* path to goalPos avoiding obstacles and other bots
            val path = findPathAStar(
                tiles = tiles,
                start = bot.pos,
                goal = goalPos,
                avoidPoints = occupiedByBots - bot.pos,
                flankPenaltyCenter = flankCenter
            )
            val nextStep = path.firstOrNull() ?: bot.pos
            val finalPos = if (nextStep == playerPos) {
                // Bot tags/intercepts player from adjacent tile without standing on top of player
                playerDamageTaken += 18
                bot.pos
            } else {
                nextStep
            }

            if (finalPos.manhattanTo(playerPos) <= 1 && seesPlayer && nextStep != playerPos) {
                playerDamageTaken += 12
            }

            val newFacing = if (finalPos != bot.pos) {
                GridPoint(finalPos.x - bot.pos.x, finalPos.y - bot.pos.y)
            } else {
                bot.facing
            }

            occupiedByBots.remove(bot.pos)
            occupiedByBots.add(finalPos)

            bot.copy(
                pos = finalPos,
                facing = newFacing,
                state = nextState,
                reactionCounter = nextReaction,
                lastKnownPlayerPos = nextLastKnown,
                lastKnownPlayerVelocity = nextVelocity,
                searchTimerTicks = nextSearchTimer,
                targetPos = goalPos,
                currentPath = path,
                currentWaypointIndex = nextWaypointIdx
            )
        }

        val newHealth = (state.playerHealth - playerDamageTaken).coerceIn(0, 100)
        val defeated = newHealth <= 0
        if (defeated) {
            latestAiLogAr = "تمت محاصرتك واستنفدت طاقتك! أعد المحاولة أو غيّر خطة التمويه."
            latestAiLogEn = "You were intercepted! Reset expedition or try a stealthier route."
        } else if (playerDamageTaken > 0) {
            latestAiLogAr = "تنبيه: اقترب منك روبوت حراسة (-$playerDamageTaken صحة)! استخدم التمويه أو الشجيرات."
            latestAiLogEn = "Alert: Bot intercepted you (-$playerDamageTaken HP)! Use cover or decoy."
        }

        return state.copy(
            bots = updatedBots,
            playerHealth = newHealth,
            isPlayerDefeated = defeated,
            aiStatusLogAr = latestAiLogAr,
            aiStatusLogEn = latestAiLogEn
        )
    }

    private fun computeFlankTarget(
        tiles: List<List<TileType>>,
        playerPos: GridPoint,
        playerVel: GridPoint,
        botIndex: Int
    ): GridPoint {
        val offsets = listOf(
            GridPoint(playerVel.x * 2 + 2, playerVel.y * 2 - 1),
            GridPoint(playerVel.x * 2 - 2, playerVel.y * 2 + 1),
            GridPoint(0, -2),
            GridPoint(0, 2)
        )
        val pick = offsets[botIndex % offsets.size]
        val candidate = GridPoint(
            (playerPos.x + pick.x).coerceIn(0, MAP_W - 1),
            (playerPos.y + pick.y).coerceIn(0, MAP_H - 1)
        )
        return if (isWalkable(tiles, candidate)) candidate else playerPos
    }

    private fun findNearestCoverTile(
        tiles: List<List<TileType>>,
        botPos: GridPoint,
        awayFrom: GridPoint
    ): GridPoint {
        var best = botPos
        var bestScore = -999f
        for (y in 0 until MAP_H) {
            for (x in 0 until MAP_W) {
                val pt = GridPoint(x, y)
                if (!isWalkable(tiles, pt)) continue
                val distFromBot = botPos.distanceTo(pt)
                if (distFromBot > 4.5f) continue
                val distFromThreat = awayFrom.distanceTo(pt)
                val coverBonus = if (tiles[y][x] == TileType.BUSH_COVER) 4f else 0f
                val score = distFromThreat + coverBonus - (distFromBot * 0.4f)
                if (score > bestScore) {
                    bestScore = score
                    best = pt
                }
            }
        }
        return best
    }

    // --- Tactical Gadgets & Grid Inventory Operations ---

    fun throwAcousticDecoy(state: ExpeditionWorldState, targetPos: GridPoint? = null): ExpeditionWorldState {
        if (state.decoyCooldownTicks > 0 || state.isMissionComplete || state.isPlayerDefeated) return state
        val decoySpot = targetPos ?: GridPoint(
            (state.playerPos.x + 3).coerceIn(1, MAP_W - 2),
            (state.playerPos.y - 2).coerceIn(1, MAP_H - 2)
        )
        val updated = state.copy(
            activeDecoyPos = decoySpot,
            decoyTicksRemaining = 5,
            decoyCooldownTicks = 6,
            aiStatusLogAr = "رميت حجر تمويه صوتي عند (${decoySpot.x}, ${decoySpot.y}) لجذب الروبوتات بعيداً!",
            aiStatusLogEn = "Threw acoustic decoy at (${decoySpot.x}, ${decoySpot.y}) to divert patrol bots!"
        )
        return stepBotsAi(updated)
    }

    fun triggerFlashlightStrobe(state: ExpeditionWorldState): ExpeditionWorldState {
        if (state.flareCooldownTicks > 0 || state.isMissionComplete || state.isPlayerDefeated) return state
        var stunnedCount = 0
        val updatedBots = state.bots.map { bot ->
            if (bot.pos.distanceTo(state.playerPos) <= 3.8f) {
                stunnedCount++
                bot.copy(stunTicks = 3, state = BotAiState.RETREAT_COVER)
            } else {
                bot
            }
        }
        return state.copy(
            bots = updatedBots,
            flareCooldownTicks = 7,
            aiStatusLogAr = if (stunnedCount > 0) {
                "أطلقت وميض المصباح الباهر! تم إرباك $stunnedCount روبوت وإجباره على الانسحاب للتغطية."
            } else {
                "أطلقت وميض المصباح الباهر (لا توجد روبوتات في المدى القريب)."
            },
            aiStatusLogEn = if (stunnedCount > 0) {
                "Triggered Strobe Flare! Disoriented $stunnedCount bot(s) into Retreat Cover."
            } else {
                "Triggered Strobe Flare (no bots within close range)."
            }
        )
    }

    fun consumeSupplyFromGrid(state: ExpeditionWorldState, slotIndex: Int): ExpeditionWorldState {
        if (slotIndex !in state.inventoryGrid.indices) return state
        val cell = state.inventoryGrid[slotIndex]
        val itemId = cell.itemId ?: return state

        val newGrid = state.inventoryGrid.toMutableList()
        if (cell.quantity > 1) {
            newGrid[slotIndex] = cell.copy(quantity = cell.quantity - 1)
        } else {
            newGrid[slotIndex] = InventoryGridCell(index = slotIndex, itemId = null, quantity = 0)
        }

        val healAmount = if (itemId.startsWith("aid-")) 35 else 20
        val staminaAmount = if (itemId.startsWith("food-") || itemId.startsWith("water-")) 40 else 20

        return state.copy(
            playerHealth = (state.playerHealth + healAmount).coerceIn(0, 100),
            playerStamina = (state.playerStamina + staminaAmount).coerceIn(0, 100),
            inventoryGrid = newGrid,
            selectedGridSlotIndex = null,
            aiStatusLogAr = "استخدمت إمدادات من الحقيبة (+$healAmount صحة، +$staminaAmount طاقة).",
            aiStatusLogEn = "Used field supply from backpack (+$healAmount HP, +$staminaAmount Stamina)."
        )
    }

    /**
     * Moves or swaps items between two inventory grid slots (supports Drag-and-Drop & Tap-to-Swap).
     */
    fun moveOrSwapGridSlots(
        grid: List<InventoryGridCell>,
        fromIndex: Int,
        toIndex: Int
    ): List<InventoryGridCell> {
        if (fromIndex == toIndex || fromIndex !in grid.indices || toIndex !in grid.indices) return grid
        val mutable = grid.toMutableList()
        val fromCell = mutable[fromIndex]
        val toCell = mutable[toIndex]

        if (fromCell.itemId != null && fromCell.itemId == toCell.itemId) {
            // Stack identical items
            mutable[toIndex] = toCell.copy(quantity = toCell.quantity + fromCell.quantity)
            mutable[fromIndex] = InventoryGridCell(index = fromIndex, itemId = null, quantity = 0)
        } else {
            mutable[toIndex] = InventoryGridCell(index = toIndex, itemId = fromCell.itemId, quantity = fromCell.quantity)
            mutable[fromIndex] = InventoryGridCell(index = fromIndex, itemId = toCell.itemId, quantity = toCell.quantity)
        }
        return mutable
    }

    fun addItemToGrid(
        grid: List<InventoryGridCell>,
        itemId: String,
        quantity: Int = 1
    ): List<InventoryGridCell> {
        val mutable = grid.toMutableList()
        val existingIdx = mutable.indexOfFirst { it.itemId == itemId }
        if (existingIdx >= 0) {
            val cell = mutable[existingIdx]
            mutable[existingIdx] = cell.copy(quantity = cell.quantity + quantity)
            return mutable
        }
        val emptyIdx = mutable.indexOfFirst { it.itemId == null }
        if (emptyIdx >= 0) {
            mutable[emptyIdx] = InventoryGridCell(index = emptyIdx, itemId = itemId, quantity = quantity)
        }
        return mutable
    }
}
