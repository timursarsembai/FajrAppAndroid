package com.example.fajrapp.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerAlarmBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            PrayerAlarmScheduler(context).scheduleAllEnabled()
        }
    }
}

