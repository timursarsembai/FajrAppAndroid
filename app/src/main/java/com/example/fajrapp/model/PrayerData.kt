package com.example.fajrapp.model

data class PrayerData(
    val key: String,
    val name: String,
    val arabicName: String,
    val time: String,
    val timeMillis: Long,
    val isNext: Boolean = false,
    val isPassed: Boolean = false
)
