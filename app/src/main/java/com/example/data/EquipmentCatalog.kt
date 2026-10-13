package com.example.data

import com.example.model.CharacterConfig
import com.example.model.CharacterPreset
import com.example.model.ChecklistEntry
import com.example.model.EquipmentCategory
import com.example.model.EquipmentItem
import com.example.model.EquippedLayer
import com.example.model.LayerSlot
import com.example.model.SceneBackground
import com.example.model.TripType

object EquipmentCatalog {

    val skinTones = listOf(
        "#F5D0B5",
        "#E0B187",
        "#C68B59",
        "#9C643B",
        "#6B4226",
        "#4A2C18"
    )

    val hairColors = listOf(
        "#1E1916",
        "#3B271A",
        "#6E4427",
        "#B57B3F",
        "#9E3A26",
        "#7D848A",
        "#2D543F"
    )

    val customPickerPalette = listOf(
        "#3D6B52", "#2D543F", "#528C6C", "#71A889",
        "#E5A93C", "#C88422", "#9E582A", "#D9534F",
        "#3B5978", "#2B3E52", "#5D7A99", "#7C6A56",
        "#5A4D41", "#8C7D70", "#C4B9AC", "#E8ECE9",
        "#272E2B", "#434E49"
    )

    private val flexibleAccessorySlots = listOf(
        LayerSlot.LEFT_HAND_GEAR,
        LayerSlot.RIGHT_HAND_GEAR,
        LayerSlot.BELT_POUCH,
        LayerSlot.NECK_CHEST_GEAR
    )

