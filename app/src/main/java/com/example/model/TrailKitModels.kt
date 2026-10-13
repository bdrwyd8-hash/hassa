package com.example.model

enum class AppLanguage(val code: String, val isRtl: Boolean) {
    AR("ar", true),
    EN("en", false)
}

enum class AppThemeMode {
    DARK,
    LIGHT
}

enum class MainSection {
    STUDIO,
    EXPEDITION,
    MY_CHARACTERS,
    CHECKLIST,
    SETTINGS
}

enum class AiDifficulty(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val descAr: String,
    val descEn: String,
    val visionRange: Float,
    val reactionDelayTicks: Int,
    val searchDurationTicks: Int,
    val enablesSquadFlanking: Boolean,
    val enablesPredictiveSearch: Boolean
) {
    RECRUIT(
        id = "recruit",
        nameAr = "مبتدئ (استجابة فردية)",
        nameEn = "Recruit (Basic Decisions)",
        descAr = "مدى رؤية قصير، استجابة أبطأ، بحث محدود دون تنسيق جماعي (نفس نقاط الصحة 100).",
        descEn = "Shorter vision, slower reaction, brief search, no squad flanking (Same 100 HP).",
        visionRange = 4.5f,
        reactionDelayTicks = 2,
        searchDurationTicks = 5,
        enablesSquadFlanking = false,
        enablesPredictiveSearch = false
    ),
    RANGER(
        id = "ranger",
        nameAr = "حارس متمرس (تكتيك متوازن)",
        nameEn = "Ranger (Smart Search)",
        descAr = "تجنب عقبات ذكي، بحث في آخر موقع معروف، وتنبيه الحلفاء القريبين (نفس نقاط الصحة 100).",
        descEn = "A* obstacle avoidance, thorough last-known-position search & local alerts (Same 100 HP).",
        visionRange = 6.2f,
        reactionDelayTicks = 1,
        searchDurationTicks = 10,
        enablesSquadFlanking = true,
        enablesPredictiveSearch = false
    ),
    TACTICIAN(
        id = "tactician",
        nameAr = "خبير تكتيكي (تعاون وتطويق)",
        nameEn = "Tactician (Cooperative Flanking)",
        descAr = "توقع مسار اللاعب، تطويق من مسارات بديلة، وتغيير الخطة عند الخطر (نفس نقاط الصحة 100).",
        descEn = "Predictive search vector, multi-bot pincer flanking & adaptive retreat (Same 100 HP).",
        visionRange = 7.8f,
        reactionDelayTicks = 0,
        searchDurationTicks = 16,
        enablesSquadFlanking = true,
        enablesPredictiveSearch = true
    );

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.AR) nameAr else nameEn
    fun description(lang: AppLanguage): String = if (lang == AppLanguage.AR) descAr else descEn
}

enum class BotAiState(val nameAr: String, val nameEn: String, val badgeColorHex: String) {
    PATROL("دورية", "Patrol", "#4E8768"),
    HARVEST_OBJECTIVE("تأمين هدف", "Contest Cache", "#3B5978"),
    SEARCH_LAST_KNOWN("بحث في آخر أثر", "Searching LKP", "#E5A93C"),
    PURSUE("ملاحقة مباشرة", "Pursuing", "#D9534F"),
    FLANK_COORDINATE("تطويق تعاوني", "Flanking", "#9E3A26"),
    RETREAT_COVER("انسحاب للتغطية", "Taking Cover", "#7C6A56");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.AR) nameAr else nameEn
}

enum class TileType {
    GROUND,
    WALL_ROCK,
    BUSH_COVER,
    WATER_STREAM,
    EXTRACTION_ZONE
}

