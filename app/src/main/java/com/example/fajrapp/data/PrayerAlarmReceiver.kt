package com.example.fajrapp.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.fajrapp.MainActivity
import com.example.fajrapp.R
import com.example.fajrapp.model.PrayerAlarm

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PrayerAlarmScheduler.ACTION_PRAYER_ALARM) return

        val alarmId = intent.getIntExtra(PrayerAlarmScheduler.EXTRA_ALARM_ID, -1)
        if (alarmId < 0) return

        val prefs = PreferencesManager(context)
        val alarm = prefs.getPrayerAlarms().firstOrNull { it.id == alarmId && it.enabled } ?: return

        val channelId = createAlarmChannel(context, alarm)
        showAlarmNotification(context, alarm, channelId)

        PrayerAlarmScheduler(context).schedule(
            alarm = alarm,
            nowMillis = System.currentTimeMillis() + 60_000L
        )
    }

    @SuppressLint("MissingPermission")
    private fun showAlarmNotification(context: Context, alarm: PrayerAlarm, channelId: String) {
        val openAppIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prayerName = prayerNameForKey(context, alarm.prayerKey)
        val soundUri = alarm.ringtoneUri?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(context.getString(R.string.alarm_notification_title))
            .setContentText(context.getString(R.string.alarm_notification_text, prayerName))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setSound(soundUri)
            .build()

        if (!canPostNotifications(context)) return
        runCatching {
            NotificationManagerCompat.from(context).notify(alarm.id + NOTIFICATION_ID_BASE, notification)
        }.onFailure {
            if (it !is SecurityException) throw it
        }
    }

    private fun createAlarmChannel(context: Context, alarm: PrayerAlarm): String {
        val channelId = "${CHANNEL_ID}_${alarm.id}"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(channelId)

        val soundUri = alarm.ringtoneUri?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.alarm_channel_description)
            setSound(soundUri, audioAttributes)
            enableVibration(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
        return channelId
    }

    private fun prayerNameForKey(context: Context, key: String): String {
        return when (key) {
            "fajr" -> context.getString(R.string.prayer_fajr)
            "duha" -> context.getString(R.string.prayer_sunrise)
            "dhuhr" -> context.getString(R.string.prayer_dhuhr)
            "asr" -> context.getString(R.string.prayer_asr)
            "maghrib" -> context.getString(R.string.prayer_maghrib)
            "isha" -> context.getString(R.string.prayer_isha)
            "tahajjud" -> context.getString(R.string.prayer_tahajjud)
            else -> key
        }
    }

    companion object {
        private const val CHANNEL_ID = "prayer_alarm_channel"
        private const val NOTIFICATION_ID_BASE = 6000
    }

    private fun canPostNotifications(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