    /**
     * 46 original starter outdoor items distributed across all 14 safe categories.
     * Every item has a unique permanent ID, bilingual name/description, supported colors,
     * weight in grams, and a local vector icon key.
     */
    val items: List<EquipmentItem> = listOf(
        // 1. HEADWEAR (4 items)
        EquipmentItem(
            id = "head-sun-brim-hat",
            nameAr = "قبعة شمسية عريضة الحواف",
            nameEn = "Wide-Brim Trail Sun Hat",
            category = EquipmentCategory.HEADWEAR,
            defaultSlot = LayerSlot.HEADWEAR,
            colors = listOf("#7C6A56", "#3D6B52", "#C4B9AC", "#2F3834"),
            weightGrams = 115,
            icon = "hat_brim",
            descriptionAr = "قبعة خفيفة بحواف واسعة لحماية الوجه والرقبة من أشعة الشمس في المسارات المفتوحة.",
            descriptionEn = "Lightweight wide-brim hat for UV shade on exposed mountain ridges."
        ),
        EquipmentItem(
            id = "head-merino-beanie",
            nameAr = "قبعة صوف ميرينو حرارية",
            nameEn = "Merino Wool Summit Beanie",
            category = EquipmentCategory.HEADWEAR,
            defaultSlot = LayerSlot.HEADWEAR,
            colors = listOf("#E5A93C", "#3D6B52", "#9E3A26", "#2B3E52"),
            weightGrams = 75,
            icon = "beanie",
            descriptionAr = "قبعة صوفية دافئة تحافظ على حرارة الرأس في الليالي الباردة والقمم العالية.",
            descriptionEn = "Ribbed merino wool knit cap for cold-weather insulation at camp."
        ),
        EquipmentItem(
            id = "head-trail-cap",
            nameAr = "قبعة مسافات طويلة مهواة",
            nameEn = "Breathable Trail Runner Cap",
            category = EquipmentCategory.HEADWEAR,
            defaultSlot = LayerSlot.HEADWEAR,
            colors = listOf("#3D6B52", "#3B5978", "#E5A93C", "#272E2B"),
            weightGrams = 65,
            icon = "cap",
            descriptionAr = "قبعة رياضية سريعة الجفاف مع فتحات تهوية جانبية للمشي السريع.",
            descriptionEn = "Quick-dry visor cap with laser-cut ventilation panels."
        ),
        EquipmentItem(
            id = "head-desert-keffiyeh-wrap",
            nameAr = "وشاح رأس صحراوي واقٍ",
            nameEn = "Desert Sun & Sand Headscarf",
            category = EquipmentCategory.HEADWEAR,
            defaultSlot = LayerSlot.HEADWEAR,
            colors = listOf("#C4B9AC", "#9E582A", "#3D6B52", "#3B5978"),
            weightGrams = 130,
            icon = "head_wrap",
            descriptionAr = "غطاء رأس قطني يحمي من الغبار والرياح الرملية وأشعة الشمس القوية.",
            descriptionEn = "Breathable woven head wrap shielding against dust, wind, and sun glare."
        ),

        // 2. SHIRTS & JACKETS (5 items: 2 shirts in UPPER_BODY, 3 jackets in OUTERWEAR)
        EquipmentItem(
            id = "shirt-merino-base",
            nameAr = "قميص أساسي صوف ميرينو",
            nameEn = "Merino Thermal Base Shirt",
            category = EquipmentCategory.SHIRTS_JACKETS,
            defaultSlot = LayerSlot.UPPER_BODY,
            colors = listOf("#3D6B52", "#3B5978", "#7C6A56", "#9E3A26", "#272E2B"),
            weightGrams = 190,
            icon = "shirt_base",
            descriptionAr = "طبقة أساسية طويلة الأكمام تنظم حرارة الجسم وتمتص الرطوبة.",
            descriptionEn = "Long-sleeve moisture-wicking base layer for all-season trekking."
        ),
        EquipmentItem(
            id = "shirt-safari-flannel",
            nameAr = "قميص رحلات بجيوب صدرية",
            nameEn = "Overland Woven Utility Shirt",
            category = EquipmentCategory.SHIRTS_JACKETS,
            defaultSlot = LayerSlot.UPPER_BODY,
            colors = listOf("#7C6A56", "#4E8768", "#9E582A", "#5D7A99"),
            weightGrams = 240,
            icon = "shirt_woven",
            descriptionAr = "قميص متين مقاوم للتمزق مع جيوب علوية لحفظ الخرائط الصغيرة.",
            descriptionEn = "Ripstop button-up trail shirt with dual chest pockets."
        ),
        EquipmentItem(
            id = "jacket-alpine-shell",
            nameAr = "سترة جبلية مقاومة للرياح",
            nameEn = "Alpine Hardshell Parka",
            category = EquipmentCategory.SHIRTS_JACKETS,
            defaultSlot = LayerSlot.OUTERWEAR,
            colors = listOf("#3D6B52", "#E5A93C", "#3B5978", "#9E3A26", "#272E2B"),
            weightGrams = 420,
            icon = "jacket_shell",
            descriptionAr = "سترة خارجية متينة تحمي من الرياح والأمطار مع ياقة مرتفعة.",
            descriptionEn = "Seam-sealed mountain jacket built for wind and rain protection."
        ),
        EquipmentItem(
            id = "jacket-down-puffer",
            nameAr = "سترة مبطنة للعزل الحراري",
            nameEn = "Insulated Down Puffer Jacket",
            category = EquipmentCategory.SHIRTS_JACKETS,
            defaultSlot = LayerSlot.OUTERWEAR,
            colors = listOf("#E5A93C", "#2B3E52", "#2D543F", "#9E582A"),
            weightGrams = 360,
            icon = "jacket_puffer",
            descriptionAr = "سترة عازلة خفيفة الوزن وقابلة للطي للرحلات الباردة.",
            descriptionEn = "High-loft quilted thermal jacket for freezing alpine evenings."
        ),
        EquipmentItem(
            id = "jacket-fleece-vest",
            nameAr = "صديرية صوفية متعددة الجيوب",
            nameEn = "Trail Fleece Utility Vest",
            category = EquipmentCategory.SHIRTS_JACKETS,
            defaultSlot = LayerSlot.OUTERWEAR,
            colors = listOf("#5A4D41", "#3D6B52", "#7C6A56", "#272E2B"),
            weightGrams = 260,
            icon = "vest_utility",
            descriptionAr = "صديرية دافئة تمنح الذراعين حرية الحركة مع جيوب بسحاب.",
            descriptionEn = "Core-warming fleece vest with zippered organizer pockets."
        ),

        // 3. PANTS (3 items)
        EquipmentItem(
            id = "pants-cargo-trek",
            nameAr = "سروال رحلات بجيب جانبي",
            nameEn = "Reinforced Cargo Trekking Pants",
            category = EquipmentCategory.PANTS,
            defaultSlot = LayerSlot.PANTS,
            colors = listOf("#5A4D41", "#3D6B52", "#272E2B", "#7C6A56"),
            weightGrams = 340,
            icon = "pants_cargo",
            descriptionAr = "سروال متين بركب مقواة وجيوب جانبية واسعة لأدوات الملاحة.",
            descriptionEn = "Articulated knee trekking trousers with gusseted cargo pockets."
        ),
        EquipmentItem(
            id = "pants-convertible-zip",
            nameAr = "سروال مسارات خفيف قابل للتحويل",
            nameEn = "Lightweight Zip-Off Trail Pants",
            category = EquipmentCategory.PANTS,
            defaultSlot = LayerSlot.PANTS,
            colors = listOf("#7C6A56", "#434E49", "#3B5978", "#C4B9AC"),
            weightGrams = 275,
            icon = "pants_zip",
            descriptionAr = "سروال خفيف الوزن وسريع الجفاف مناسب للمناخ المتغير.",
            descriptionEn = "Stretch nylon trail pants ideal for river crossings and warm canyons."
        ),
        EquipmentItem(
            id = "pants-thermal-snow",
            nameAr = "سروال ثلجي معزول",
            nameEn = "Alpine Thermal Snow Trousers",
            category = EquipmentCategory.PANTS,
            defaultSlot = LayerSlot.PANTS,
            colors = listOf("#2B3E52", "#272E2B", "#2D543F", "#9E3A26"),
            weightGrams = 480,
            icon = "pants_snow",
            descriptionAr = "سروال مبطن مقاوم للبلل والثلوج مع حواف سفلية محكمة.",
            descriptionEn = "Weatherproof fleece-lined pants with reinforced scuff guards."
        ),

        // 4. BOOTS & OUTDOOR FOOTWEAR (3 items)
        EquipmentItem(
            id = "boots-leather-summit",
            nameAr = "حذاء جبلي جلدي عالي الكاحل",
            nameEn = "High-Ankle Leather Summit Boots",
            category = EquipmentCategory.FOOTWEAR,
            defaultSlot = LayerSlot.FOOTWEAR,
            colors = listOf("#5A4D41", "#3A2618", "#7C6A56", "#272E2B"),
            weightGrams = 920,
            icon = "boots_leather",
            descriptionAr = "حذاء جبلي متين يدعم الكاحل في المنحدرات الصخرية الوعرة.",
            descriptionEn = "Full-grain leather hiking boots with deep lug traction soles."
        ),
        EquipmentItem(
            id = "boots-trail-runner",
            nameAr = "حذاء مسارات خفيف وعالي الثبات",
            nameEn = "Ultralight Trail Approach Shoes",
            category = EquipmentCategory.FOOTWEAR,
            defaultSlot = LayerSlot.FOOTWEAR,
            colors = listOf("#3D6B52", "#E5A93C", "#3B5978", "#434E49"),
            weightGrams = 580,
            icon = "shoes_trail",
            descriptionAr = "حذاء خفيف مرن للمسافات الطويلة مع نعل مانع للانزلاق.",
            descriptionEn = "Cushioned low-cut trail shoes built for fast-packing comfort."
        ),
        EquipmentItem(
            id = "boots-arctic-insulated",
            nameAr = "حذاء شتوي معزول للثلوج",
            nameEn = "Insulated Winter Expedition Boots",
            category = EquipmentCategory.FOOTWEAR,
            defaultSlot = LayerSlot.FOOTWEAR,
            colors = listOf("#272E2B", "#2B3E52", "#7C6A56", "#9E3A26"),
            weightGrams = 1150,
            icon = "boots_winter",
            descriptionAr = "حذاء عالي العزل مخصص للسير فوق الثلوج والأراضي المتجمدة.",
            descriptionEn = "Waterproof thermal boots rated for sub-zero camp conditions."
        ),

        // 5. BACKPACKS (4 items)
        EquipmentItem(
            id = "pack-daypack-22l",
            nameAr = "حقيبة نزهة نهارية 22 لتر",
            nameEn = "22L Ridge Daypack",
            category = EquipmentCategory.BACKPACKS,
            defaultSlot = LayerSlot.BACKPACK,
            colors = listOf("#3D6B52", "#E5A93C", "#3B5978", "#5A4D41"),
            weightGrams = 540,
            icon = "pack_day",
            descriptionAr = "حقيبة ظهر مدمجة وخفيفة لنزهات اليوم الواحد مع أحزمة كتف مبطنة.",
            descriptionEn = "Compact daypack with breathable mesh back panel and sternum strap."
        ),
        EquipmentItem(
            id = "pack-expedition-65l",
            nameAr = "حقيبة بعثات كبرى 65 لتر",
            nameEn = "65L Multi-Day Expedition Pack",
            category = EquipmentCategory.BACKPACKS,
            defaultSlot = LayerSlot.BACKPACK,
            colors = listOf("#2D543F", "#9E582A", "#2B3E52", "#272E2B"),
            weightGrams = 1650,
            icon = "pack_expedition",
            descriptionAr = "حقيبة تخييم كبيرة بإطار داخلي داعم وحزام خصر لتوزيع الوزن.",
            descriptionEn = "Internal-frame trekking pack with top lid and sleeping bag compartment."
        ),
        EquipmentItem(
            id = "pack-rolltop-dry-35l",
            nameAr = "حقيبة مقاومة للماء 35 لتر",
            nameEn = "35L Roll-Top Waterproof Pack",
            category = EquipmentCategory.BACKPACKS,
            defaultSlot = LayerSlot.BACKPACK,
            colors = listOf("#E5A93C", "#3D6B52", "#3B5978", "#434E49"),
            weightGrams = 780,
            icon = "pack_rolltop",
            descriptionAr = "حقيبة محكمة الإغلاق تحمي المعدات من الأمطار الغزيرة وعبور الأنهار.",
            descriptionEn = "Taped-seam roll-top rucksack keeping gear dry in heavy downpours."
        ),
        EquipmentItem(
            id = "pack-canvas-heritage-40l",
            nameAr = "حقيبة قماشية كلاسيكية 40 لتر",
            nameEn = "40L Waxed Canvas Bushcraft Pack",
            category = EquipmentCategory.BACKPACKS,
            defaultSlot = LayerSlot.BACKPACK,
            colors = listOf("#7C6A56", "#5A4D41", "#2D543F", "#9E582A"),
            weightGrams = 1280,
            icon = "pack_canvas",
            descriptionAr = "حقيبة قماش مشمع متينة مع أحزمة جلدية وجيوب خارجية مزدوجة.",
            descriptionEn = "Durable waxed cotton rucksack with brass buckles and twin side pockets."
        ),

        // 6. WATER BOTTLES (3 items)
        EquipmentItem(
            id = "water-steel-flask-1l",
            nameAr = "قارورة فولاذية معزولة 1 لتر",
            nameEn = "1L Insulated Stainless Flask",
            category = EquipmentCategory.WATER_BOTTLES,
            defaultSlot = LayerSlot.LEFT_HAND_GEAR,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#3B5978", "#3D6B52", "#E5A93C", "#E8ECE9"),
            weightGrams = 380,
            icon = "bottle_flask",
            descriptionAr = "قارورة مزدوجة الجدار تحفظ برودة الماء لـ 24 ساعة أو سخونة المشروبات.",
            descriptionEn = "Vacuum-insulated steel bottle keeping water cold for 24 hours."
        ),
        EquipmentItem(
            id = "water-trail-canteen",
            nameAr = "زمزمية رحلات واسعة الفوهة",
            nameEn = "Wide-Mouth Trail Canteen",
            category = EquipmentCategory.WATER_BOTTLES,
            defaultSlot = LayerSlot.LEFT_HAND_GEAR,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#4E8768", "#7C6A56", "#5D7A99", "#9E582A"),
            weightGrams = 195,
            icon = "bottle_canteen",
            descriptionAr = "زمزمية متينة خالية من المواد الضارة مع تدريج لقياس كمية السوائل.",
            descriptionEn = "BPA-free graduated trail canteen with leakproof tether cap."
        ),
        EquipmentItem(
            id = "water-filter-squeeze",
            nameAr = "قارورة بفلتر تنقية مياه مدمج",
            nameEn = "Collapsible Filter Squeeze Bottle",
            category = EquipmentCategory.WATER_BOTTLES,
            defaultSlot = LayerSlot.LEFT_HAND_GEAR,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#5D7A99", "#3D6B52", "#E5A93C"),
            weightGrams = 140,
            icon = "bottle_filter",
            descriptionAr = "قارورة مرنة مزودة بمرشح دقيق لتنقية مياه الجداول الطبيعية.",
            descriptionEn = "Hollow-fiber membrane filter bottle for safe backcountry hydration."
        ),

        // 7. FOOD SUPPLIES (3 items)
        EquipmentItem(
            id = "food-trail-mix-pouch",
            nameAr = "حقيبة مكسرات وفواكه مجففة",
            nameEn = "High-Energy Trail Mix Pouch",
            category = EquipmentCategory.FOOD_SUPPLIES,
            defaultSlot = LayerSlot.BELT_POUCH,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#E5A93C", "#9E582A", "#4E8768"),
            weightGrams = 250,
            icon = "food_mix",
            descriptionAr = "مزيج غني بالطاقة من اللوز والتمر والزبيب للمسارات الطويلة.",
            descriptionEn = "Resealable ration pack of roasted nuts, dates, and dried berries."
        ),
        EquipmentItem(
            id = "food-freeze-dried-meal",
            nameAr = "وجبة تخييم مجففة بالتبريد",
            nameEn = "Freeze-Dried Camp Stew Pack",
            category = EquipmentCategory.FOOD_SUPPLIES,
            defaultSlot = LayerSlot.BELT_POUCH,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#9E582A", "#3D6B52", "#3B5978"),
            weightGrams = 180,
            icon = "food_meal",
            descriptionAr = "وجبة ساخنة خفيفة الوزن تحضر بإضافة الماء المغلي خلال دقائق.",
            descriptionEn = "Lightweight dehydrated lentil & rice meal pouch for camp dinners."
        ),
        EquipmentItem(
            id = "food-oat-energy-bars",
            nameAr = "ألواح شوفان وعسل للطاقة",
            nameEn = "Honey & Oat Trekking Bars",
            category = EquipmentCategory.FOOD_SUPPLIES,
            defaultSlot = LayerSlot.BELT_POUCH,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#C88422", "#7C6A56", "#3D6B52"),
            weightGrams = 160,
            icon = "food_bars",
            descriptionAr = "ثلاثة ألواح غذائية متوازنة سهلة الحمل في جيب الخصر.",
            descriptionEn = "Compact pack of three whole-grain oat and wildflower honey bars."
        ),

        // 8. FLASHLIGHTS & LANTERNS (3 items)
        EquipmentItem(
            id = "light-camp-lantern",
            nameAr = "فانوس تخييم LED قابل للشحن",
            nameEn = "Rechargeable Warm LED Camp Lantern",
            category = EquipmentCategory.FLASHLIGHTS,
            defaultSlot = LayerSlot.LEFT_HAND_GEAR,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#E5A93C", "#3D6B52", "#9E3A26", "#272E2B"),
            weightGrams = 290,
            icon = "lantern_camp",
            descriptionAr = "فانوس بإضاءة دافئة 360 درجة مع خطاف تعليق للخيمة.",
            descriptionEn = "360-degree dimmable camp lantern with folding bail handle."
        ),
        EquipmentItem(
            id = "light-aluminum-torch",
            nameAr = "مصباح يدوي ألمنيوم مقاوم للماء",
            nameEn = "Waterproof Aluminum Trail Torch",
            category = EquipmentCategory.FLASHLIGHTS,
            defaultSlot = LayerSlot.LEFT_HAND_GEAR,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#272E2B", "#4E8768", "#E5A93C"),
            weightGrams = 165,
            icon = "flashlight_torch",
            descriptionAr = "مصباح يدوي بمدى رؤية بعيد وهيكل معدني متين.",
            descriptionEn = "Anodized aluminum handheld flashlight with IPX7 water resistance."
        ),
        EquipmentItem(
            id = "light-headlamp-strap",
            nameAr = "مصباح رأس ليلي متعدد الدرجات",
            nameEn = "Dual-Beam Night Trail Headlamp",
            category = EquipmentCategory.FLASHLIGHTS,
            defaultSlot = LayerSlot.NECK_CHEST_GEAR,
            compatibleSlots = flexibleAccessorySlots + LayerSlot.HEADWEAR,
            colors = listOf("#E5A93C", "#3B5978", "#3D6B52"),
            weightGrams = 95,
            icon = "headlamp",
            descriptionAr = "مصباح خفيف يترك اليدين حرتين مع وضع إضاءة خافتة للقراءة.",
            descriptionEn = "Hands-free LED lamp with flood beam and night-vision red mode."
        ),

        // 9. MAPS & COMPASSES (3 items)
        EquipmentItem(
            id = "nav-brass-compass",
            nameAr = "بوصلة ميدانية نحاسية دقيقة",
            nameEn = "Precision Liquid-Filled Compass",
            category = EquipmentCategory.MAPS_COMPASSES,
            defaultSlot = LayerSlot.NECK_CHEST_GEAR,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#E5A93C", "#7C6A56", "#3D6B52"),
            weightGrams = 85,
            icon = "compass_brass",
            descriptionAr = "بوصلة مغناطيسية بحلقة دوارة مضيئة لتحديد الاتجاهات دون بطارية.",
            descriptionEn = "Luminous bezel sighting compass with lanyard cord."
        ),
        EquipmentItem(
            id = "nav-topo-map-case",
            nameAr = "خريطة طبوغرافية في حافظة شفافة",
            nameEn = "Waterproof Topographic Map Roll",
            category = EquipmentCategory.MAPS_COMPASSES,
            defaultSlot = LayerSlot.RIGHT_HAND_GEAR,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#C4B9AC", "#E5A93C", "#5D7A99"),
            weightGrams = 120,
            icon = "map_topo",
            descriptionAr = "خريطة تضاريس مفصلة محفوظة داخل غلاف مقاوم للماء والغبار.",
            descriptionEn = "Contour trail map sealed inside a weather-resistant document sleeve."
        ),
        EquipmentItem(
            id = "nav-altimeter-watch",
            nameAr = "مقياس ارتفاع وضغط جوي",
            nameEn = "Barometric Altimeter & Clinometer",
            category = EquipmentCategory.MAPS_COMPASSES,
            defaultSlot = LayerSlot.NECK_CHEST_GEAR,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#3B5978", "#272E2B", "#E5A93C"),
            weightGrams = 70,
            icon = "altimeter",
            descriptionAr = "جهاز جيب لقياس الارتفاع عن سطح البحر ومراقبة تغيرات الطقس.",
            descriptionEn = "Pocket barometer and elevation gauge for tracking weather shifts."
        ),

        // 10. FIRST-AID KITS (3 items)
        EquipmentItem(
            id = "aid-field-pouch-red",
            nameAr = "حقيبة إسعافات أولية ميدانية",
            nameEn = "Trail First-Aid Medical Pouch",
            category = EquipmentCategory.FIRST_AID,
            defaultSlot = LayerSlot.BELT_POUCH,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#D9534F", "#E5A93C", "#3D6B52"),
            weightGrams = 310,
            icon = "first_aid_kit",
            descriptionAr = "تضم ضمادات معقمة، مطهر، شريطاً طبياً، وملقطاً في حقيبة منظمة.",
            descriptionEn = "Water-resistant pouch stocked with sterile dressings, tape, and antiseptic."
        ),
        EquipmentItem(
            id = "aid-blister-care-tin",
            nameAr = "علبة عناية بالقدمين والضمادات",
            nameEn = "Blister & Foot Care Pocket Tin",
            category = EquipmentCategory.FIRST_AID,
            defaultSlot = LayerSlot.BELT_POUCH,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#E8ECE9", "#D9534F", "#3B5978"),
            weightGrams = 95,
            icon = "first_aid_tin",
            descriptionAr = "لصقات طبية مبطنة لحماية القدمين أثناء المشي لمسافات طويلة.",
            descriptionEn = "Compact aluminum tin with hydrocolloid pads and elastic wraps."
        ),
        EquipmentItem(
            id = "aid-thermal-blanket-pack",
            nameAr = "بطانية طوارئ حرارية عاكسة",
            nameEn = "Reflective Thermal Space Blanket",
            category = EquipmentCategory.FIRST_AID,
            defaultSlot = LayerSlot.BELT_POUCH,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#E5A93C", "#C4B9AC", "#D9534F"),
            weightGrams = 65,
            icon = "thermal_blanket",
            descriptionAr = "بطانية خفيفة جداً تعكس حرارة الجسم للوقاية من البرد المفاجئ.",
            descriptionEn = "Ultralight aluminized mylar sheet retaining body heat in emergencies."
        ),

        // 11. TENTS & SLEEPING BAGS (3 items)
        EquipmentItem(
            id = "camp-ultralight-tent-roll",
            nameAr = "خيمة قبة خفيفة مطوية",
            nameEn = "2-Person Dome Tent Roll",
            category = EquipmentCategory.TENTS_SLEEPING,
            defaultSlot = LayerSlot.CAMP_GEAR,
            colors = listOf("#4E8768", "#E5A93C", "#3B5978", "#9E582A"),
            weightGrams = 1450,
            icon = "tent_roll",
            descriptionAr = "خيمة مقاومة للرياح والأمطار تثبت أسفل أو خلف حقيبة الظهر.",
            descriptionEn = "Packable aluminum-pole dome tent secured in a compression sack."
        ),
        EquipmentItem(
            id = "camp-mummy-sleeping-bag",
            nameAr = "كيس نوم حراري مدمج",
            nameEn = "3-Season Mummy Sleeping Bag",
            category = EquipmentCategory.TENTS_SLEEPING,
            defaultSlot = LayerSlot.CAMP_GEAR,
            colors = listOf("#3B5978", "#2D543F", "#9E3A26", "#E5A93C"),
            weightGrams = 980,
            icon = "sleeping_bag",
            descriptionAr = "كيس نوم دافئ ومضغوط يوفر عزلاً ممتازاً في ليالي التخييم.",
            descriptionEn = "Compressible insulated sleeping bag with draft collar."
        ),
        EquipmentItem(
            id = "camp-foam-sleeping-pad",
            nameAr = "فرشة أرضية عازلة قابلة للطي",
            nameEn = "Closed-Cell Foam Sleeping Pad",
            category = EquipmentCategory.TENTS_SLEEPING,
            defaultSlot = LayerSlot.CAMP_GEAR,
            colors = listOf("#E5A93C", "#3D6B52", "#5A4D41"),
            weightGrams = 390,
            icon = "sleeping_pad",
            descriptionAr = "وسادة عزل أرضية تحمي من برودة التربة والصخور تحت كيس النوم.",
            descriptionEn = "Accordion-fold thermal ground mat strapping cleanly to the pack."
        ),

        // 12. WEATHER PROTECTION (3 items)
        EquipmentItem(
            id = "weather-rain-poncho",
            nameAr = "معطف مطر واقٍ واسع",
            nameEn = "Pack-Cover Rain Poncho",
            category = EquipmentCategory.WEATHER_PROTECTION,
            defaultSlot = LayerSlot.WEATHER_GEAR,
            colors = listOf("#E5A93C", "#3D6B52", "#3B5978", "#9E3A26"),
            weightGrams = 260,
            icon = "poncho_rain",
            descriptionAr = "رداء مطري خفيف يغطي المستكشف وحقيبة الظهر أثناء الهطول الغزير.",
            descriptionEn = "Hooded waterproof tarp-poncho shielding both hiker and backpack."
        ),
        EquipmentItem(
            id = "weather-neck-gaiter",
            nameAr = "طوق رقبة واقٍ من الرياح",
            nameEn = "Fleece Windproof Neck Gaiter",
            category = EquipmentCategory.WEATHER_PROTECTION,
            defaultSlot = LayerSlot.WEATHER_GEAR,
            colors = listOf("#2D543F", "#9E3A26", "#E5A93C", "#272E2B"),
            weightGrams = 60,
            icon = "neck_gaiter",
            descriptionAr = "طوق مرن يحمي العنق والوجه من الهواء البارد والغبار.",
            descriptionEn = "Soft thermal neck warmer preventing windchill on exposed trails."
        ),
        EquipmentItem(
            id = "weather-glacier-goggles",
            nameAr = "نظارة شمسية جبلية مستقطبة",
            nameEn = "Polarized Glacier Sun Glasses",
            category = EquipmentCategory.WEATHER_PROTECTION,
            defaultSlot = LayerSlot.NECK_CHEST_GEAR,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#272E2B", "#E5A93C", "#3B5978"),
            weightGrams = 45,
            icon = "glasses_glacier",
            descriptionAr = "نظارة بحواجز جانبية لحماية العينين من وهج الثلج والشمس.",
            descriptionEn = "Side-shielded UV400 mountaineering eyewear reducing snow glare."
        ),

        // 13. HIKING ACCESSORIES (3 items)
        EquipmentItem(
            id = "acc-trekking-poles",
            nameAr = "عصا مشي جبلية تلسكوبية",
            nameEn = "Carbon Cork Trekking Pole",
            category = EquipmentCategory.HIKING_ACCESSORIES,
            defaultSlot = LayerSlot.RIGHT_HAND_GEAR,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#E5A93C", "#3D6B52", "#3B5978", "#C4B9AC"),
            weightGrams = 220,
            icon = "trekking_pole",
            descriptionAr = "عصا توازن بمقبض فلين مريح لتخفيف الضغط على الركبتين في الصعود والهبوط.",
            descriptionEn = "Adjustable telescoping pole with ergonomic cork grip and wrist strap."
        ),
        EquipmentItem(
            id = "acc-compact-binoculars",
            nameAr = "منظار ميداني مدمج لمراقبة الطبيعة",
            nameEn = "8x42 Waterproof Trail Binoculars",
            category = EquipmentCategory.HIKING_ACCESSORIES,
            defaultSlot = LayerSlot.NECK_CHEST_GEAR,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#272E2B", "#3D6B52", "#7C6A56"),
            weightGrams = 410,
            icon = "binoculars",
            descriptionAr = "منظار عالي الوضوح لمراقبة الطيور والمعالم البعيدة في المسار.",
            descriptionEn = "Fog-proof prism binoculars for wildlife spotting and route scouting."
        ),
        EquipmentItem(
            id = "acc-safety-whistle-carabiner",
            nameAr = "صافرة تنبيه وحلقة تعليق معدنية",
            nameEn = "Pealess Safety Whistle & Carabiner",
            category = EquipmentCategory.HIKING_ACCESSORIES,
            defaultSlot = LayerSlot.NECK_CHEST_GEAR,
            compatibleSlots = flexibleAccessorySlots,
            colors = listOf("#E5A93C", "#D9534F", "#3D6B52"),
            weightGrams = 35,
            icon = "whistle_carabiner",
            descriptionAr = "صافرة إشارة عالية الصوت تعمل في جميع الظروف الجوية.",
            descriptionEn = "115dB acoustic signaling whistle clipped to an accessory carabiner."
        ),

        // 14. DECORATIVE PATCHES & COLOR VARIATIONS (3 items)
        EquipmentItem(
            id = "patch-pine-crest",
            nameAr = "شارة قمة الصنوبر المطرزة",
            nameEn = "Embroidered Pine Crest Badge",
            category = EquipmentCategory.PATCHES_DECALS,
            defaultSlot = LayerSlot.DECORATIVE_PATCH,
            colors = listOf("#4E8768", "#E5A93C", "#3B5978", "#9E3A26"),
            weightGrams = 15,
            icon = "patch_pine",
            descriptionAr = "شارة قماشية مطرزة ترمز لعشاق المسارات الغابية.",
            descriptionEn = "Woven merit patch celebrating woodland trail stewardship."
        ),
        EquipmentItem(
            id = "patch-compass-star",
            nameAr = "وسام نجمة البوصلة الذهبية",
            nameEn = "Golden Compass Rose Emblem",
            category = EquipmentCategory.PATCHES_DECALS,
            defaultSlot = LayerSlot.DECORATIVE_PATCH,
            colors = listOf("#E5A93C", "#9E582A", "#3D6B52", "#E8ECE9"),
            weightGrams = 15,
            icon = "patch_compass",
            descriptionAr = "وسام تزييني يثبت على الصدر أو الحقيبة للمستكشفين.",
            descriptionEn = "Classic stitched navigation rose emblem for jackets and packs."
        ),
        EquipmentItem(
            id = "patch-desert-sun",
            nameAr = "شارة شمس الكثبان الصحراوية",
            nameEn = "Dune Horizon Explorer Patch",
            category = EquipmentCategory.PATCHES_DECALS,
            defaultSlot = LayerSlot.DECORATIVE_PATCH,
            colors = listOf("#9E582A", "#E5A93C", "#3B5978", "#3D6B52"),
            weightGrams = 15,
            icon = "patch_dune",
            descriptionAr = "شارة مستوحاة من ألوان الغروب فوق الكثبان والوديان الصخرية.",
            descriptionEn = "Warm terracotta & amber horizon badge for canyon trekkers."
        )
    )

