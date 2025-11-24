package com.wellycorp.demolottie

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Helper object để quản lý các style khóa kéo khác nhau
 * Hỗ trợ 2 loại: Row (đường khóa kéo) và Zipper (con kéo)
 */
object LottieImageReplacer {
    private const val TAG = "LottieImageReplacer"
    private const val MODIFIED_ROW_JSON_FILE = "row_modified.json"
    private const val MODIFIED_ZIPPER_JSON_FILE = "zipper_modified.json"
    private const val MODIFIED_WALLPAPER_JSON_FILE = "wallpaper_modified.json"
    
    enum class StyleType {
        ROW,        // Đường khóa kéo
        ZIPPER,     // Con kéo
        WALLPAPER   // Hình nền
    }

    /**
     * Copy file JSON từ row_json/ hoặc zipper_json/ vào internal storage
     * @param context Context
     * @param jsonFileName Tên file JSON (ví dụ: "video_row_1.json" hoặc "video_zipper_1.json")
     * @param styleType Loại style (ROW hoặc ZIPPER)
     * @return Đường dẫn file JSON đã được copy
     */
    fun selectStyle(
        context: Context,
        jsonFileName: String,
        styleType: StyleType
    ): String {
        try {
            // Xác định thư mục và file output
            val (folderName, outputFile) = when (styleType) {
                StyleType.ROW -> "row_json" to MODIFIED_ROW_JSON_FILE
                StyleType.ZIPPER -> "zipper_json" to MODIFIED_ZIPPER_JSON_FILE
                StyleType.WALLPAPER -> "wallpaper/url" to MODIFIED_WALLPAPER_JSON_FILE
            }
            
            val jsonPath = "$folderName/$jsonFileName"
            Log.d(TAG, "Loading JSON from: $jsonPath")
            
            // Đọc file JSON từ assets
            val jsonString = context.assets.open(jsonPath).bufferedReader().use { it.readText() }
            
            // Lưu vào internal storage
            val file = File(context.filesDir, outputFile)
            file.writeText(jsonString)

            Log.d(TAG, "JSON copied to: ${file.absolutePath}")
            return file.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "Error loading style JSON", e)
            throw e
        }
    }
    
    /**
     * Backward compatibility - giữ lại hàm cũ
     */
    @Deprecated("Use selectStyle() instead", ReplaceWith("selectStyle(context, jsonFileName, StyleType.ROW)"))
    fun selectRowStyle(context: Context, jsonFileName: String): String {
        return selectStyle(context, jsonFileName, StyleType.ROW)
    }
    
    /**
     * Lấy danh sách tất cả style có sẵn theo loại
     * @param styleType Loại style (ROW hoặc ZIPPER)
     * @return List các tên file JSON (không có đường dẫn)
     */
    fun getAvailableStyles(context: Context, styleType: StyleType): List<String> {
        return try {
            val folderName = when (styleType) {
                StyleType.ROW -> "row_json"
                StyleType.ZIPPER -> "zipper_json"
                StyleType.WALLPAPER -> "wallpaper/url"
            }
            context.assets.list(folderName)?.toList()?.sorted() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error listing styles", e)
            emptyList()
        }
    }
    
    /**
     * Lấy thumbnail path tương ứng với JSON file
     * video_row_1.json -> row/thumbnail_big_row_1.png
     * video_zipper_1.json -> zipper/thumbnail_big_zipper_1.png
     * video_ai_art_1.json -> wallpaper/thumb/thumbnail_big_wallpaper_ai_art_1.jpg
     */
    fun getThumbnailPath(jsonFileName: String, styleType: StyleType): String {
        return when (styleType) {
            StyleType.ROW -> {
                val number = jsonFileName.replace("video_row_", "").replace(".json", "")
                "row/thumbnail_big_row_$number.png"
            }
            StyleType.ZIPPER -> {
                val number = jsonFileName.replace("video_zipper_", "").replace(".json", "")
                "zipper/thumbnail_big_zipper_$number.png"
            }
            StyleType.WALLPAPER -> {
                // video_ai_art_1.json -> thumbnail_big_wallpaper_ai_art_1.jpg
                val name = jsonFileName.replace("video_", "wallpaper_").replace(".json", "")
                "wallpaper/thumb/thumbnail_big_$name.jpg"
            }
        }
    }

    /**
     * Lấy đường dẫn file JSON đã được chọn theo loại
     */
    fun getModifiedJsonPath(context: Context, styleType: StyleType): String {
        val fileName = when (styleType) {
            StyleType.ROW -> MODIFIED_ROW_JSON_FILE
            StyleType.ZIPPER -> MODIFIED_ZIPPER_JSON_FILE
            StyleType.WALLPAPER -> MODIFIED_WALLPAPER_JSON_FILE
        }
        return File(context.filesDir, fileName).absolutePath
    }

    /**
     * Kiểm tra xem có file JSON đã được chọn không
     */
    fun hasModifiedJson(context: Context, styleType: StyleType): Boolean {
        val fileName = when (styleType) {
            StyleType.ROW -> MODIFIED_ROW_JSON_FILE
            StyleType.ZIPPER -> MODIFIED_ZIPPER_JSON_FILE
            StyleType.WALLPAPER -> MODIFIED_WALLPAPER_JSON_FILE
        }
        return File(context.filesDir, fileName).exists()
    }

    /**
     * Xóa file JSON đã chọn (reset về default)
     */
    fun clearModifiedJson(context: Context, styleType: StyleType) {
        val fileName = when (styleType) {
            StyleType.ROW -> MODIFIED_ROW_JSON_FILE
            StyleType.ZIPPER -> MODIFIED_ZIPPER_JSON_FILE
            StyleType.WALLPAPER -> MODIFIED_WALLPAPER_JSON_FILE
        }
        File(context.filesDir, fileName).delete()
        Log.d(TAG, "Cleared modified JSON: $fileName")
    }
}
