package com.wellycorp.demolottie

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SettingsActivity : AppCompatActivity() {

    private lateinit var styleRecyclerView: RecyclerView
    private lateinit var applyButton: Button
    private lateinit var rowTabButton: Button
    private lateinit var zipperTabButton: Button
    private lateinit var wallpaperTabButton: Button
    private lateinit var styleAdapter: StyleAdapter
    
    private var selectedRowStyle: String? = null
    private var selectedZipperStyle: String? = null
    private var selectedWallpaperStyle: String? = null
    private var currentTab: LottieImageReplacer.StyleType = LottieImageReplacer.StyleType.ROW

    companion object {
        private const val PREFS_NAME = "zipper_settings"
        private const val KEY_SELECTED_ROW_JSON = "selected_row_json"
        private const val KEY_SELECTED_ZIPPER_JSON = "selected_zipper_json"
        private const val KEY_SELECTED_WALLPAPER_JSON = "selected_wallpaper_json"
        private const val DEFAULT_ROW_JSON = "video_row_1.json"
        private const val DEFAULT_ZIPPER_JSON = "video_zipper_1.json"
        private const val DEFAULT_WALLPAPER_JSON = "video_ai_art_1.json"

        fun getSelectedRowJsonFile(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_SELECTED_ROW_JSON, DEFAULT_ROW_JSON) ?: DEFAULT_ROW_JSON
        }

        fun saveSelectedRowJsonFile(context: Context, jsonFileName: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SELECTED_ROW_JSON, jsonFileName).apply()
        }
        
        fun getSelectedZipperJsonFile(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_SELECTED_ZIPPER_JSON, DEFAULT_ZIPPER_JSON) ?: DEFAULT_ZIPPER_JSON
        }

        fun saveSelectedZipperJsonFile(context: Context, jsonFileName: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SELECTED_ZIPPER_JSON, jsonFileName).apply()
        }
        
        fun getSelectedWallpaperJsonFile(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_SELECTED_WALLPAPER_JSON, DEFAULT_WALLPAPER_JSON) ?: DEFAULT_WALLPAPER_JSON
        }

        fun saveSelectedWallpaperJsonFile(context: Context, jsonFileName: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SELECTED_WALLPAPER_JSON, jsonFileName).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        styleRecyclerView = findViewById(R.id.styleRecyclerView)
        applyButton = findViewById(R.id.applyButton)
        rowTabButton = findViewById(R.id.rowTabButton)
        zipperTabButton = findViewById(R.id.zipperTabButton)
        wallpaperTabButton = findViewById(R.id.wallpaperTabButton)

        // Load current selected JSON files
        selectedRowStyle = getSelectedRowJsonFile(this)
        selectedZipperStyle = getSelectedZipperJsonFile(this)
        selectedWallpaperStyle = getSelectedWallpaperJsonFile(this)

        // Setup tab buttons
        setupTabButtons()

        // Setup RecyclerView with default tab (ROW)
        setupRecyclerView(LottieImageReplacer.StyleType.ROW)

        // Setup apply button
        applyButton.setOnClickListener {
            try {
                Toast.makeText(this, "Đang áp dụng style...", Toast.LENGTH_SHORT).show()
                
                // Apply row style
                selectedRowStyle?.let { jsonFileName ->
                    LottieImageReplacer.selectStyle(
                        context = this,
                        jsonFileName = jsonFileName,
                        styleType = LottieImageReplacer.StyleType.ROW
                    )
                    saveSelectedRowJsonFile(this, jsonFileName)
                }
                
                // Apply zipper style
                selectedZipperStyle?.let { jsonFileName ->
                    LottieImageReplacer.selectStyle(
                        context = this,
                        jsonFileName = jsonFileName,
                        styleType = LottieImageReplacer.StyleType.ZIPPER
                    )
                    saveSelectedZipperJsonFile(this, jsonFileName)
                }
                
                // Apply wallpaper style
                selectedWallpaperStyle?.let { jsonFileName ->
                    LottieImageReplacer.selectStyle(
                        context = this,
                        jsonFileName = jsonFileName,
                        styleType = LottieImageReplacer.StyleType.WALLPAPER
                    )
                    saveSelectedWallpaperJsonFile(this, jsonFileName)
                }
                
                Toast.makeText(this, "Đã áp dụng style thành công!", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
    
    private fun setupTabButtons() {
        // Row tab click
        rowTabButton.setOnClickListener {
            if (currentTab != LottieImageReplacer.StyleType.ROW) {
                currentTab = LottieImageReplacer.StyleType.ROW
                updateTabUI()
                setupRecyclerView(LottieImageReplacer.StyleType.ROW)
            }
        }
        
        // Zipper tab click
        zipperTabButton.setOnClickListener {
            if (currentTab != LottieImageReplacer.StyleType.ZIPPER) {
                currentTab = LottieImageReplacer.StyleType.ZIPPER
                updateTabUI()
                setupRecyclerView(LottieImageReplacer.StyleType.ZIPPER)
            }
        }
        
        // Wallpaper tab click
        wallpaperTabButton.setOnClickListener {
            if (currentTab != LottieImageReplacer.StyleType.WALLPAPER) {
                currentTab = LottieImageReplacer.StyleType.WALLPAPER
                updateTabUI()
                setupRecyclerView(LottieImageReplacer.StyleType.WALLPAPER)
            }
        }
        
        // Set initial tab state
        updateTabUI()
    }
    
    private fun updateTabUI() {
        when (currentTab) {
            LottieImageReplacer.StyleType.ROW -> {
                rowTabButton.isEnabled = false
                zipperTabButton.isEnabled = true
                wallpaperTabButton.isEnabled = true
            }
            LottieImageReplacer.StyleType.ZIPPER -> {
                rowTabButton.isEnabled = true
                zipperTabButton.isEnabled = false
                wallpaperTabButton.isEnabled = true
            }
            LottieImageReplacer.StyleType.WALLPAPER -> {
                rowTabButton.isEnabled = true
                zipperTabButton.isEnabled = true
                wallpaperTabButton.isEnabled = false
            }
        }
    }

    private fun setupRecyclerView(styleType: LottieImageReplacer.StyleType) {
        // Get all JSON files based on style type
        val styleList = getStylesFromAssets(styleType)
        
        val currentSelected = when (styleType) {
            LottieImageReplacer.StyleType.ROW -> selectedRowStyle ?: DEFAULT_ROW_JSON
            LottieImageReplacer.StyleType.ZIPPER -> selectedZipperStyle ?: DEFAULT_ZIPPER_JSON
            LottieImageReplacer.StyleType.WALLPAPER -> selectedWallpaperStyle ?: DEFAULT_WALLPAPER_JSON
        }

        styleAdapter = StyleAdapter(styleList, currentSelected) { jsonFileName ->
            when (styleType) {
                LottieImageReplacer.StyleType.ROW -> selectedRowStyle = jsonFileName
                LottieImageReplacer.StyleType.ZIPPER -> selectedZipperStyle = jsonFileName
                LottieImageReplacer.StyleType.WALLPAPER -> selectedWallpaperStyle = jsonFileName
            }
        }

        styleRecyclerView.layoutManager = GridLayoutManager(this, 2)
        styleRecyclerView.adapter = styleAdapter
    }

    private fun getStylesFromAssets(styleType: LottieImageReplacer.StyleType): List<StyleItem> {
        val styleList = mutableListOf<StyleItem>()
        
        try {
            // Lấy tất cả file JSON theo loại
            val jsonFiles = LottieImageReplacer.getAvailableStyles(this, styleType)
            
            // Tạo StyleItem cho mỗi JSON file
            jsonFiles.forEach { jsonFileName ->
                // Lấy số từ tên file
                val displayName = when (styleType) {
                    LottieImageReplacer.StyleType.ROW -> {
                        val number = jsonFileName.replace("video_row_", "").replace(".json", "")
                        "Row $number"
                    }
                    LottieImageReplacer.StyleType.ZIPPER -> {
                        val number = jsonFileName.replace("video_zipper_", "").replace(".json", "")
                        "Zipper $number"
                    }
                    LottieImageReplacer.StyleType.WALLPAPER -> {
                        // video_ai_art_1.json -> AI Art 1
                        jsonFileName.replace("video_", "")
                            .replace(".json", "")
                            .replace("_", " ")
                            .split(" ")
                            .joinToString(" ") { it.capitalize() }
                    }
                }
                
                // Lấy thumbnail path tương ứng
                val thumbnailPath = LottieImageReplacer.getThumbnailPath(jsonFileName, styleType)
                
                styleList.add(StyleItem(jsonFileName, displayName, thumbnailPath))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi khi tải danh sách styles", Toast.LENGTH_SHORT).show()
        }

        return styleList
    }
}

data class StyleItem(
    val fileName: String,        // Tên file JSON: video_row_1.json
    val displayName: String,     // Tên hiển thị: Style 1
    val thumbnailPath: String?   // Đường dẫn thumbnail: row/thumbnail_big_row_1.png
)
