package com.example.pomodoro

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.model.Timer
import kotlinx.coroutines.*
import java.util.*

class TimersViewModel : ViewModel(), TimersManager {

    /**
     * Must be run on [viewModelScope] in the UI thread.
     * One countdown is enough since only one timer can work at a time.
     */
    private var countdownJob: Job? = null

    private val _timers = MutableLiveData<List<Timer>>().apply { value = mutableListOf() }
    val timers: LiveData<List<Timer>>
        get() = _timers

    private val _tickEvent = MutableLiveData<Timer>()
    val tickEvent: LiveData<Timer>
        get() = _tickEvent

    private val _tickEventAtIndex = MutableLiveData<Int>()
    val tickEventAtIndex: LiveData<Int>
        get() = _tickEventAtIndex

    private val _timeUpEvent = MutableLiveData<Timer>()
    val timeUpEvent: LiveData<Timer>
        get() = _timeUpEvent

    private val _timeUpEventAtIndex = MutableLiveData<Int>()
    val timeUpEventAtIndex: LiveData<Int>
        get() = _timeUpEventAtIndex

    // TODO: move countdown logic to a distinct class and expose it to a service directly
    private val _continueUntilSystemTime = MutableLiveData<Long>()
    val continueUntilSystemTime: LiveData<Long>
        get() = _continueUntilSystemTime

    fun doneHandlingTimeUpEvent() { // could be simplified
        _timeUpEvent.value = null
        _timeUpEventAtIndex.value = null
    }

    override fun createTimer(minutes: Int, seconds: Int) {
        val millisToGo = minutes * 60 * 1000L + seconds * 1000L
        val newTimer = Timer(UUID.randomUUID(), millisToGo)
        _timers.value = timers.value?.plus(newTimer) ?: listOf(newTimer)
    }

    override fun startTimer(id: UUID) {
        timers.value?.find { it.id == id }?.also { timerToStart ->
            if (timerToStart.isRunning) {
                Log.e(TAG, "Attempt to start a timer that's already running (${timerToStart.id})")
                return
            }

            if (timerToStart.hasFinished) {
                timerToStart.elapsedTime = 0 // reset finished timer
            }

            stopCurrentlyRunningTimer()
            startCountdown(timerToStart)
            timerToStart.isRunning = true
        }
    }

    override fun stopTimer(id: UUID) {
        timers.value?.find { it.id == id }?.also { timerToStop ->
            if (!timerToStop.isRunning) {
                Log.e(TAG, "Attempt to stop a timer that isn't running (${timerToStop.id})")
                return
            }

            stopCountdown()
        }
    }

    override fun deleteTimer(id: UUID) {
        timers.value?.find { it.id == id }?.also { timerToDelete ->
            if (timerToDelete.isRunning) {
                stopCountdown()
            }
            _timers.value = timers.value?.minus(timerToDelete)
        }
    }

    private fun triggerTimerTickEvent(timer: Timer) {
        timers.value?.indexOf(timer)?.also { index ->
            _tickEventAtIndex.value = index
            _tickEvent.value = timer
        }
    }

    private fun triggerTimeUpEvent(timer: Timer) {
        timers.value?.indexOf(timer)?.also { index ->
            _timeUpEventAtIndex.value = index
            _timeUpEvent.value = timer
        }
    }

    private fun stopCurrentlyRunningTimer() {
        timers.value?.find(Timer::isRunning)?.also { stopTimer(it.id) }
    }

    /**
     * Starts a coroutine which periodically updates [Timer.remainingTime] and triggers [tickEvent].
     *
     * Triggers [timeUpEvent] once the time is over.
     */
    private fun startCountdown(runningTimer: Timer) {
        val interval = COUNTDOWN_INTERVAL
        val initialElapsed = runningTimer.elapsedTime
        countdownJob?.cancel()
        _continueUntilSystemTime.value = SystemClock.elapsedRealtime() + runningTimer.remainingTime
        countdownJob = viewModelScope.launch {
            val startedSysTime = SystemClock.elapsedRealtime()
            Log.i(TAG, "Started countdown for ${runningTimer.id} (${interval}ms tick)")
            try {
                while (true) {
                    val elapsedSinceCountdown = SystemClock.elapsedRealtime() - startedSysTime
                    runningTimer.elapsedTime = initialElapsed + elapsedSinceCountdown
                    triggerTimerTickEvent(runningTimer)
                    if (runningTimer.hasFinished) {
                        cancel()
                    }
                    delay(interval)
                }
            } catch (ex: CancellationException) {
                // cancelled
            } finally {
                runningTimer.isRunning = false
                _continueUntilSystemTime.value = null

                triggerTimerTickEvent(runningTimer)
                if (runningTimer.hasFinished) {
                    triggerTimeUpEvent(runningTimer)
                }

                Log.i(TAG, "Finished countdown for ${runningTimer.id}")
            }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }

    private companion object {
        val TAG = TimersViewModel::class.simpleName
        const val COUNTDOWN_INTERVAL = 250L
    }
}