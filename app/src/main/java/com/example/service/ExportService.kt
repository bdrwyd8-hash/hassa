package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.example.data.EquipmentCatalog
import com.example.model.AppLanguage
import com.example.model.CharacterConfig
import com.example.model.EquippedLayer
import com.example.model.LayerSlot
import com.example.model.SavedCharacter
import com.example.model.SceneBackground
import com.example.ui.canvas.CharacterBitmapRenderer
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

sealed class ImportValidationResult {
    data class Success(val character: SavedCharacter) : ImportValidationResult()
    data class Error(val messageAr: String, val messageEn: String) : ImportValidationResult() {
        fun message(lang: AppLanguage): String = if (lang == AppLanguage.AR) messageAr else messageEn
    }
}

sealed class PngExportResult {
    data class Success(val bitmap: Bitmap, val pngBytes: ByteArray, val cacheFile: File?) : PngExportResult()
    data class Error(val messageAr: String, val messageEn: String) : PngExportResult() {
        fun message(lang: AppLanguage): String = if (lang == AppLanguage.AR) messageAr else messageEn
    }
}

object ExportService {

    private const val MAX_JSON_CHARS = 200_000
    private const val EXPORT_APP_ID = "TrailKit Studio"
    private const val EXPORT_SCHEMA_VERSION = 1

    /**
     * Renders the actual 2D character, selected scene (or neutral export background),
     * and all visible equipped layers in deterministic zOrder onto a real Bitmap and encodes PNG bytes.
     */
    fun exportCharacterPng(
        context: Context?,
        characterName: String,
        config: CharacterConfig,
        useNeutralBackground: Boolean = false
    ): PngExportResult {
        return try {
            val effectiveConfig = if (useNeutralBackground) {
                config.copy(scene = SceneBackground.NEUTRAL_STUDIO)
            } else {
                config
            }
            val width = 800
            val height = 1000
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(CharacterBitmapRenderer.parseColorSafe(effectiveConfig.scene.skyTopHex))
            val canvas = Canvas(bitmap)

            CharacterBitmapRenderer.drawCharacterScene(
                canvas = canvas,
                width = width.toFloat(),
                height = height.toFloat(),
                config = effectiveConfig,
                characterName = characterName
            )

            val bos = ByteArrayOutputStream()
            val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
            val bytes = bos.toByteArray()
            if (!ok || bytes.isEmpty()) {
                return PngExportResult.Error(
                    "فشل ضغط صورة PNG",
                    "Failed to encode PNG image bytes"
                )
            }

            var savedFile: File? = null
            if (context != null) {
                try {
                    val safeName = characterName.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_").ifBlank { "explorer" }
                    val file = File(context.cacheDir, "trailkit_${safeName}.png")
                    FileOutputStream(file).use { it.write(bytes) }
                    savedFile = file
                } catch (_: Exception) {
                    // In-memory bytes & bitmap still valid even if cache write is restricted
                }
            }

            PngExportResult.Success(bitmap = bitmap, pngBytes = bytes, cacheFile = savedFile)
        } catch (e: Exception) {
            PngExportResult.Error(
                "حدث خطأ أثناء تصدير صورة PNG: ${e.localizedMessage ?: ""}",
                "Error while rendering PNG export: ${e.localizedMessage ?: ""}"
            )
        }
    }

    /**
     * Produces a clean, versioned JSON backup string for a character.
     */
    fun exportCharacterJson(characterName: String, config: CharacterConfig): String {
        val root = JSONObject()
        root.put("app", EXPORT_APP_ID)
        root.put("schemaVersion", EXPORT_SCHEMA_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val charObj = JSONObject()
        charObj.put("name", characterName.trim().ifBlank { "Trail Explorer" })
        charObj.put("config", StorageService.configToJson(config))
        root.put("character", charObj)

        return root.toString(2)
    }

    /**
     * Strictly validates imported JSON before anything is written to storage.
     * Rejects:
     * - Empty or excessively large input (> 200KB)
     * - Malformed JSON syntax
     * - Unknown or incompatible schemaVersion
     * - Missing required `character`, `name`, or `config` objects
     * - Invalid or unknown equipment item IDs or invalid layer slots
     * Never overwrites existing characters silently (generates a unique ID and safe name).
     */
    fun validateAndParseCharacterJson(
        rawJson: String,
        existingCharacters: List<SavedCharacter>
    ): ImportValidationResult {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return ImportValidationResult.Error(
                "بيانات JSON فارغة. يرجى لصق محتوى صالح أو اختيار ملف.",
                "JSON input is empty. Please paste valid JSON or choose a file."
            )
        }
        if (trimmed.length > MAX_JSON_CHARS) {
            return ImportValidationResult.Error(
                "حجم ملف JSON كبير جداً ويتجاوز الحد الآمن المسموح به.",
                "JSON file is excessively large and exceeds the safe size limit."
            )
        }

        val root = try {
            JSONObject(trimmed)
        } catch (e: Exception) {
            return ImportValidationResult.Error(
                "صيغة JSON غير صالحة أو تالفة.",
                "Malformed JSON syntax."
            )
        }

        if (!root.has("schemaVersion")) {
            return ImportValidationResult.Error(
                "حقل إصدار المخطط (schemaVersion) مفقود.",
                "Missing required 'schemaVersion' field."
            )
        }
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion != EXPORT_SCHEMA_VERSION) {
            return ImportValidationResult.Error(
                "إصدار ملف البيانات غير متوافق ($schemaVersion). الإصدار المدعوم هو $EXPORT_SCHEMA_VERSION.",
                "Incompatible schemaVersion ($schemaVersion). Supported version is $EXPORT_SCHEMA_VERSION."
            )
        }

