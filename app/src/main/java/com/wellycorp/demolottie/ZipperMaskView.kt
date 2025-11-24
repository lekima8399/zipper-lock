package com.wellycorp.demolottie

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class ZipperMaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maskPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        isAntiAlias = true
    }

    private var currentProgress = 0f
    private var gradient: LinearGradient? = null

    fun setProgress(progress: Float) {
        currentProgress = progress
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient()
    }

    private fun updateGradient() {
        val height = height.toFloat()
        if (height <= 0) return
        
        // Create gradient that goes from transparent at top to opaque at zipper position
        val zipperPosition = height * (0.3f + currentProgress * 0.4f) // Adjust range as needed
        
        gradient = LinearGradient(
            0f, 0f,
            0f, zipperPosition,
            intArrayOf(
                0xFF000000.toInt(), // Opaque black (will become transparent with DST_OUT)
                0x00000000.toInt()  // Transparent
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        maskPaint.shader = gradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (gradient == null) {
            updateGradient()
        }
        
        // Draw the mask - this will "punch out" the area to reveal wallpaper below
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)
    }
}
