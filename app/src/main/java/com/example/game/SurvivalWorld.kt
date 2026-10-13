package com.example.game

import com.example.model.AiDifficulty
import com.example.model.AppLanguage
import com.example.model.BotAiState
import com.example.model.CharacterConfig
import com.example.model.GridPoint
import kotlin.math.sqrt

enum class ObstacleType {
    ROCK_BOULDER,
    TREE_PINE,
    TREE_OAK,
    BUSH_DENSE,
    CABIN_WALL,
    WATER_POND,
    CAMPFIRE
}

data class WorldObstacle(
    val id: String,
    val type: ObstacleType,
    val x: Float,
    val y: Float,
    val radius: Float,           // Collision radius on the ground
    val canopyRadius: Float = radius, // Overhead visual radius
    val width: Float = radius * 2,
    val height: Float = radius * 2,
    val rotation: Float = 0f
) {
    fun containsPoint(px: Float, py: Float): Boolean {
        val dx = px - x
        val dy = py - y
        return (dx * dx + dy * dy) <= (radius * radius)
    }

    fun isUnderCanopy(px: Float, py: Float): Boolean {
        if (canopyRadius <= 0f) return false
        val dx = px - x
        val dy = py - (y - 30f) // Canopy is visually centered slightly higher
        return (dx * dx + dy * dy) <= (canopyRadius * canopyRadius)
    }
}

data class WorldSupplyItem(
    val id: String,
    val itemId: String,
    val x: Float,
    val y: Float,
    val isPrimaryObjective: Boolean,
    val collected: Boolean = false,
    val nameAr: String,
    val nameEn: String,
    val pulseTimer: Float = 0f
) {
    fun name(lang: AppLanguage): String = if (lang == AppLanguage.AR) nameAr else nameEn
}

data class DecoyProjectile(
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float,
    var currentX: Float = startX,
    var currentY: Float = startY,
    var progress: Float = 0f,
    var hasLanded: Boolean = false,
    var rippleRadius: Float = 0f,
    var rippleAlpha: Float = 1f
)

data class FootstepDecal(
    val x: Float,
    val y: Float,
    val angleDegrees: Float,
    var alpha: Float = 0.5f,
    val isLeft: Boolean
)

data class AmbientParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    val size: Float,
    val isLeaf: Boolean
)

data class BotCharacter(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var facingDegrees: Float = 90f,
    var animPhase: Float = 0f,
    var isMoving: Boolean = false,
    var state: BotAiState = BotAiState.PATROL,
    var health: Int = 100,
    var stunSeconds: Float = 0f,
    var alertPulse: Float = 0f,
    var lastKnownPlayerX: Float? = null,
    var lastKnownPlayerY: Float? = null,
    var searchTimerSeconds: Float = 0f,
    var waypoints: List<Pair<Float, Float>> = emptyList(),
    var currentWaypointIndex: Int = 0,
    var primaryColorHex: String = "#3D6B52",
    var vestColorHex: String = "#5A4D41",
    var hatColorHex: String = "#7C6A56",
    var skinColorHex: String = "#E0B187",
    var hairColorHex: String = "#3B271A"
) {
    fun name(lang: AppLanguage): String = if (lang == AppLanguage.AR) nameAr else nameEn
}

data class SurvivalPlayer(
    var x: Float = 280f,
    var y: Float = 1450f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var facingDegrees: Float = 0f,
    var animPhase: Float = 0f,
    var isMoving: Boolean = false,
    var isRunning: Boolean = false,
    var health: Int = 100,
    var stamina: Float = 100f,
    var inStealthBush: Boolean = false,
    var flashlightOn: Boolean = true
)

data class ExtractionZone(
    val x: Float = 2100f,
    val y: Float = 320f,
    val radius: Float = 95f,
    var pulseTimer: Float = 0f
)

class SurvivalWorldData {
    val width: Float = 2400f
    val height: Float = 1800f

    val extractionZone = ExtractionZone()

