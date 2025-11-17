package com.wellycorp.demolottie

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieAnimationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val lottieView = findViewById<LottieAnimationView>(R.id.rowLottieView)
        lottieView.setAnimation("row.json")
        
        var initialTouchY = 0f
        var initialProgress = 0f
        
        lottieView.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialTouchY = event.y
                    initialProgress = lottieView.progress
                    if (lottieView.isAnimating) {
                        lottieView.pauseAnimation()
                    }
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.y - initialTouchY
                    val viewHeight = view.height.toFloat()
                    val progressChange = deltaY / viewHeight
                    var newProgress = initialProgress + progressChange
                    
                    // Clamp progress between 0 and 1
                    newProgress = newProgress.coerceIn(0f, 1f)
                    lottieView.progress = newProgress
                    true
                }
                else -> false
            }
        }
    }
}