data class GridPoint(val x: Int, val y: Int) {
    fun manhattanTo(other: GridPoint): Int = kotlin.math.abs(x - other.x) + kotlin.math.abs(y - other.y)
    fun distanceTo(other: GridPoint): Float {
        val dx = (x - other.x).toFloat()
        val dy = (y - other.y).toFloat()
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

data class ResourceNode(
    val id: String,
    val pos: GridPoint,
    val itemId: String,
    val isPrimaryObjective: Boolean,
    val collected: Boolean = false
)

data class BotAgent(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val pos: GridPoint,
    val facing: GridPoint = GridPoint(0, 1),
    val state: BotAiState = BotAiState.PATROL,
    val health: Int = 100, // Constant across difficulties!
    val stunTicks: Int = 0,
    val reactionCounter: Int = 0,
    val lastKnownPlayerPos: GridPoint? = null,
    val lastKnownPlayerVelocity: GridPoint = GridPoint(0, 0),
    val searchTimerTicks: Int = 0,
    val targetPos: GridPoint = pos,
    val currentPath: List<GridPoint> = emptyList(),
    val patrolWaypoints: List<GridPoint> = emptyList(),
    val currentWaypointIndex: Int = 0
) {
    fun name(lang: AppLanguage): String = if (lang == AppLanguage.AR) nameAr else nameEn
}

data class InventoryGridCell(
    val index: Int, // 0 until 20 (5 cols x 4 rows)
    val itemId: String? = null,
    val quantity: Int = 0
)

data class ExpeditionProgression(
    val missionsCompleted: Int = 0,
    val totalCachesSecured: Int = 0,
    val highestDifficultyWon: String = ""
)

enum class EquipmentCategory(
    val id: String,
    val nameAr: String,
    val nameEn: String
) {
    ALL("all", "الكل", "All Gear"),
    HEADWEAR("headwear", "أغطية الرأس", "Headwear"),
    SHIRTS_JACKETS("shirts_jackets", "القمصان والسترات", "Shirts & Jackets"),
    PANTS("pants", "السراويل", "Pants"),
    FOOTWEAR("footwear", "الأحذية الجبلية", "Boots & Footwear"),
    BACKPACKS("backpacks", "حقائب الظهر", "Backpacks"),
    WATER_BOTTLES("water_bottles", "قوارير المياه", "Water Bottles"),
    FOOD_SUPPLIES("food_supplies", "إمدادات الغذاء", "Food Supplies"),
    FLASHLIGHTS("flashlights", "المصابيح والفوانيس", "Flashlights & Lanterns"),
    MAPS_COMPASSES("maps_compasses", "الخرائط والبوصلات", "Maps & Compasses"),
    FIRST_AID("first_aid", "الإسعافات الأولية", "First-Aid Kits"),
    TENTS_SLEEPING("tents_sleeping", "الخيام وأكياس النوم", "Tents & Sleeping Bags"),
    WEATHER_PROTECTION("weather_protection", "الحماية من الطقس", "Weather Protection"),
    HIKING_ACCESSORIES("hiking_accessories", "ملحقات التنزه", "Hiking Accessories"),
    PATCHES_DECALS("patches_decals", "الشارات والألوان", "Patches & Badges");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.AR) nameAr else nameEn
}

/**
 * Deterministic visual slots for layering on the 2D character canvas.
 * Base body and head are always rendered; equipment layers sit in deterministic zOrder
 * and can also be explicitly moved up/down or moved between compatible slots.
 */
enum class LayerSlot(
    val id: String,
    val defaultZOrder: Int,
    val nameAr: String,
    val nameEn: String
) {
    CAMP_GEAR("camp_gear", 10, "معدات التخييم الخلفية", "Camp & Shelter Gear"),
    BACKPACK("backpack", 15, "حقيبة الظهر", "Backpack"),
    PANTS("pants", 30, "السروال", "Pants"),
    FOOTWEAR("footwear", 35, "الحذاء", "Footwear"),
    UPPER_BODY("upper_body", 40, "القميص الأساسي", "Upper-Body Shirt"),
    OUTERWEAR("outerwear", 45, "السترة الخارجية", "Jacket & Outerwear"),
    WEATHER_GEAR("weather_gear", 50, "معدات الطقس", "Weather Protection"),
    HEADWEAR("headwear", 60, "غطاء الرأس", "Hat & Headwear"),
    BELT_POUCH("belt_pouch", 65, "حقيبة الخصر والإمدادات", "Belt Pouch & Supplies"),
    LEFT_HAND_GEAR("left_hand_gear", 70, "معدات الجانب الأيسر", "Left-Side Gear"),
    RIGHT_HAND_GEAR("right_hand_gear", 75, "معدات الجانب الأيمن", "Right-Side Gear"),
    NECK_CHEST_GEAR("neck_chest_gear", 80, "معدات الصدر والملاحة", "Chest & Navigation Gear"),
    DECORATIVE_PATCH("decorative_patch", 85, "شارة تزيينية", "Decorative Patch");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.AR) nameAr else nameEn
}

data class EquipmentItem(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val category: EquipmentCategory,
    val defaultSlot: LayerSlot,
    val compatibleSlots: List<LayerSlot> = listOf(defaultSlot),
    val colors: List<String>,
    val weightGrams: Int,
    val icon: String,
    val descriptionAr: String,
    val descriptionEn: String
) {
    fun name(lang: AppLanguage): String = if (lang == AppLanguage.AR) nameAr else nameEn
    fun description(lang: AppLanguage): String = if (lang == AppLanguage.AR) descriptionAr else descriptionEn
}

data class EquippedLayer(
    val slot: LayerSlot,
    val itemId: String,
    val colorHex: String,
    val visible: Boolean = true,
    val zOrder: Int = slot.defaultZOrder
)

