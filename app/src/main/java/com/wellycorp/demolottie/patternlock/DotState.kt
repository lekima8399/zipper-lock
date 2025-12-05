package com.wellycorp.demolottie.patternlock

import android.animation.ValueAnimator

/**
 * Created by ThuanND on 12/5/2025
 */
data class DotState(
    var mScale: Float = 1.0f,
    var mTranslateY: Float = 0.0f,
    var mAlpha: Float = 1.0f,
    var mSize: Float = 0f,
    var mLineEndX: Float = Float.MIN_VALUE,
    var mLineEndY: Float = Float.MIN_VALUE,
    var mLineAnimator: ValueAnimator? = null
)