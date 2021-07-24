package com.example.pomodoro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.viewModels
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
import java.util.*

class MainActivity : AppCompatActivity(), LifecycleObserver {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: TimersViewModel by viewModels()

    private val timerAdapter by lazy { TimerAdapter(viewModel) }

    private var continueUntilSystemTime: Long? = null

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

        attachViewModelObservers()
        attachUiHandlers()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNotifications()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onApplicationBackground() {
        continueUntilSystemTime?.also { startNotifications(it) }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onApplicationForeground() {
        stopNotifications()
    }

    private fun attachViewModelObservers() {
        viewModel.timers.observe(this) { timers ->
            timerAdapter.submitList(timers)
        }

        // TODO: Notify with payloads to avoid item view re-creation
        viewModel.tickEventAtIndex.observe(this) { timerIndex ->
            timerAdapter.notifyItemChanged(timerIndex)
        }

        viewModel.timeUpEventAtIndex.observe(this) { timerIndex ->
            timerAdapter.notifyItemChanged(timerIndex)
        }

        viewModel.continueUntilSystemTime.observe(this) {
            continueUntilSystemTime = it
        }
    }

    @SuppressLint("ShowToast")
    private fun attachUiHandlers() {
        binding.editMinutes.addTextChangedListener(NonNegativeIntTextWatcher(59))
        binding.editMinutes.addTextChangedListener { minutesText ->
            if (!minutesText.isNullOrEmpty()) {
                binding.editSeconds.error = null
            }
        }

        binding.editSeconds.addTextChangedListener(NonNegativeIntTextWatcher(59))
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

            viewModel.createTimer(minutes, seconds)
        }
    }

    private fun stopNotifications() {
        val stopIntent = Intent(this, PomodoroService::class.java)
        stopIntent.putExtras(PomodoroService.createBundleForStopping())
        startService(stopIntent)
    }

    private fun startNotifications(untilSystemTime: Long) {
        val startIntent = Intent(this, PomodoroService::class.java)
        startIntent.putExtras(PomodoroService.createBundleForStarting(untilSystemTime))
        startService(startIntent)
    }
}
