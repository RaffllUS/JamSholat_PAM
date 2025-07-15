package com.example.jamsholat

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("prayer_alarms", Context.MODE_PRIVATE)
            val times = prefs.getStringSet("enabled_prayers", emptySet()) ?: emptySet()

            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            times.forEach { prayerName ->
                val triggerAtMillis = prefs.getLong("${prayerName}_time", -1L)
                if (triggerAtMillis > 0) {
                    val pi = PendingIntent.getBroadcast(
                        context,
                        prayerName.hashCode(),
                        Intent(context, AlarmReceiver::class.java).apply {
                            putExtra("PRAYER_NAME", prayerName)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                    PendingIntent.FLAG_IMMUTABLE else 0
                    )
                    alarmMgr.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pi
                    )
                }
            }
        }
    }
}