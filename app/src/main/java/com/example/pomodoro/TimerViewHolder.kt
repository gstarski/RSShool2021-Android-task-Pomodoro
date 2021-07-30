package com.example.pomodoro

import android.graphics.drawable.AnimationDrawable
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.example.pomodoro.config.CrossCheckShenanigans
import com.example.pomodoro.databinding.ItemTimerBinding
import com.example.pomodoro.model.Timer
import com.google.android.material.color.MaterialColors

class TimerViewHolder(
    val binding: ItemTimerBinding,
    val manager: TimersManager) : RecyclerView.ViewHolder(binding.root) {

    fun bind(timer: Timer) {
        attachClickListeners(timer)
        displayTimeRemaining(timer)
        setTimeDisplayColor(timer)
        setProgress(timer)
        setBackground(timer)
        setBlinking(timer)
        adjustStartStopButton(timer)
    }

    private fun attachClickListeners(timer: Timer) {
        binding.buttonStartStop.setOnClickListener {
            if (timer.isRunning) {
                manager.stopTimer(timer.id)
            } else {
                manager.startTimer(timer.id)
            }
        }

        binding.buttonDelete.setOnClickListener {
            manager.deleteTimer(timer.id)
        }
    }

    private fun setBlinking(timer: Timer) {
        val blinking = (binding.blinkingIndicator.background as AnimationDrawable)

        if (timer.isRunning) {
            binding.blinkingIndicator.isInvisible = false
            blinking.start()
        } else {
            binding.blinkingIndicator.isInvisible = true
            blinking.stop()
        }
    }

    private fun displayTimeRemaining(timer: Timer) {
        val shouldShowInitial =
            CrossCheckShenanigans.SHOULD_SHOW_INITIAL_VALUE_WHEN_FINISHED && timer.hasFinished

        val time = if (shouldShowInitial) timer.initialTime else timer.remainingTime
        binding.textTimeRemaining.text = formatTime(time)
    }

    private fun setTimeDisplayColor(timer: Timer) {
        val colorAttr = if (timer.hasFinished) R.attr.colorOnPrimary else R.attr.colorOnSurface
        val color = MaterialColors.getColor(binding.root, colorAttr)
        binding.textTimeRemaining.setTextColor(color)
    }

    private fun adjustStartStopButton(timer: Timer) {
        if (timer.hasFinished && !CrossCheckShenanigans.SHOULD_ALLOW_RESTART_WHEN_FINISHED) {
            binding.buttonStartStop.isEnabled = false
        }

        binding.buttonStartStop.text = when {
            timer.isRunning -> "Stop"
            timer.hasFinished -> "Again"
            else -> "Start"
        }
    }

    private fun setProgress(timer: Timer) {
        val progress = if (timer.hasFinished) 0f else timer.elapsedTime.toFloat() / timer.initialTime
        binding.progressPie.setProgress(progress)
    }

    private fun setBackground(timer: Timer) {
        if (timer.hasFinished)  {
            binding.root.setCardBackgroundColor(MaterialColors.getColor(binding.root, R.attr.colorPrimaryVariant))
        } else {
            val color = MaterialColors.getColor(binding.root, R.attr.colorSurface)
            binding.root.setCardBackgroundColor(color)
        }
    }
}