    val obstacles: List<WorldObstacle> = listOf(
        // Rock ridges near entrance
        WorldObstacle("rock-1", ObstacleType.ROCK_BOULDER, 450f, 1380f, radius = 55f),
        WorldObstacle("rock-2", ObstacleType.ROCK_BOULDER, 520f, 1420f, radius = 45f),
        WorldObstacle("rock-3", ObstacleType.ROCK_BOULDER, 720f, 1260f, radius = 60f),

        // Deep forest trees (Oaks and Pines)
        WorldObstacle("tree-1", ObstacleType.TREE_PINE, 350f, 1220f, radius = 22f, canopyRadius = 75f),
        WorldObstacle("tree-2", ObstacleType.TREE_OAK, 200f, 1320f, radius = 26f, canopyRadius = 88f),
        WorldObstacle("tree-3", ObstacleType.TREE_PINE, 580f, 1140f, radius = 22f, canopyRadius = 72f),
        WorldObstacle("tree-4", ObstacleType.TREE_OAK, 840f, 1400f, radius = 26f, canopyRadius = 84f),
        WorldObstacle("tree-5", ObstacleType.TREE_PINE, 760f, 1080f, radius = 22f, canopyRadius = 76f),

        // Dense stealth bushes along the trail
        WorldObstacle("bush-1", ObstacleType.BUSH_DENSE, 400f, 1460f, radius = 48f),
        WorldObstacle("bush-2", ObstacleType.BUSH_DENSE, 650f, 1320f, radius = 52f),
        WorldObstacle("bush-3", ObstacleType.BUSH_DENSE, 920f, 1180f, radius = 55f),

        // Abandoned Ranger Cabin in the central clearing
        WorldObstacle("cabin-wall-1", ObstacleType.CABIN_WALL, 1150f, 920f, radius = 70f, canopyRadius = 110f),
        WorldObstacle("campfire-1", ObstacleType.CAMPFIRE, 1280f, 960f, radius = 25f),

        // Central rocky ridge & stream boulders
        WorldObstacle("rock-4", ObstacleType.ROCK_BOULDER, 1380f, 760f, radius = 65f),
        WorldObstacle("rock-5", ObstacleType.ROCK_BOULDER, 1480f, 720f, radius = 50f),
        WorldObstacle("tree-6", ObstacleType.TREE_PINE, 1550f, 850f, radius = 24f, canopyRadius = 80f),
        WorldObstacle("tree-7", ObstacleType.TREE_OAK, 1320f, 1150f, radius = 28f, canopyRadius = 92f),
        WorldObstacle("bush-4", ObstacleType.BUSH_DENSE, 1450f, 1020f, radius = 48f),

        // Upper forest grove & extraction approaches
        WorldObstacle("rock-6", ObstacleType.ROCK_BOULDER, 1750f, 520f, radius = 62f),
        WorldObstacle("rock-7", ObstacleType.ROCK_BOULDER, 1850f, 480f, radius = 48f),
        WorldObstacle("tree-8", ObstacleType.TREE_PINE, 1680f, 420f, radius = 24f, canopyRadius = 78f),
        WorldObstacle("tree-9", ObstacleType.TREE_OAK, 1920f, 620f, radius = 26f, canopyRadius = 86f),
        WorldObstacle("tree-10", ObstacleType.TREE_PINE, 1980f, 220f, radius = 24f, canopyRadius = 80f),
        WorldObstacle("bush-5", ObstacleType.BUSH_DENSE, 1820f, 380f, radius = 50f),
        WorldObstacle("bush-6", ObstacleType.BUSH_DENSE, 2020f, 440f, radius = 46f)
    )

    fun createSupplies(): MutableList<WorldSupplyItem> = mutableListOf(
        WorldSupplyItem(
            id = "cache-beacon-1",
            itemId = "nav-brass-compass",
            x = 680f,
            y = 1200f,
            isPrimaryObjective = true,
            nameAr = "منارة الإشارة 1: بوصلة الملاحة",
            nameEn = "Signal Beacon 1: Brass Compass"
        ),
        WorldSupplyItem(
            id = "cache-beacon-2",
            itemId = "aid-field-pouch-red",
            x = 1180f,
            y = 880f,
            isPrimaryObjective = true,
            nameAr = "منارة الإشارة 2: حقيبة الإسعافات الطبية",
            nameEn = "Signal Beacon 2: First-Aid Kit"
        ),
        WorldSupplyItem(
            id = "cache-beacon-3",
            itemId = "food-freeze-dried-meal",
            x = 1720f,
            y = 480f,
            isPrimaryObjective = true,
            nameAr = "منارة الإشارة 3: حصص الطوارئ الغذائية",
            nameEn = "Signal Beacon 3: Survival Rations"
        ),
        WorldSupplyItem(
            id = "supply-water-1",
            itemId = "water-steel-flask-1l",
            x = 420f,
            y = 1420f,
            isPrimaryObjective = false,
            nameAr = "قارورة مياه فولاذية",
            nameEn = "Insulated Water Flask"
        ),
        WorldSupplyItem(
            id = "supply-food-2",
            itemId = "food-trail-mix-pouch",
            x = 1320f,
            y = 1010f,
            isPrimaryObjective = false,
            nameAr = "مكسرات وفواكه طاقة",
            nameEn = "High-Energy Trail Mix"
        ),
        WorldSupplyItem(
            id = "supply-light-3",
            itemId = "light-aluminum-torch",
            x = 1880f,
            y = 560f,
            isPrimaryObjective = false,
            nameAr = "مصباح يدوي تكتيكي",
            nameEn = "Tactical Flashlight"
        )
    )

    fun createBots(): MutableList<BotCharacter> = mutableListOf(
        BotCharacter(
            id = "bot-scout-alpha",
            nameAr = "كشاف الغابة ألفا",
            nameEn = "Scout Alpha",
            x = 880f,
            y = 1180f,
            facingDegrees = 180f,
            primaryColorHex = "#7C6A56",
            vestColorHex = "#4E8768",
            hatColorHex = "#3D6B52",
            waypoints = listOf(
                Pair(880f, 1180f),
                Pair(620f, 1220f),
                Pair(520f, 1360f),
                Pair(750f, 1340f)
            )
        ),
        BotCharacter(
            id = "bot-tracker-bravo",
            nameAr = "متعقب الأثر برافو",
            nameEn = "Tracker Bravo",
            x = 1420f,
            y = 860f,
            facingDegrees = 270f,
            primaryColorHex = "#2D543F",
            vestColorHex = "#5A4D41",
            hatColorHex = "#9E3A26",
            waypoints = listOf(
                Pair(1420f, 860f),
                Pair(1180f, 920f),
                Pair(1300f, 1100f),
                Pair(1500f, 980f)
            )
        ),
        BotCharacter(
            id = "bot-flanker-charlie",
            nameAr = "مطوّق المحيط تشارلي",
            nameEn = "Flanker Charlie",
            x = 1820f,
            y = 520f,
            facingDegrees = 220f,
            primaryColorHex = "#2B3E52",
            vestColorHex = "#272E2B",
            hatColorHex = "#E5A93C",
            waypoints = listOf(
                Pair(1820f, 520f),
                Pair(1650f, 440f),
                Pair(1900f, 380f),
                Pair(1780f, 620f)
            )
        )
    )
}
