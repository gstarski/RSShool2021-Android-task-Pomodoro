package com.example.pomodoro

data class DisplayTime(val hours: Int, val minutes: Int, val seconds: Int) {
    override fun toString(): String {
        return listOf(hours, minutes, seconds).joinToString(":") { formatLeadingZero(it) }
    }

    companion object {
        fun fromMillis(millis: Long): DisplayTime {
            val hours = (millis / 1000 / 3600).toInt()
            val minutes = (millis / 1000 % 3600 / 60).toInt()
            val seconds = (millis / 1000 % 60).toInt()
            return DisplayTime(hours, minutes, seconds)
        }
    }
}

fun formatLeadingZero(num: Int): String {
    return if (num < 10) "0$num" else num.toString()
}

fun formatLeadingZero(num: Long): String {
    return if (num < 10) "0$num" else num.toString()
}

fun formatTime(timeMillis: Long): String {
    val hh = formatLeadingZero(timeMillis / 1000 / 3600)
    val mm = formatLeadingZero(timeMillis / 1000 % 3600 / 60)
    val ss = formatLeadingZero(timeMillis / 1000 % 60)
    return "$hh:$mm:$ss"
}
