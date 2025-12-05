package com.wellycorp.demolottie.patternlock

import android.view.HapticFeedbackConstants
import androidx.annotation.IntDef
import java.util.ArrayList
import kotlin.math.abs

/**
 * Created by ThuanND on 12/5/2025
 */
class DotHelper {

    fun initValue(){

    }

}

@IntDef(
    AspectRatio.ASPECT_RATIO_SQUARE,
    AspectRatio.ASPECT_RATIO_WIDTH_BIAS,
    AspectRatio.ASPECT_RATIO_HEIGHT_BIAS
)
@Retention(AnnotationRetention.SOURCE)
annotation class AspectRatio {
    companion object {
        const val ASPECT_RATIO_SQUARE = 0
        const val ASPECT_RATIO_WIDTH_BIAS = 1
        const val ASPECT_RATIO_HEIGHT_BIAS = 2
    }
}

@IntDef(PatternViewMode.CORRECT, PatternViewMode.AUTO_DRAW, PatternViewMode.WRONG)
@Retention(AnnotationRetention.SOURCE)
annotation class PatternViewMode {
    companion object {
        const val CORRECT = 0
        const val AUTO_DRAW = 1
        const val WRONG = 2
    }
}