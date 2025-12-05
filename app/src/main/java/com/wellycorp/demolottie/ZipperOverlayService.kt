package com.wellycorp.demolottie

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowMetrics
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.airbnb.lottie.LottieAnimationView
import com.wellycorp.demolottie.ClippingLottieView

class ZipperOverlayService : Service() {

    companion object {
        private const val TAG = "ZipperOverlayService"
        private const val CHANNEL_ID = "zipper_overlay_channel"
        private const val NOTIFICATION_ID = 1
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val interpolator = AccelerateDecelerateInterpolator()
    
    // Class-level references for Lottie views
    private var wallpaperLottieView: LottieAnimationView? = null
    private var rowLottieView: ClippingLottieView? = null
    private var zipperLottieView: LottieAnimationView? = null

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Zipper Overlay")
                .setContentText("Đang chạy overlay...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
            startForeground(NOTIFICATION_ID, notification)
        }
        
        setupOverlay()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Zipper Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Zipper overlay service notification"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        // Setup zipper views
        setupZipperViews()
    }

    private fun setupZipperViews() {
        wallpaperLottieView = overlayView?.findViewById(R.id.wallpaperLottieView)
        rowLottieView = overlayView?.findViewById(R.id.rowLottieView)
        zipperLottieView = overlayView?.findViewById(R.id.zipperLottieView)
        
        // Remove all padding from views
        wallpaperLottieView?.setPadding(0, 0, 0, 0)
        rowLottieView?.setPadding(0, 0, 0, 0)
        zipperLottieView?.setPadding(0, 0, 0, 0)
        
        // Enable hardware acceleration for better performance
        wallpaperLottieView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        zipperLottieView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        // rowLottieView already has layerType="software" in XML for clipping
        
        // Set scale type to FIT_XY to maintain animation coordinates
        wallpaperLottieView?.scaleType = ImageView.ScaleType.FIT_XY
        rowLottieView?.scaleType = ImageView.ScaleType.FIT_XY
        zipperLottieView?.scaleType = ImageView.ScaleType.FIT_XY
        
        // Get selected JSON files from settings
        val selectedRowJson = SettingsActivity.getSelectedRowJsonFile(this)
        val selectedZipperJson = SettingsActivity.getSelectedZipperJsonFile(this)
        val selectedWallpaperJson = SettingsActivity.getSelectedWallpaperJsonFile(this)
        Log.d(TAG, "Loading selected styles - Row: $selectedRowJson, Zipper: $selectedZipperJson, Wallpaper: $selectedWallpaperJson")
        
        // Load wallpaper animation
        if (LottieImageReplacer.hasModifiedJson(this, LottieImageReplacer.StyleType.WALLPAPER)) {
            // Load from internal storage (user đã chọn style)
            val modifiedPath = LottieImageReplacer.getModifiedJsonPath(this, LottieImageReplacer.StyleType.WALLPAPER)
            Log.d(TAG, "Loading selected wallpaper style from: $modifiedPath")
            wallpaperLottieView?.setAnimation(java.io.FileInputStream(modifiedPath), null)
        } else {
            // Load default từ assets/wallpaper/url/
            Log.d(TAG, "Loading default wallpaper style: $selectedWallpaperJson")
            wallpaperLottieView?.setAnimation("wallpaper/url/$selectedWallpaperJson")
        }
        wallpaperLottieView?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        // Load row animation
        if (LottieImageReplacer.hasModifiedJson(this, LottieImageReplacer.StyleType.ROW)) {
            // Load from internal storage (user đã chọn style)
            val modifiedPath = LottieImageReplacer.getModifiedJsonPath(this, LottieImageReplacer.StyleType.ROW)
            Log.d(TAG, "Loading selected row style from: $modifiedPath")
            rowLottieView?.setAnimation(java.io.FileInputStream(modifiedPath), null)
        } else {
            // Load default từ assets/row_json/
            Log.d(TAG, "Loading default row style: $selectedRowJson")
            rowLottieView?.setAnimation("row_json/$selectedRowJson")
        }
        
        // Load zipper animation
        if (LottieImageReplacer.hasModifiedJson(this, LottieImageReplacer.StyleType.ZIPPER)) {
            // Load from internal storage (user đã chọn style)
            val modifiedPath = LottieImageReplacer.getModifiedJsonPath(this, LottieImageReplacer.StyleType.ZIPPER)
            Log.d(TAG, "Loading selected zipper style from: $modifiedPath")
            zipperLottieView?.setAnimation(java.io.FileInputStream(modifiedPath), null)
        } else {
            // Load default từ assets/zipper_json/
            Log.d(TAG, "Loading default zipper style: $selectedZipperJson")
            zipperLottieView?.setAnimation("zipper_json/$selectedZipperJson")
        }
        
        // Set initial progress - all animations start at 0 for synchronization
        wallpaperLottieView?.progress = 0f
        rowLottieView?.progress = 0f
        zipperLottieView?.progress = 0f // Start at 0 to position zipper at top
        rowLottieView?.setRevealProgress(0f)
        
        // Ensure zipper is visible from the start
        zipperLottieView?.alpha = 1f
        zipperLottieView?.visibility = View.VISIBLE
        zipperLottieView?.bringToFront() // Bring zipper to front
        
        Log.d(TAG, "Zipper setup - visibility: ${zipperLottieView?.visibility}, alpha: ${zipperLottieView?.alpha}, progress: ${zipperLottieView?.progress}")
        
        // Ensure views are positioned at top edge with no offset
        wallpaperLottieView?.post {
            wallpaperLottieView?.translationY = 0f
            wallpaperLottieView?.translationX = 0f
        }
        rowLottieView?.post {
            rowLottieView?.translationY = 0f
            rowLottieView?.translationX = 0f
        }
        zipperLottieView?.post {
            zipperLottieView?.translationY = 0f
            zipperLottieView?.translationX = 0f
        }
        
        // Setup touch listener with dismiss functionality
        setupTouchListener()
    }