    private val itemsById: Map<String, EquipmentItem> = items.associateBy { it.id }

    fun findById(id: String): EquipmentItem? = itemsById[id]

    /**
     * 4 original character presets with distinct colors, clothing, and gear loadouts.
     */
    val presets: List<CharacterPreset> = listOf(
        CharacterPreset(
            id = "preset-pine-ranger",
            nameAr = "حارس الغابة",
            nameEn = "Forest Ranger",
            config = CharacterConfig(
                skinColorHex = "#E0B187",
                hairStyle = 0,
                hairColorHex = "#3B271A",
                scene = SceneBackground.PINE_VALLEY,
                equippedLayers = mapOf(
                    LayerSlot.UPPER_BODY to EquippedLayer(LayerSlot.UPPER_BODY, "shirt-merino-base", "#3D6B52"),
                    LayerSlot.OUTERWEAR to EquippedLayer(LayerSlot.OUTERWEAR, "jacket-fleece-vest", "#5A4D41"),
                    LayerSlot.PANTS to EquippedLayer(LayerSlot.PANTS, "pants-cargo-trek", "#5A4D41"),
                    LayerSlot.FOOTWEAR to EquippedLayer(LayerSlot.FOOTWEAR, "boots-leather-summit", "#5A4D41"),
                    LayerSlot.HEADWEAR to EquippedLayer(LayerSlot.HEADWEAR, "head-sun-brim-hat", "#7C6A56"),
                    LayerSlot.BACKPACK to EquippedLayer(LayerSlot.BACKPACK, "pack-canvas-heritage-40l", "#7C6A56"),
                    LayerSlot.NECK_CHEST_GEAR to EquippedLayer(LayerSlot.NECK_CHEST_GEAR, "nav-brass-compass", "#E5A93C"),
                    LayerSlot.RIGHT_HAND_GEAR to EquippedLayer(LayerSlot.RIGHT_HAND_GEAR, "acc-trekking-poles", "#E5A93C"),
                    LayerSlot.DECORATIVE_PATCH to EquippedLayer(LayerSlot.DECORATIVE_PATCH, "patch-pine-crest", "#4E8768")
                )
            )
        ),
        CharacterPreset(
            id = "preset-desert-nomad",
            nameAr = "رحالة الصحراء",
            nameEn = "Desert Canyon Explorer",
            config = CharacterConfig(
                skinColorHex = "#C68B59",
                hairStyle = 1,
                hairColorHex = "#1E1916",
                scene = SceneBackground.DESERT_CANYON,
                equippedLayers = mapOf(
                    LayerSlot.UPPER_BODY to EquippedLayer(LayerSlot.UPPER_BODY, "shirt-safari-flannel", "#7C6A56"),
                    LayerSlot.PANTS to EquippedLayer(LayerSlot.PANTS, "pants-convertible-zip", "#C4B9AC"),
                    LayerSlot.FOOTWEAR to EquippedLayer(LayerSlot.FOOTWEAR, "boots-trail-runner", "#E5A93C"),
                    LayerSlot.HEADWEAR to EquippedLayer(LayerSlot.HEADWEAR, "head-desert-keffiyeh-wrap", "#C4B9AC"),
                    LayerSlot.BACKPACK to EquippedLayer(LayerSlot.BACKPACK, "pack-daypack-22l", "#5A4D41"),
                    LayerSlot.LEFT_HAND_GEAR to EquippedLayer(LayerSlot.LEFT_HAND_GEAR, "water-steel-flask-1l", "#E5A93C"),
                    LayerSlot.RIGHT_HAND_GEAR to EquippedLayer(LayerSlot.RIGHT_HAND_GEAR, "nav-topo-map-case", "#C4B9AC"),
                    LayerSlot.DECORATIVE_PATCH to EquippedLayer(LayerSlot.DECORATIVE_PATCH, "patch-desert-sun", "#9E582A")
                )
            )
        ),
        CharacterPreset(
            id = "preset-alpine-mountaineer",
            nameAr = "مستكشف القمم الجليدية",
            nameEn = "Alpine Peak Mountaineer",
            config = CharacterConfig(
                skinColorHex = "#F5D0B5",
                hairStyle = 2,
                hairColorHex = "#6E4427",
                scene = SceneBackground.ALPINE_PEAK,
                equippedLayers = mapOf(
                    LayerSlot.UPPER_BODY to EquippedLayer(LayerSlot.UPPER_BODY, "shirt-merino-base", "#3B5978"),
                    LayerSlot.OUTERWEAR to EquippedLayer(LayerSlot.OUTERWEAR, "jacket-down-puffer", "#E5A93C"),
                    LayerSlot.WEATHER_GEAR to EquippedLayer(LayerSlot.WEATHER_GEAR, "weather-neck-gaiter", "#2D543F"),
                    LayerSlot.PANTS to EquippedLayer(LayerSlot.PANTS, "pants-thermal-snow", "#2B3E52"),
                    LayerSlot.FOOTWEAR to EquippedLayer(LayerSlot.FOOTWEAR, "boots-arctic-insulated", "#272E2B"),
                    LayerSlot.HEADWEAR to EquippedLayer(LayerSlot.HEADWEAR, "head-merino-beanie", "#9E3A26"),
                    LayerSlot.BACKPACK to EquippedLayer(LayerSlot.BACKPACK, "pack-expedition-65l", "#2B3E52"),
                    LayerSlot.CAMP_GEAR to EquippedLayer(LayerSlot.CAMP_GEAR, "camp-mummy-sleeping-bag", "#3B5978"),
                    LayerSlot.BELT_POUCH to EquippedLayer(LayerSlot.BELT_POUCH, "aid-field-pouch-red", "#D9534F")
                )
            )
        ),
        CharacterPreset(
            id = "preset-night-camper",
            nameAr = "مخيم النجوم الليلي",
            nameEn = "Starry Basecamp Hiker",
            config = CharacterConfig(
                skinColorHex = "#9C643B",
                hairStyle = 3,
                hairColorHex = "#1E1916",
                scene = SceneBackground.STARRY_CAMP,
                equippedLayers = mapOf(
                    LayerSlot.UPPER_BODY to EquippedLayer(LayerSlot.UPPER_BODY, "shirt-merino-base", "#272E2B"),
                    LayerSlot.OUTERWEAR to EquippedLayer(LayerSlot.OUTERWEAR, "jacket-alpine-shell", "#3D6B52"),
                    LayerSlot.PANTS to EquippedLayer(LayerSlot.PANTS, "pants-cargo-trek", "#272E2B"),
                    LayerSlot.FOOTWEAR to EquippedLayer(LayerSlot.FOOTWEAR, "boots-leather-summit", "#3A2618"),
                    LayerSlot.HEADWEAR to EquippedLayer(LayerSlot.HEADWEAR, "head-trail-cap", "#E5A93C"),
                    LayerSlot.BACKPACK to EquippedLayer(LayerSlot.BACKPACK, "pack-rolltop-dry-35l", "#3D6B52"),
                    LayerSlot.CAMP_GEAR to EquippedLayer(LayerSlot.CAMP_GEAR, "camp-ultralight-tent-roll", "#4E8768"),
                    LayerSlot.LEFT_HAND_GEAR to EquippedLayer(LayerSlot.LEFT_HAND_GEAR, "light-camp-lantern", "#E5A93C"),
                    LayerSlot.DECORATIVE_PATCH to EquippedLayer(LayerSlot.DECORATIVE_PATCH, "patch-compass-star", "#E5A93C")
                )
            )
        )
    )

