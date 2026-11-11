package com.carrot233.travelmemory.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val stampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun nowStamp(): String = LocalDateTime.now().format(stampFormatter)

fun String?.blankToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

fun displayDate(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return ""
    return value.replace('T', ' ').take(16)
}

fun formatDuration(seconds: Int?): String {
    if (seconds == null || seconds < 0) return ""
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remain = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, remain)
    } else {
        "%d:%02d".format(minutes, remain)
    }
}

fun formatCoord(value: Double?): String = value?.let { "%.4f".format(it) }.orEmpty()

fun mimeForPath(path: String): String = when (path.substringAfterLast('.').lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "mp4" -> "video/mp4"
    "webm" -> "video/webm"
    "3gp" -> "video/3gpp"
    else -> "application/octet-stream"
}
