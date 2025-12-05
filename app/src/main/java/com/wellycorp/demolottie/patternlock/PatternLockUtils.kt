package com.wellycorp.demolottie.patternlock

/**
 * Created by ThuanND on 12/5/2025
 */
object PatternLockUtils {

    /**
     * Convert pattern to string representation
     */
    fun patternToString(pattern: List<Dot>?): String {
        if (pattern == null) return ""

        return buildString {
            pattern.forEach { dot ->
                append(dot.mRow * PatternLockViewKt.Companion.sDotCount + dot.mColumn)
            }
        }
    }

    /**
     * Convert saved string back to pattern
     */
    fun stringToPattern(string: String): List<Dot> {
        val result = ArrayList<Dot>()

        string.forEach { ch ->
            val number = Character.getNumericValue(ch)
            val row = number / PatternLockViewKt.Companion.sDotCount
            val col = number % PatternLockViewKt.Companion.sDotCount
            result.add(Dot.Companion.of(row, col))
        }
        return result
    }
}