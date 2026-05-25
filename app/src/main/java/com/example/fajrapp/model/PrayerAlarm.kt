package com.example.fajrapp.model

data class PrayerAlarm(
    val id: Int,
    val prayerKey: String,
    val offsetMinutes: Int,
    val enabled: Boolean = true,
    val repeatMode: String = RepeatMode.WEEKLY,
    val repeatDaysIso: Set<Int> = RepeatMode.ALL_WEEK_DAYS,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = ""
)

object RepeatMode {
    const val WEEKLY = "WEEKLY"
    const val CUSTOM_DAYS = "CUSTOM_DAYS"
    val ALL_WEEK_DAYS: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7) // ISO: 1=Mon..7=Sun
}
