package com.ytsave.app.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return if (unitIndex == 0) {
            "${size.toInt()} ${units[unitIndex]}"
        } else {
            String.format(Locale.US, "%.1f %s", size, units[unitIndex])
        }
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "0:00"
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%d:%02d", mins, secs)
        }
    }

    fun formatSpeed(bytesPerSecond: Double): String {
        return when {
            bytesPerSecond >= 1_000_000 -> String.format(Locale.US, "%.1f MB/s", bytesPerSecond / 1_000_000)
            bytesPerSecond >= 1_000 -> String.format(Locale.US, "%.0f KB/s", bytesPerSecond / 1_000)
            else -> String.format(Locale.US, "%.0f B/s", bytesPerSecond)
        }
    }

    fun formatEta(seconds: Int): String {
        if (seconds <= 0) return "--:--"
        val mins = seconds / 60
        val secs = seconds % 60
        return if (mins >= 60) {
            String.format(Locale.US, "%d:%02d:%02d", mins / 60, mins % 60, secs)
        } else {
            String.format(Locale.US, "%d:%02d", mins, secs)
        }
    }

    fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}