    fun defaultCharacterConfig(): CharacterConfig = presets.first().config

    fun defaultChecklistFor(tripType: TripType): List<ChecklistEntry> = when (tripType) {
        TripType.DAY_HIKE -> listOf(
            ChecklistEntry("chk-dh-1", TripType.DAY_HIKE, "قارورة مياه 1.5 لتر على الأقل", "At least 1.5L Water Bottle", 380, true),
            ChecklistEntry("chk-dh-2", TripType.DAY_HIKE, "قبعة شمسية ونظارة واقية", "Sun Hat & UV Glasses", 140, true),
            ChecklistEntry("chk-dh-3", TripType.DAY_HIKE, "وجبات خفيفة عالية الطاقة (مكسرات/شوفان)", "High-Energy Trail Snacks", 250, false),
            ChecklistEntry("chk-dh-4", TripType.DAY_HIKE, "حقيبة إسعافات أولية شخصية", "Personal First-Aid Pouch", 310, false),
            ChecklistEntry("chk-dh-5", TripType.DAY_HIKE, "خريطة المسار وبوصلة ميدانية", "Trail Map & Compass", 180, false),
            ChecklistEntry("chk-dh-6", TripType.DAY_HIKE, "مصباح يدوي أو مصباح رأس احتياطي", "Backup Headlamp or Flashlight", 95, false),
            ChecklistEntry("chk-dh-7", TripType.DAY_HIKE, "معطف مطر خفيف للطوارئ", "Lightweight Rain Shell / Poncho", 260, false)
        )
        TripType.CAMPING -> listOf(
            ChecklistEntry("chk-cp-1", TripType.CAMPING, "خيمة مقاومة للرياح مع أوتادها", "Weatherproof Tent & Stakes", 1450, true),
            ChecklistEntry("chk-cp-2", TripType.CAMPING, "كيس نوم مناسب لدرجة الحرارة", "3-Season Sleeping Bag", 980, true),
            ChecklistEntry("chk-cp-3", TripType.CAMPING, "فرشة أرضية عازلة للرطوبة", "Insulated Sleeping Pad", 390, false),
            ChecklistEntry("chk-cp-4", TripType.CAMPING, "فانوس تخييم LED وبطارية إضافية", "Camp LED Lantern & Power Bank", 340, false),
            ChecklistEntry("chk-cp-5", TripType.CAMPING, "إمدادات مياه وفلتر تنقية", "Water Supply & Filtration Bottle", 520, false),
            ChecklistEntry("chk-cp-6", TripType.CAMPING, "وجبات تخييم مجففة وأواني طبخ", "Camp Meals & Cookware Kit", 480, false),
            ChecklistEntry("chk-cp-7", TripType.CAMPING, "أكياس لحفظ النفايات وحماية البيئة", "Leave-No-Trace Waste Bags", 60, false)
        )
        TripType.COLD_WEATHER -> listOf(
            ChecklistEntry("chk-cw-1", TripType.COLD_WEATHER, "طبقة أساسية حرارية (صوف ميرينو)", "Merino Thermal Base Layer", 190, true),
            ChecklistEntry("chk-cw-2", TripType.COLD_WEATHER, "سترة عازلة مبطنة ومقاومة للرياح", "Insulated Down Puffer & Wind Shell", 620, true),
            ChecklistEntry("chk-cw-3", TripType.COLD_WEATHER, "قبعة صوفية وطوق رقبة وقفازات", "Warm Beanie, Neck Gaiter & Gloves", 180, false),
            ChecklistEntry("chk-cw-4", TripType.COLD_WEATHER, "حذاء شتوي معزول وجوارب صوفية إضافية", "Insulated Boots & Extra Wool Socks", 1150, false),
            ChecklistEntry("chk-cw-5", TripType.COLD_WEATHER, "بطانية طوارئ حرارية عاكسة", "Reflective Thermal Space Blanket", 65, false),
            ChecklistEntry("chk-cw-6", TripType.COLD_WEATHER, "حافظة مشروبات ساخنة معزولة", "Insulated Hot Beverage Flask", 380, false)
        )
        TripType.EMERGENCY_BAG -> listOf(
            ChecklistEntry("chk-em-1", TripType.EMERGENCY_BAG, "حقيبة إسعافات أولية شاملة وأدوية أساسية", "Comprehensive First-Aid Kit", 420, true),
            ChecklistEntry("chk-em-2", TripType.EMERGENCY_BAG, "مياه شرب معقمة وأقراص تنقية", "Sealed Drinking Water & Filter", 1000, true),
            ChecklistEntry("chk-em-3", TripType.EMERGENCY_BAG, "أغذية طويلة الصلاحية جاهزة للأكل", "Non-Perishable Energy Rations", 500, false),
            ChecklistEntry("chk-em-4", TripType.EMERGENCY_BAG, "مصباح يدوي وصافرة تنبيه عالية الصوت", "Flashlight & Safety Signal Whistle", 200, false),
            ChecklistEntry("chk-em-5", TripType.EMERGENCY_BAG, "بطانية حرارية ومعطف واقٍ من المطر", "Thermal Blanket & Rain Poncho", 325, false),
            ChecklistEntry("chk-em-6", TripType.EMERGENCY_BAG, "نسخة ورقية من الخرائط وأرقام الطوارئ", "Waterproof Map & Emergency Contacts", 110, false)
        )
    }

    fun allDefaultChecklists(): List<ChecklistEntry> =
        TripType.entries.flatMap { defaultChecklistFor(it) }
}
