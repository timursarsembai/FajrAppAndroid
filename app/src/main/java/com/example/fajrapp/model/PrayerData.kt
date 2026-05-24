package com.example.fajrapp.model

data class PrayerData(
    val name: String,
    val arabicName: String,
    val time: String,
    val isNext: Boolean = false,
    val isPassed: Boolean = false,
    val timeLeft: String? = null
)
