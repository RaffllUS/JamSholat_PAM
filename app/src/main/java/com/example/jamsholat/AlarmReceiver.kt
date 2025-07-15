package com.example.jamsholat

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        var mediaPlayer: MediaPlayer? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra("PRAYER_NAME") ?: "Waktu Sholat"

        // Cegah adzan diputar lebih dari sekali
        if (mediaPlayer?.isPlaying == true) return

        // Mainkan adzan.mp3
        mediaPlayer = MediaPlayer.create(context, R.raw.adzan)?.apply {
            isLooping = false // hanya sekali putar
            start()

            // Lepaskan resource setelah selesai
            setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }
        }

        // Intent untuk tombol STOP
        val stopIntent = Intent(context, StopAdzanReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tampilkan notifikasi dengan tombol STOP
        val notification = NotificationCompat.Builder(context, "azan_channel")
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Adzan $prayer")
            .setContentText("Ketuk STOP untuk menghentikan suara")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_stop, "STOP", stopPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(12345, notification)
    }
}