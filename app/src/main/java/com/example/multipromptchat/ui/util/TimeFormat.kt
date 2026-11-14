package com.example.multipromptchat.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Compact, chat-list friendly timestamps (minSdk 26 → java.time is available). */
object TimeFormat {

    private val hourMinute: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val monthDay: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")
    private val fullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")

    fun chatTimestamp(millis: Long?, now: Long = System.currentTimeMillis()): String {
        if (millis == null || millis <= 0L) return ""
        val zone = ZoneId.systemDefault()
        val time = Instant.ofEpochMilli(millis).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone)
        return when {
            time.toLocalDate() == today.toLocalDate() -> time.format(hourMinute)
            time.toLocalDate() == today.minusDays(1).toLocalDate() -> "昨天"
            time.year == today.year -> time.format(monthDay)
            else -> time.format(fullDate)
        }
    }

    fun messageTime(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val time: ZonedDateTime = Instant.ofEpochMilli(millis).atZone(zone)
        return time.format(hourMinute)
    }
}
