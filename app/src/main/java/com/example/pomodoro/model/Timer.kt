package com.example.pomodoro.model

import java.util.*

data class Timer(
    val id: UUID,
    val initialTime: Long,
    var elapsedTime: Long = 0,
    var isRunning: Boolean = false
) {
    /**
     * Calculated as a difference between the initial time and elapsed time.
     */
    val remainingTime: Long
        get() = (initialTime - elapsedTime).coerceAtLeast(0)

    val hasFinished: Boolean
        get() = remainingTime == 0L
}