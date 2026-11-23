package com.example.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object SypFormatter {
    private val usIntegerFormat: NumberFormat
        get() = NumberFormat.getIntegerInstance(Locale.US).apply {
            isGroupingUsed = true
        }

    fun format(amount: Long, showPlusSign: Boolean = false): String {
        val absFormatted = usIntegerFormat.format(abs(amount))
        val prefix = when {
            amount < 0 -> "-"
            amount > 0 && showPlusSign -> "+"
            else -> ""
        }
        return "$prefix$absFormatted ل.س"
    }

    fun formatNumberOnly(amount: Long): String {
        return usIntegerFormat.format(amount)
    }

    fun normalizeDigits(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            when (ch) {
                in '0'..'9' -> sb.append(ch)
                in '\u0660'..'\u0669' -> sb.append(('0'.code + (ch.code - '\u0660'.code)).toChar())
                in '\u06F0'..'\u06F9' -> sb.append(('0'.code + (ch.code - '\u06F0'.code)).toChar())
            }
        }
        return sb.toString()
    }

    fun parsePositiveWholeAmount(raw: String): Long? {
        val normalized = normalizeDigits(raw)
        if (normalized.isEmpty()) return null
        val value = normalized.toLongOrNull() ?: return null
        return if (value > 0L) value else null
    }
}
