package com.example.pomodoro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.pomodoro.databinding.ActivityMainBinding
import com.example.pomodoro.utils.NonNegativeIntTextWatcher
import kotlinx.coroutines.Job
import java.util.*

class MainActivity : AppCompatActivity(), TimersController, LifecycleObserver {

    private lateinit var binding: ActivityMainBinding

    private val timerAdapter = TimerAdapter(this)
    private val timers = mutableListOf<Timer>()

    private var countdownJob: Job? = null // switch to coroutines?

    // Using one countdown since only one timer can work at a time
    private var countdown: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerTimers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = timerAdapter
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }

        attachHandlers()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdown?.cancel()
        stopNotifications()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onApplicationBackground() {
        timers.find(Timer::isRunning)?.also { runningTimer ->
            startNotifications(runningTimer)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onApplicationForeground() {
        stopNotifications()
    }

    @SuppressLint("ShowToast")
    private fun attachHandlers() {
        binding.editMinutes.addTextChangedListener(NonNegativeIntTextWatcher(60))
        binding.editMinutes.addTextChangedListener { minutesText ->
            if (!minutesText.isNullOrEmpty()) {
                binding.editSeconds.error = null
            }
        }

        binding.editSeconds.addTextChangedListener(NonNegativeIntTextWatcher(60))
        binding.editSeconds.addTextChangedListener { secondsText ->
            if (!secondsText.isNullOrEmpty()) {
                binding.editMinutes.error = null
            }
        }

        binding.buttonAddTimer.setOnClickListener {
            if (binding.editMinutes.text.isEmpty() && binding.editSeconds.text.isEmpty()) {
                binding.editMinutes.error = "How long?"
                binding.editSeconds.error = "How long?"
                return@setOnClickListener
            }

            val minutes = binding.editMinutes.text.toString().toIntOrNull() ?: 0
            val seconds = binding.editSeconds.text.toString().toIntOrNull() ?: 0

            if (minutes + seconds == 0) {
                val msg = "What a peculiar choice of timing... \uD83E\uDD14"
                Toast.makeText(this, msg, Toast.LENGTH_LONG).apply {
                    this.setGravity(Gravity.CENTER, 0, 0)
                    this.show()
                }
                return@setOnClickListener
            }

            createTimer(minutes, seconds)
        }
    }

    private fun stopNotifications() {
        val stopIntent = Intent(this, PomodoroService::class.java)
        stopIntent.putExtras(PomodoroService.createBundleForStopping())
        startService(stopIntent)
    }

    private fun startNotifications(timer: Timer) {
        val startIntent = Intent(this, PomodoroService::class.java)
        startIntent.putExtras(PomodoroService.createBundleForStarting(
            timer.remainingTime + SystemClock.elapsedRealtime()
        ))
        startService(startIntent)
    }


    //region Timers
    // TODO: Move timers to ViewModel
    // TODO: Notify adapter about timer changes with payloads
    override fun createTimer(minutes: Int, seconds: Int) {
        val millisToGo = minutes * 60 * 1000L + seconds * 1000L
        val newTimer = Timer(UUID.randomUUID(), millisToGo)
        timers.add(newTimer)
        timerAdapter.submitList(timers.toList())
    }

    override fun startTimer(id: UUID) {
        timers.find { it.id == id }?.also { timerToStart ->
            if (timerToStart.isRunning) {
                throw RuntimeException(
                    "Attempt to start a timer that's already running (${timerToStart.id})")
            }

            stopCurrentlyRunningTimer()
            timerToStart.isRunning = true

            if (timerToStart.hasFinished) {
                timerToStart.elapsedTime = 0
            }

            timerAdapter.notifyItemChanged(timers.indexOf(timerToStart))
            countdown?.cancel()
            countdown = createCountdown(timerToStart)
            countdown?.start()
        }
    }

    override fun stopTimer(id: UUID) {
        timers.find { it.id == id }?.also { timerToStop ->
            if (!timerToStop.isRunning) {
                throw RuntimeException(
                    "Attempt to stop a timer that isn't running (${timerToStop.id})")
            }

            countdown?.cancel()
            timerToStop.isRunning = false
            timerAdapter.notifyItemChanged(timers.indexOf(timerToStop))
        }
    }

    override fun deleteTimer(id: UUID) {
        timers.find { it.id == id }?.let { timerToDelete ->
            if (timerToDelete.isRunning) {
                countdown?.cancel()
            }

            timers.remove(timerToDelete)
            timerAdapter.submitList(timers.toList())
        }
    }

    private fun stopCurrentlyRunningTimer() {
        timers.find(Timer::isRunning)?.also { stopTimer(it.id) }
    }

    private fun createCountdown(timer: Timer): CountDownTimer {
        val interval = 250L
        var firstTick = true
        return object : CountDownTimer(timer.remainingTime, interval) {
            override fun onTick(millisUntilFinished: Long) {
                if (firstTick) { // skipping the first tick because onTick() fires right away
                    firstTick = false
                    return
                }

                timer.elapsedTime += interval

                if (timer.remainingTime == 0L) {
                    cancel()
                    stopTimer(timer.id)
                } else {
                    timerAdapter.notifyItemChanged(timers.indexOf(timer))
                }
            }

            override fun onFinish() {
                timer.elapsedTime = timer.initialTime
                stopTimer(timer.id)
            }
        }
    }
    //endregion Timers
}