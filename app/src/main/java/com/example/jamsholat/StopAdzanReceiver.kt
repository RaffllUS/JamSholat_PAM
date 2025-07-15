package com.example.jamsholat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class StopAdzanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Hentikan dan lepas mediaPlayer jika sedang diputar
        AlarmReceiver.mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
            AlarmReceiver.mediaPlayer = null
        }

        // Hapus notifikasi adzan (ID 12345)
        NotificationManagerCompat.from(context).cancel(12345)
    }
}