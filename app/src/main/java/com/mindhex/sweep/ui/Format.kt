package com.mindhex.sweep.ui

import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

/** Human-readable file size, e.g. "1.4 GB". */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val i = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / 1024.0.pow(i.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[i])
}
