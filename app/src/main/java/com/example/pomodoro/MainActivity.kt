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
import com.example.pomodoro.service.PomodoroService
import com.example.pomodoro.utils.NonNegativeIntTextWatcher
import com.example.pomodoro.utils.playAlertRingtone
import java.util.*

class MainActivity : AppCompatActivity(), LifecycleObserver {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: TimersViewModel by viewModels()

    private val timerAdapter by lazy { TimerAdapter(viewModel) }

    private var continueUntilSystemTime: Long? = null

    private var shouldPlayAlerts: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeRecyclerView()
        attachViewModelObservers()
        attachUiHandlers()
    }

    override fun onStart() {
        binding.editSeconds.clearFocus()
        binding.editMinutes.clearFocus()
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNotifications()
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onApplicationBackground() {
        shouldPlayAlerts = false
        continueUntilSystemTime?.also { startNotifications(it) }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onApplicationForeground() {
        stopNotifications()
    }

    private fun initializeRecyclerView() {
        binding.recyclerTimers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = timerAdapter

            itemAnimator?.apply {
                changeDuration = ANIM_TIME_VERY_SHORT
                moveDuration = ANIM_TIME_VERY_SHORT
            }

            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }
    }

    private fun attachViewModelObservers() {
        viewModel.timers.observe(this) { timers ->
            timerAdapter.submitList(timers)
        }

        viewModel.timeUpEventAtIndex.observe(this) { timerIndex ->
            timerIndex?.also {
                if (shouldPlayAlerts) {
                    playAlertRingtone(this)
                }
                timerAdapter.notifyItemChanged(timerIndex)
                viewModel.doneHandlingTimeUpEvent()
            }
        }

        viewModel.tickEventAtIndex.observe(this) { timerIndex ->
            timerIndex?.also {
                shouldPlayAlerts = true

                // this could introduce a bug: last tick happens during animation -> finished state is not rendered
                // it will not occur since adapter gets notified during timeUpEvent
                if (binding.recyclerTimers.itemAnimator?.isRunning == false) {
                    timerAdapter.notifyItemChanged(timerIndex)
                }
            }
        }

        viewModel.continueUntilSystemTime.observe(this) {
            continueUntilSystemTime = it
        }
    }

    @SuppressLint("ShowToast")
    private fun attachUiHandlers() {
        binding.editMinutes.addTextChangedListener(NonNegativeIntTextWatcher(MAX_MINUTES))
        binding.editMinutes.addTextChangedListener { minutesText ->
            if (!minutesText.isNullOrEmpty()) {
                binding.editSeconds.error = null
            }
        }

        binding.editSeconds.addTextChangedListener(NonNegativeIntTextWatcher(MAX_SECONDS))
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

    companion object {
        // 23:59:59
        private const val MAX_SECONDS = 59
        private const val MAX_MINUTES = 1439

        private const val ANIM_TIME_VERY_SHORT = 120L

        const val EXTRA_RESTORED_FROM_SERVICE = "RESTORED_FROM_SERVICE"
    }
}
