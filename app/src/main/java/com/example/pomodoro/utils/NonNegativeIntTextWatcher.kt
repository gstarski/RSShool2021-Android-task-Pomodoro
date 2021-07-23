package com.example.pomodoro.utils

import android.text.Editable
import android.text.TextWatcher

/**
 * Ensures that edited value is within range [0..max].
 */
class NonNegativeIntTextWatcher(max: Int = Int.MAX_VALUE) : TextWatcher {

    private val minValue: Int = 0
    private val maxValue: Int = max

    private var textBeforeChange = ""

    init {
        if (max < 0) {
            throw IllegalArgumentException("Max value can't be negative.")
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        textBeforeChange = s.toString()
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        if (s == null) {
            return
        }

        removeLeadingZeroes(s)

        if (s.isBlank()) {
            return
        }

        ensureWithinRange(s)
    }

    private fun removeLeadingZeroes(s: Editable) {
        val leadingZeroesCount = s.length - s.trimStart('0').length
        if (leadingZeroesCount > 0) {
            when {
                s.length == 1 -> return // "0" -> "0"
                leadingZeroesCount == s.length -> s.setValue(0) // "000" -> "0"
                else -> s.delete(0, leadingZeroesCount) // "007" -> "7"
            }
        }
    }

    private fun ensureWithinRange(s: Editable) {
        val number = s.toString().toIntOrNull()
        when {
            number == null -> rollbackChange(s)
            number < minValue -> s.setValue(minValue)
            number > maxValue -> s.setValue(maxValue)
        }
    }

    private fun rollbackChange(s: Editable) {
        s.replace(0, s.length, textBeforeChange)
    }

    private companion object {
        fun Editable.setValue(number: Int) = this.replace(0, this.length, number.toString())
    }
}
