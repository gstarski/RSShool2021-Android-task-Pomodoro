package com.example.pomodoro

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.pomodoro.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity(), TimersController {

    private lateinit var binding: ActivityMainBinding

    private val timerAdapter = TimerAdapter(this)
    private val timers = mutableListOf<Timer>()

    // Using one countdown since only one timer can work at a time
    private var countdown: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerTimers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = timerAdapter
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }

        binding.buttonAddTimer.setOnClickListener {
            val minutes = binding.editMinutes.text.toString().toIntOrNull() ?: 0
            val seconds = 4
            createTimer(minutes, seconds)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countdown?.cancel()
    }

    override fun createTimer(minutes: Int, seconds: Int) {
        val millisToGo = minutes * 60 * 1000L + seconds * 1000L
        val newTimer = Timer(UUID.randomUUID(), millisToGo)
        timers.add(newTimer)
        timerAdapter.submitList(timers.toList())
    }

    // Possible improvement: switch to payloads

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
}