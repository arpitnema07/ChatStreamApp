package com.example.chatstreamapp.utils

import android.view.View
import io.getstream.chat.android.client.models.Message
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

const val STAR = "star"

fun formatDate(date: Date): String {
    val now = Date()
    val diffInMillis = now.time - date.time
    val diffInMinutes = diffInMillis / (1000 * 60)

    return if (diffInMinutes < 1) {
        "just now"
    } else {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
    }
}

fun formatFileSize(size: Int): String {
    if (size <= 0) {
        return "0 B"
    }
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return String.format("%.2f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}


interface MessageOnClick {
    fun onSentItemLongPress(message: Message, view: View)
    fun onReceiveItemLongPress(message: Message, view: View)
    fun onClickAudio(message: Message,view: View)
}