package com.example.pomodoro.utils

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import com.example.pomodoro.R

fun playAlertRingtone(context: Context) {
    val uri = Uri.parse("android.resource://com.example.pomodoro/${R.raw.alert}")
    RingtoneManager.getRingtone(context, uri).play()
}