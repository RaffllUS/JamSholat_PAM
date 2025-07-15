package com.example.jamsholat

import android.hardware.*
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.*

class KiblatActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var compassView: ImageView
    private lateinit var textDirection: TextView

    // Koordinat Ka'bah
    private val kaabaLat = 21.422487
    private val kaabaLng = 39.826206

    // Lokasi pengguna
    private var userLat = 0.0
    private var userLng = 0.0
    private var qiblaDirection = 0.0

    private var currentAzimuth = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kiblat)

        // Inisialisasi view
        compassView = findViewById(R.id.compassView)
        textDirection = findViewById(R.id.textDirection)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Ambil data lokasi dari Intent
        userLat = intent.getDoubleExtra("lat", 0.0)
        userLng = intent.getDoubleExtra("lon", 0.0)

        if (userLat == 0.0 && userLng == 0.0) {
            textDirection.text = "Arah Kiblat: Lokasi tidak tersedia"
        } else {
            qiblaDirection = calculateQiblaDirection(userLat, userLng)
            textDirection.text = "Arah Kiblat: %.2f°".format(qiblaDirection)
        }
    }

    override fun onResume() {
        super.onResume()
        // Sensor kompas (arah)
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val azimuth = event?.values?.get(0) ?: return
        val angleToQibla = (qiblaDirection - azimuth + 360) % 360

        val rotate = RotateAnimation(
            currentAzimuth,
            -angleToQibla.toFloat(),
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 500
            fillAfter = true
        }

        compassView.startAnimation(rotate)
        currentAzimuth = -angleToQibla.toFloat()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Tidak digunakan
    }

    // Fungsi untuk menghitung arah kiblat
    private fun calculateQiblaDirection(lat: Double, lng: Double): Double {
        val phiUser = Math.toRadians(lat)
        val lambdaUser = Math.toRadians(lng)
        val phiKaaba = Math.toRadians(kaabaLat)
        val lambdaKaaba = Math.toRadians(kaabaLng)

        val deltaLambda = lambdaKaaba - lambdaUser
        val y = sin(deltaLambda)
        val x = cos(phiUser) * tan(phiKaaba) - sin(phiUser) * cos(deltaLambda)
        val theta = atan2(y, x)

        return (Math.toDegrees(theta) + 360) % 360
    }
}