        val charObj = root.optJSONObject("character")
            ?: return ImportValidationResult.Error(
                "كائن بيانات الشخصية (character) مفقود في الملف.",
                "Missing required 'character' object in JSON."
            )

        val rawName = charObj.optString("name", "").trim()
        if (rawName.isEmpty() || rawName.length > 60) {
            return ImportValidationResult.Error(
                "اسم الشخصية مفقود أو يتجاوز الطول المسموح به.",
                "Character name is missing or exceeds maximum length."
            )
        }

        val configObj = charObj.optJSONObject("config")
            ?: return ImportValidationResult.Error(
                "إعدادات مظهر الشخصية (config) مفقودة.",
                "Missing required character 'config' object."
            )

        // Validate scene if provided
        val sceneId = configObj.optString("scene", SceneBackground.PINE_VALLEY.id)
        val scene = SceneBackground.entries.find { it.id == sceneId }
            ?: return ImportValidationResult.Error(
                "مرجع المشهد الخلفي غير صالح: $sceneId",
                "Invalid scene reference: $sceneId"
            )

        // Strictly validate every equipped layer reference
        val layersArr = configObj.optJSONArray("equippedLayers") ?: JSONArray()
        if (layersArr.length() > LayerSlot.entries.size) {
            return ImportValidationResult.Error(
                "عدد الطبقات المجهزة في الملف يتجاوز الحد المسموح.",
                "Too many equipped layers in imported JSON."
            )
        }

        val validatedLayers = mutableMapOf<LayerSlot, EquippedLayer>()
        for (i in 0 until layersArr.length()) {
            val lObj = layersArr.optJSONObject(i)
                ?: return ImportValidationResult.Error(
                    "بيانات طبقة غير صالحة عند الفهرس $i.",
                    "Malformed layer entry at index $i."
                )
            val slotId = lObj.optString("slot", "")
            val itemId = lObj.optString("itemId", "")
            val slot = LayerSlot.entries.find { it.id == slotId }
                ?: return ImportValidationResult.Error(
                    "موضع طبقة غير معروف: $slotId",
                    "Unknown layer slot: $slotId"
                )
            val item = EquipmentCatalog.findById(itemId)
                ?: return ImportValidationResult.Error(
                    "معرّف القطعة غير موجود في مكتبة المعدات: $itemId",
                    "Invalid item reference not found in Equipment Library: $itemId"
                )
            val colorHex = lObj.optString("colorHex", item.colors.first())
            if (!Regex("^#[0-9A-Fa-f]{6}$").matches(colorHex.trim())) {
                return ImportValidationResult.Error(
                    "كود اللون غير صالح للقطعة $itemId: $colorHex",
                    "Invalid hex color format for item $itemId: $colorHex"
                )
            }
            validatedLayers[slot] = EquippedLayer(
                slot = slot,
                itemId = item.id,
                colorHex = colorHex.trim().uppercase(),
                visible = lObj.optBoolean("visible", true),
                zOrder = lObj.optInt("zOrder", slot.defaultZOrder).coerceIn(1, 200)
            )
        }

        val skinHex = configObj.optString("skinColorHex", "#E0B187").trim()
        val hairColorHex = configObj.optString("hairColorHex", "#3B271A").trim()
        if (!Regex("^#[0-9A-Fa-f]{6}$").matches(skinHex) || !Regex("^#[0-9A-Fa-f]{6}$").matches(hairColorHex)) {
            return ImportValidationResult.Error(
                "ألوان الشخصية الأساسية غير صالحة.",
                "Invalid base character hex colors."
            )
        }

        val parsedConfig = CharacterConfig(
            skinColorHex = skinHex.uppercase(),
            hairStyle = configObj.optInt("hairStyle", 0).coerceIn(0, 3),
            hairColorHex = hairColorHex.uppercase(),
            scene = scene,
            equippedLayers = validatedLayers
        )

        // Ensure existing characters are never overwritten silently
        val existingNames = existingCharacters.map { it.name.lowercase() }.toSet()
        var safeName = rawName
        var suffix = 1
        while (safeName.lowercase() in existingNames) {
            suffix++
            safeName = "$rawName ($suffix)"
        }

        val newCharacter = SavedCharacter(
            id = "char-import-${System.currentTimeMillis()}-${(100..999).random()}",
            name = safeName,
            updatedAt = System.currentTimeMillis(),
            config = parsedConfig
        )

        return ImportValidationResult.Success(newCharacter)
    }
}
