package com.example.pomodoro

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.example.pomodoro.databinding.ItemTimerBinding
import com.example.pomodoro.model.Timer

class TimerAdapter(
    private val timersManager: TimersManager
) : ListAdapter<Timer, TimerViewHolder>(itemComparator) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTimerBinding.inflate(inflater, parent, false)
        return TimerViewHolder(binding, timersManager)
    }

    override fun onBindViewHolder(holder: TimerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: TimerViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)
    }

    private companion object {
        private val itemComparator = object : DiffUtil.ItemCallback<Timer>() {
            override fun areItemsTheSame(oldItem: Timer, newItem: Timer): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Timer, newItem: Timer): Boolean {
                return oldItem.isRunning == newItem.isRunning
                        && oldItem.elapsedTime == newItem.elapsedTime
                        && oldItem.initialTime == newItem.initialTime
            }

        }
    }
}
