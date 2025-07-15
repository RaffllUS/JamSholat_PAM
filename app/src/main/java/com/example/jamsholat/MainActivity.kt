package com.example.jamsholat

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.jamsholat.databinding.ActivityMainBinding
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler()
    private val prefs by lazy { getSharedPreferences("alarms", MODE_PRIVATE) }

    private var userLat = -6.9667
    private var userLng = 110.4167

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                "azan_channel",
                "Notifikasi Adzan",
                NotificationManager.IMPORTANCE_HIGH
            ).also { ch ->
                getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(ch)
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_kiblat -> {
                    Intent(this, KiblatActivity::class.java).apply {
                        putExtra("lat", userLat)
                        putExtra("lon", userLng)
                    }.also {
                        startActivity(it)
                        overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out)
                    }
                    true
                }
                R.id.nav_info -> true
                else -> false
            }
        }

        requestLocationPermission()
        updateCurrentTime()
        showRandomQuote()

        if (AlarmReceiver.mediaPlayer?.isPlaying == true) {
            showStopButton()
        }
    }

    private fun updateCurrentTime() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                binding.textCurrentPrayer.text =
                    SimpleDateFormat("HH:mm:ss 'WIB'", Locale.getDefault()).format(Date())
                handler.postDelayed(this, 1000)
            }
        }, 0)
    }

    private fun showRandomQuote() {
        binding.textQuote.text = listOf(
            "\u201cShalat adalah tiang agama.\u201d",
            "\u201cSesungguhnya shalat itu mencegah dari perbuatan keji dan mungkar.\u201d",
            "\u201cJangan tinggalkan shalat, karena itu cahaya hidupmu.\u201d"
        ).random()
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100
            )
        } else {
            getLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getLocation() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val loc: Location? = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (loc != null) {
            userLat = loc.latitude
            userLng = loc.longitude
        } else {
            binding.textCity.text = "Semarang (Default)"
        }

        try {
            val geo = Geocoder(this, Locale.getDefault())
            val list: List<Address>? = geo.getFromLocation(userLat, userLng, 1)
            binding.textCity.text = list?.firstOrNull()?.locality ?: "Tidak Diketahui"
        } catch (_: Exception) {
            binding.textCity.text = "Lokasi Tidak Dikenal"
        }

        binding.textDate.text = getHijriDate()

        val cal = Calendar.getInstance()
        val times = PrayerCalculator.calcPrayerTimes(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            userLng, userLat
        )

        val jadwal = listOf(
            Triple("Subuh", times[0], R.drawable.ic_subuh),
            Triple("Dzuhur", times[2], R.drawable.ic_dzuhur),
            Triple("Ashar", times[3], R.drawable.ic_ashar),
            Triple("Maghrib", times[4], R.drawable.ic_maghrib),
            Triple("Isya", times[5], R.drawable.ic_isya)
        )

        binding.layoutWaktuSholat.removeAllViews()
        jadwal.forEach { (name, time, icon) ->
            addPrayerCard(name, formatTime(time), icon)
        }

        if (AlarmReceiver.mediaPlayer?.isPlaying == true) {
            showStopButton()
        }
    }

    private fun addPrayerCard(title: String, time: String, iconRes: Int) {
        val isSet = prefs.contains(title)

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 16, 0, 0) }
            radius = 16f
            cardElevation = 6f
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 24, 24, 24)
            gravity = Gravity.CENTER_VERTICAL
        }

        val iv = ImageView(this).apply {
            setImageResource(iconRes)
            layoutParams = LinearLayout.LayoutParams(64, 64)
        }

        val tv = TextView(this).apply {
            text = "$title\n$time"
            textSize = 18f
            setPadding(16, 0, 0, 0)
        }

        val bell = ImageView(this).apply {
            setImageResource(if (isSet) R.drawable.ic_alarm_on else R.drawable.ic_alarm_off)
            layoutParams = LinearLayout.LayoutParams(48, 48).also {
                it.marginStart = 16
            }
            setColorFilter(ContextCompat.getColor(context, R.color.primary_green))
            setOnClickListener {
                if (prefs.contains(title)) {
                    cancelAdzanAlarm(title)
                    setImageResource(R.drawable.ic_alarm_off)
                    Toast.makeText(context, "Alarm $title dimatikan", Toast.LENGTH_SHORT).show()
                } else {
                    scheduleAdzanAlarm(title, time)
                    setImageResource(R.drawable.ic_alarm_on)
                    Toast.makeText(context, "Alarm $title di-set", Toast.LENGTH_SHORT).show()
                }
                if (AlarmReceiver.mediaPlayer?.isPlaying == true) {
                    showStopButton()
                }
            }
        }

        row.apply {
            addView(iv)
            addView(tv)
            addView(bell)
        }
        card.addView(row)
        binding.layoutWaktuSholat.addView(card)
    }

    private fun showStopButton() {
        val stopButton = ImageView(this).apply {
            setImageResource(R.drawable.ic_stop) // Ganti dengan ikon stop
            layoutParams = LinearLayout.LayoutParams(96, 96).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 32
            }
            setOnClickListener {
                val stopIntent = Intent(this@MainActivity, StopAdzanReceiver::class.java)
                sendBroadcast(stopIntent)
                Toast.makeText(this@MainActivity, "Adzan dihentikan", Toast.LENGTH_SHORT).show()
                (parent as? LinearLayout)?.removeView(this)
            }
        }
        binding.layoutWaktuSholat.addView(stopButton)
    }

    private fun scheduleAdzanAlarm(prayerName: String, timeStr: String) {
        val (h, m) = timeStr.split(":").map { it.toInt() }
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
        }
        val trigger = cal.timeInMillis

        prefs.edit().putLong(prayerName, trigger).apply()

        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("PRAYER_NAME", prayerName)
        }
        val pi = PendingIntent.getBroadcast(
            this,
            prayerName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
    }

    private fun cancelAdzanAlarm(prayerName: String) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            this,
            prayerName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
        prefs.edit().remove(prayerName).apply()
    }

    private fun formatTime(time: Double): String {
        val hh = floor(time).toInt()
        val mm = ((time - hh) * 60).roundToInt()
        return "%02d:%02d".format(hh, mm)
    }

    private fun getHijriDate(): String {
        val masehi = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id")).format(Date())
        val hijri = UmmalquraCalendar()
        val d = hijri.get(Calendar.DAY_OF_MONTH)
        val m = hijri.get(Calendar.MONTH)
        val y = hijri.get(Calendar.YEAR)
        val names = listOf(
            "Muharram","Safar","Rabiul Awal","Rabiul Akhir",
            "Jumadil Awal","Jumadil Akhir","Rajab","Sya'ban",
            "Ramadhan","Syawal","Dzulkaidah","Dzulhijjah"
        )
        return "$masehi / $d ${names[m]} $y H"
    }
}