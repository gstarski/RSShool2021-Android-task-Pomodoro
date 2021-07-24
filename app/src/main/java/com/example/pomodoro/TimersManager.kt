package com.example.pomodoro

import java.util.*

/**
 * An object that owns a list of timers.
 */
interface TimersManager {
    /**
     * Creates a new timer.
     * It needs to be started manually (no autostart).
     */
    fun createTimer(minutes: Int, seconds: Int)

    /**
     * Starts the timer.
     * Stops other timers (only one timer can work at a time).
     */
    fun startTimer(id: UUID)

    /**
     * Puts the timer on pause (without progress loss).
     */
    fun stopTimer(id: UUID)

    /**
     * Deletes the timer.
     */
    fun deleteTimer(id: UUID)
}