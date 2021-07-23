package com.example.pomodoro

fun formatTime(timeMillis: Long): String {
    fun formatLeadingZero(num: Long) = if (num < 10) "0$num" else num.toString()
    val hh = formatLeadingZero(timeMillis / 1000 / 3600)
    val mm = formatLeadingZero(timeMillis / 1000 % 3600 / 60)
    val ss = formatLeadingZero(timeMillis / 1000 % 60)
    return "$hh:$mm:$ss"
}