enum class SceneBackground(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val skyTopHex: String,
    val skyBottomHex: String,
    val mountainFarHex: String,
    val mountainNearHex: String,
    val groundHex: String,
    val isNeutral: Boolean = false
) {
    PINE_VALLEY(
        id = "pine_valley",
        nameAr = "وادي الصنوبر",
        nameEn = "Pine Valley",
        skyTopHex = "#1A2E26",
        skyBottomHex = "#2F4F3E",
        mountainFarHex = "#223A2E",
        mountainNearHex = "#192B22",
        groundHex = "#132019"
    ),
    DESERT_CANYON(
        id = "desert_canyon",
        nameAr = "وادي الصحراء",
        nameEn = "Desert Canyon",
        skyTopHex = "#3A2618",
        skyBottomHex = "#6E472B",
        mountainFarHex = "#54341E",
        mountainNearHex = "#3D2414",
        groundHex = "#2B190E"
    ),
    ALPINE_PEAK(
        id = "alpine_peak",
        nameAr = "القمة الجليدية",
        nameEn = "Alpine Peak",
        skyTopHex = "#1B2838",
        skyBottomHex = "#364F6B",
        mountainFarHex = "#4A6583",
        mountainNearHex = "#2C3E50",
        groundHex = "#22303F"
    ),
    STARRY_CAMP(
        id = "starry_camp",
        nameAr = "مخيم النجوم",
        nameEn = "Starry Basecamp",
        skyTopHex = "#0F141C",
        skyBottomHex = "#1D2736",
        mountainFarHex = "#16202B",
        mountainNearHex = "#101820",
        groundHex = "#0D1318"
    ),
    NEUTRAL_STUDIO(
        id = "neutral_studio",
        nameAr = "خلفية محايدة للتصدير",
        nameEn = "Neutral Export Studio",
        skyTopHex = "#232A27",
        skyBottomHex = "#1B211E",
        mountainFarHex = "#232A27",
        mountainNearHex = "#1B211E",
        groundHex = "#171C19",
        isNeutral = true
    );

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.AR) nameAr else nameEn
}

data class CharacterPreset(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val config: CharacterConfig
) {
    fun label(lang: AppLanguage): String = if (lang == AppLanguage.AR) nameAr else nameEn
}

data class CharacterConfig(
    val skinColorHex: String = "#E0B187",
    val hairStyle: Int = 0, // 0: Short Explorer, 1: Wavy Trail, 2: Tied Bun, 3: Cropped Buzz
    val hairColorHex: String = "#2C221E",
    val eyeColorHex: String = "#3B2F2F",
    val baseUnderwearColorHex: String = "#3A4E43",
    val scene: SceneBackground = SceneBackground.PINE_VALLEY,
    val equippedLayers: Map<LayerSlot, EquippedLayer> = emptyMap()
) {
    fun orderedLayers(): List<EquippedLayer> {
        return equippedLayers.values.sortedWith(
            compareBy<EquippedLayer> { it.zOrder }.thenBy { it.slot.defaultZOrder }
        )
    }
}

data class SavedCharacter(
    val id: String,
    val name: String,
    val updatedAt: Long,
    val config: CharacterConfig
)

enum class CharacterSortOption(val nameAr: String, val nameEn: String) {
    RECENT("الأحدث تعديلاً", "Recently Modified"),
    NAME("الاسم (أ-ي)", "Name (A-Z)");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.AR) nameAr else nameEn
}

enum class ItemSortOption(val nameAr: String, val nameEn: String) {
    DEFAULT("الترتيب الافتراضي", "Default"),
    NAME("الاسم", "Name"),
    WEIGHT_ASC("الوزن: من الأخف", "Weight: Lightest"),
    WEIGHT_DESC("الوزن: من الأثقل", "Weight: Heaviest");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.AR) nameAr else nameEn
}

enum class TripType(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val subtitleAr: String,
    val subtitleEn: String
) {
    DAY_HIKE(
        "day_hike",
        "نزهة نهارية",
        "Day Hike",
        "مسار جبلي ليوم واحد مع المستلزمات الأساسية",
        "Single-day trail walk with essential supplies"
    ),
    CAMPING(
        "camping",
        "تخييم بري",
        "Camping Trip",
        "إقامة ليلية في الطبيعة مع المأوى والإضاءة",
        "Overnight outdoor stay with shelter & lighting"
    ),
    COLD_WEATHER(
        "cold_weather",
        "رحلة طقس بارد",
        "Cold-Weather Trip",
        "عزل حراري وطبقات حماية من الرياح والبرودة",
        "Thermal insulation & windproof layering"
    ),
    EMERGENCY_BAG(
        "emergency_bag",
        "حقيبة طوارئ عامة",
        "Emergency Preparedness Bag",
        "جاهزية عامة للإمدادات والإسعافات الأولية",
        "General readiness supplies & first-aid essentials"
    );

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.AR) nameAr else nameEn
    fun subtitle(lang: AppLanguage): String = if (lang == AppLanguage.AR) subtitleAr else subtitleEn
}

data class ChecklistEntry(
    val id: String,
    val tripType: TripType,
    val titleAr: String,
    val titleEn: String,
    val weightGrams: Int = 250,
    val isPacked: Boolean = false,
    val isCustom: Boolean = false
) {
    fun title(lang: AppLanguage): String = if (lang == AppLanguage.AR) titleAr else titleEn
}

data class UserPreferences(
    val language: AppLanguage = AppLanguage.AR,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val animationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true
)