    private fun setupTouchListener() {
        var initialTouchY = 0f
        var initialProgress = 0f
        
        zipperLottieView?.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialTouchY = event.y
                    initialProgress = rowLottieView?.progress ?: 0f
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.y - initialTouchY
                    val viewHeight = view.height.toFloat()
                    val progressChange = deltaY / viewHeight
                    var newProgress = initialProgress + progressChange
                    
                    newProgress = newProgress.coerceIn(0f, 1f)
                    
                    // Sync all views with same progress for synchronized animation
                    wallpaperLottieView?.progress = newProgress
                    rowLottieView?.progress = newProgress
                    zipperLottieView?.progress = newProgress
                    rowLottieView?.setRevealProgress(newProgress)
                    
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    val currentProgress = rowLottieView?.progress ?: 0f
                    
                    if (currentProgress < 0.5f) {
                        // Auto-lock: animate back to initial position
                        animateToInitialPosition()
                    } else {
                        // Auto-play: animate to complete and dismiss
                        animateToCompleteAndDismiss()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun animateToInitialPosition() {
        val initialProgress = 0f
        val currentProgress = rowLottieView?.progress ?: 0f
        val duration = 300L // 0.3 seconds
        val startTime = System.currentTimeMillis()
        
        val animateRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val fraction = (elapsed.toFloat() / duration).coerceAtMost(1f)
                val interpolatedFraction = interpolator.getInterpolation(fraction)
                
                val newProgress = currentProgress - (currentProgress - initialProgress) * interpolatedFraction
                
                // Sync all views with same progress
                wallpaperLottieView?.progress = newProgress
                rowLottieView?.progress = newProgress
                zipperLottieView?.progress = newProgress
                rowLottieView?.setRevealProgress(newProgress)
                
                if (fraction < 1f) {
                    handler.postDelayed(this, 16) // ~60fps
                }
            }
        }
        
        handler.post(animateRunnable)
    }

    private fun animateToCompleteAndDismiss() {
        val currentProgress = rowLottieView?.progress ?: 0f
        val duration = 500L // 0.5 seconds
        val startTime = System.currentTimeMillis()
        
        val animateRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val fraction = (elapsed.toFloat() / duration).coerceAtMost(1f)
                val interpolatedFraction = interpolator.getInterpolation(fraction)
                
                val newProgress = currentProgress + (1f - currentProgress) * interpolatedFraction
                
                // Sync all views with same progress
                wallpaperLottieView?.progress = newProgress
                rowLottieView?.progress = newProgress
                zipperLottieView?.progress = newProgress
                rowLottieView?.setRevealProgress(newProgress)
                
                if (fraction < 1f) {
                    handler.postDelayed(this, 16) // ~60fps
                } else {
                    // Animation complete, dismiss overlay
                    handler.postDelayed({ hideOverlay() }, 200) // Small delay before dismiss
                }
            }
        }
        
        handler.post(animateRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        showOverlay()
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        Log.d(TAG, "Attempting to show overlay")
        try {
            if (overlayView?.parent == null) {
                Log.d(TAG, "Adding overlay view to window")
                
                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val flag = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    1832/*flag*/,
                    PixelFormat.TRANSLUCENT
                )

                params.gravity = Gravity.TOP or Gravity.START
                params.x = 0
                params.y = 0
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                
                // Remove any padding from overlay view
                overlayView?.setPadding(0, 0, 0, 0)
                getPhysicalScreenSize(this).let { (w, h) ->
                    params.width = w
                    params.height = h
                }
                windowManager?.addView(overlayView, params)
                
                // Reset views to initial position AFTER they're added to window
                resetViewsToInitialPosition()
                
                Log.d(TAG, "Overlay view added successfully")
            } else {
                Log.d(TAG, "Overlay already visible")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay", e)
            e.printStackTrace()
        }
    }

    fun getPhysicalScreenSize(context: Context?): Pair<Int, Int> {
        var w = 0
        var h = 0
        context?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val metrics: WindowMetrics =
                    context.getSystemService(WindowManager::class.java).currentWindowMetrics
                w = metrics.bounds.right - metrics.bounds.left
                h = metrics.bounds.bottom - metrics.bounds.top
            } else {
                val windowManager =
                    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val metrics = DisplayMetrics()
                windowManager.defaultDisplay.getRealMetrics(metrics)
                w = metrics.widthPixels
                h = metrics.heightPixels
            }
        }
        return Pair(w, h)
    }
    
    private fun resetViewsToInitialPosition() {
        // Reset all views to 0 for synchronized start position
        wallpaperLottieView?.progress = 0f
        rowLottieView?.progress = 0f
        zipperLottieView?.progress = 0f
        rowLottieView?.setRevealProgress(0f)
        
        // Ensure zipper remains visible
        zipperLottieView?.alpha = 1f
        zipperLottieView?.visibility = View.VISIBLE
        zipperLottieView?.bringToFront()
        
        Log.d(TAG, "Reset views - all progress: 0")
    }

    private fun hideOverlay() {
        Log.d(TAG, "Hiding overlay")
        try {
            if (overlayView?.parent != null) {
                windowManager?.removeView(overlayView)
            }
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (overlayView?.parent != null) {
                windowManager?.removeView(overlayView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
