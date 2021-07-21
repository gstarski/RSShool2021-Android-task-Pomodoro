package com.example.pomodoro

import android.graphics.drawable.AnimationDrawable
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.example.pomodoro.databinding.ItemTimerBinding
import com.google.android.material.color.MaterialColors

class TimerViewHolder(
    val binding: ItemTimerBinding,
    val controller: TimersController) : RecyclerView.ViewHolder(binding.root) {

    fun bind(timer: Timer) {
        if (timer.isRunning) {
            startBlinkingIndicator()
            binding.buttonStartStop.text = "Stop"
        } else {
            stopBlinkingIndicator()
            binding.buttonStartStop.text = "Start"
        }

        attachClickListeners(timer)
        displayTimeRemaining(timer)
        setTimeDisplayColor(timer)
        setProgress(timer)
        setBackground(timer)
    }

    private fun attachClickListeners(timer: Timer) {
        binding.buttonStartStop.setOnClickListener {
            if (timer.isRunning) {
                controller.stopTimer(timer.id)
            } else {
                controller.startTimer(timer.id)
            }
        }

        binding.buttonDelete.setOnClickListener {
            controller.deleteTimer(timer.id)
        }
    }

    private fun displayTimeRemaining(timer: Timer) {
        val time = if (timer.hasFinished) timer.initialTime else timer.remainingTime
        binding.textTimeRemaining.text = formatTime(time)
    }

    private fun setTimeDisplayColor(timer: Timer) {
        val colorAttr = if (timer.hasFinished) R.attr.colorOnPrimary else R.attr.colorOnSurface
        val color = MaterialColors.getColor(binding.root, colorAttr)
        binding.textTimeRemaining.setTextColor(color)
    }

    private fun startBlinkingIndicator() {
        binding.blinkingIndicator.isInvisible = false
        (binding.blinkingIndicator.background as AnimationDrawable).start()
    }

    private fun stopBlinkingIndicator() {
        binding.blinkingIndicator.isInvisible = true
        (binding.blinkingIndicator.background as AnimationDrawable).stop()
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

    private fun formatTime(timeMillis: Long): String {
        fun formatLeadingZero(num: Long) = if (num < 10) "0$num" else num.toString()
        val hh = formatLeadingZero(timeMillis / 1000 / 3600)
        val mm = formatLeadingZero(timeMillis / 1000 % 3600 / 60)
        val ss = formatLeadingZero(timeMillis / 1000 % 60)
        return "$hh:$mm:$ss"
    }
}