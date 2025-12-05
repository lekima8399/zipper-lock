package com.wellycorp.demolottie.patternlock

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Converted to Kotlin by ChatGPT – Idiomatic & Clean
 * Original author: ThuanND on 12/5/2025
 */
@Parcelize
data class Dot(
    val mRow: Int,
    val mColumn: Int
) : Parcelable {

    fun getRow(): Int {
        return mRow
    }

    fun getColumn(): Int {
        return mColumn
    }

    val id: Int
        get() = mRow * PatternLockViewKt.Companion.sDotCount + mColumn

    companion object {

        /** Pre-created dot grid, like static block in Java */
        private val sDots: Array<Array<Dot>> by lazy {
            Array(PatternLockViewKt.Companion.sDotCount) { row ->
                Array(PatternLockViewKt.Companion.sDotCount) { col ->
                    Dot(row, col)
                }
            }
        }

        @JvmStatic
        fun of(row: Int, column: Int): Dot {
            checkRange(row, column)
            return sDots[row][column]
        }

        @JvmStatic
        fun of(id: Int): Dot {
            val count = PatternLockViewKt.Companion.sDotCount
            return of(id / count, id % count)
        }

        private fun checkRange(row: Int, column: Int) {
            val max = PatternLockViewKt.Companion.sDotCount - 1
            require(row in 0..max) {
                "mRow must be in range 0-$max"
            }
            require(column in 0..max) {
                "mColumn must be in range 0-$max"
            }
        }
    }

    override fun toString(): String {
        return "(Row = $mRow, Col = $mColumn)"
    }
}
