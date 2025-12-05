package com.wellycorp.demolottie.patternlock

/**
 * Created by ThuanND on 12/5/2025
 */
interface PatternLockViewListener {
    /**
     * Fired when the pattern drawing has just started.
     */
    fun onStarted()

    /**
     * Fired when the pattern is progressing (user connected a new dot).
     */
    fun onProgress(progressPattern: List<Dot>)

    /**
     * Fired when the user has completed drawing the pattern.
     */
    fun onComplete(pattern: List<Dot>)

    /**
     * Fired when the pattern has been cleared.
     */
    fun onCleared()
}