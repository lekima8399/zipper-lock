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
import com.wellycorp.demolottie.databinding.ViewOverlayLockBinding
import kotlin.getValue

class ZipperOverlayService : Service() {

    companion object {
        private const val TAG = "ZipperOverlayService"
        private const val CHANNEL_ID = "zipper_overlay_channel"
        private const val NOTIFICATION_ID = 1
    }

    private val windowManager: WindowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val binding: ViewOverlayLockBinding by lazy {
        ViewOverlayLockBinding.inflate(LayoutInflater.from(this.baseContext), null, false)
    }
    private val handler = Handler(Looper.getMainLooper())
    private val interpolator = AccelerateDecelerateInterpolator()

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        createNotification()
        setupZipperViews()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlay()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (binding.root.parent != null) {
                windowManager.removeView(binding.root)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotification(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Zipper Overlay")
                .setContentText("Đang chạy overlay...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
            startForeground(NOTIFICATION_ID, notification)
        }
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

    private fun setupZipperViews() {
        binding.wallpaperLottieView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        binding.zipperLottieView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        val selectedRowJson = SettingsActivity.getSelectedRowJsonFile(this)
        val selectedZipperJson = SettingsActivity.getSelectedZipperJsonFile(this)
        val selectedWallpaperJson = SettingsActivity.getSelectedWallpaperJsonFile(this)
        if (LottieImageReplacer.hasModifiedJson(this, LottieImageReplacer.StyleType.WALLPAPER)) {
            val modifiedPath = LottieImageReplacer.getModifiedJsonPath(this, LottieImageReplacer.StyleType.WALLPAPER)
            binding.wallpaperLottieView.setAnimation(java.io.FileInputStream(modifiedPath), null)
        } else {
            binding.wallpaperLottieView.setAnimation("wallpaper/url/$selectedWallpaperJson")
        }
        binding.wallpaperLottieView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        if (LottieImageReplacer.hasModifiedJson(this, LottieImageReplacer.StyleType.ROW)) {
            val modifiedPath = LottieImageReplacer.getModifiedJsonPath(this, LottieImageReplacer.StyleType.ROW)
            binding.rowLottieView.setAnimation(java.io.FileInputStream(modifiedPath), null)
        } else {
            binding.rowLottieView.setAnimation("row_json/$selectedRowJson")
        }
        if (LottieImageReplacer.hasModifiedJson(this, LottieImageReplacer.StyleType.ZIPPER)) {
            val modifiedPath = LottieImageReplacer.getModifiedJsonPath(this, LottieImageReplacer.StyleType.ZIPPER)
            binding.zipperLottieView.setAnimation(java.io.FileInputStream(modifiedPath), null)
        } else {
            binding.zipperLottieView.setAnimation("zipper_json/$selectedZipperJson")
        }
        resetViewsToInitialPosition()
        setupTouchListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        var initialTouchY = 0f
        var initialProgress = 0f
        
        binding.zipperLottieView.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialTouchY = event.y
                    initialProgress = binding.rowLottieView.progress
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.y - initialTouchY
                    val viewHeight = view.height.toFloat()
                    val progressChange = deltaY / viewHeight
                    var newProgress = initialProgress + progressChange
                    newProgress = newProgress.coerceIn(0f, 1f)
                    setProgressLottieView(newProgress)
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    val currentProgress = binding.rowLottieView.progress
                    if (currentProgress < 0.5f) {
                        animateToInitialPosition()
                    } else {
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
        val currentProgress = binding.rowLottieView.progress
        val duration = 300L
        val startTime = System.currentTimeMillis()
        
        val animateRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val fraction = (elapsed.toFloat() / duration).coerceAtMost(1f)
                val interpolatedFraction = interpolator.getInterpolation(fraction)
                val newProgress = currentProgress - (currentProgress - initialProgress) * interpolatedFraction
                setProgressLottieView(newProgress)
                if (fraction < 1f) {
                    handler.postDelayed(this, 16)
                }
            }
        }
        handler.post(animateRunnable)
    }

    private fun animateToCompleteAndDismiss() {
        val currentProgress = binding.rowLottieView.progress
        val duration = 500L
        val startTime = System.currentTimeMillis()
        
        val animateRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val fraction = (elapsed.toFloat() / duration).coerceAtMost(1f)
                val interpolatedFraction = interpolator.getInterpolation(fraction)
                
                val newProgress = currentProgress + (1f - currentProgress) * interpolatedFraction
                setProgressLottieView(newProgress)
                if (fraction < 1f) {
                    handler.postDelayed(this, 16)
                } else {
                    handler.postDelayed({ hideOverlay() }, 200)
                }
            }
        }
        
        handler.post(animateRunnable)
    }

    private fun showOverlay() {
        try {
            if (binding.root.parent == null) {
                
                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
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
                binding.root.setPadding(0, 0, 0, 0)
                getPhysicalScreenSize(this).let { (w, h) ->
                    params.width = w
                    params.height = h
                }
                windowManager.addView(binding.root, params)
                
                // Reset views to initial position AFTER they're added to window
                resetViewsToInitialPosition()
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
                val metrics = DisplayMetrics()
                windowManager.defaultDisplay.getRealMetrics(metrics)
                w = metrics.widthPixels
                h = metrics.heightPixels
            }
        }
        return Pair(w, h)
    }

    private fun setProgressLottieView(progress: Float) {
        binding.wallpaperLottieView.progress = progress
        binding.rowLottieView.progress = progress
        binding.zipperLottieView.progress = progress
        binding.rowLottieView.setRevealProgress(progress)
    }
    
    private fun resetViewsToInitialPosition() {
        setProgressLottieView(0f)
        binding.zipperLottieView.bringToFront()
    }

    private fun hideOverlay() {
        try {
            if (binding.root.parent != null) {
                windowManager.removeView(binding.root)
            }
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
