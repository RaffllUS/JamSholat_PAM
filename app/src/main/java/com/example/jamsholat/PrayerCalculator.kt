package com.example.jamsholat

import kotlin.math.*

object PrayerCalculator {

    private fun degToRad(deg: Double) = Math.toRadians(deg)
    private fun radToDeg(rad: Double) = Math.toDegrees(rad)

    private fun safeAcos(x: Double): Double = acos(x.coerceIn(-1.0, 1.0))
    private fun normalize360(angle: Double): Double = (angle % 360.0).let { if (it < 0) it + 360 else it }

    /**
     * Menghitung waktu-waktu sholat:
     * [0] Fajr
     * [1] Sunrise
     * [2] Zuhr
     * [3] Asr
     * [4] Maghrib
     * [5] Isha
     */
    fun calcPrayerTimes(
        year: Int,
        month: Int,
        day: Int,
        longitude: Double,
        latitude: Double,
        timeZone: Int = 7,
        fajrAngle: Double = -18.0,
        ishaAngle: Double = -18.0
    ): DoubleArray {

        // Days since J2000.0
        val D = (367 * year) - ((year + ((month + 9) / 12)) * 7 / 4) + ((275 * month) / 9) + day - 730531.5

        val L = normalize360(280.461 + 0.9856474 * D) // Mean longitude
        val M = normalize360(357.528 + 0.9856003 * D) // Mean anomaly
        val lambda = normalize360(L + 1.915 * sin(degToRad(M)) + 0.02 * sin(degToRad(2 * M))) // Ecliptic longitude
        val obliquity = 23.439 - 0.0000004 * D // Obliquity of the ecliptic

        var alpha = radToDeg(atan2(cos(degToRad(obliquity)) * sin(degToRad(lambda)), cos(degToRad(lambda))))
        alpha = normalize360(alpha)

        val ST = normalize360(100.46 + 0.985647352 * D) // Sidereal time at Greenwich
        val decl = radToDeg(asin(sin(degToRad(obliquity)) * sin(degToRad(lambda)))) // Declination of sun

        val noon = normalize360(alpha - ST)
        val UTNoon = (noon - longitude) / 15.0
        val zuhr = UTNoon + timeZone

        // Durinal arc: waktu antara matahari terbit dan terbenam
        val hourAngle = safeAcos(
            (sin(degToRad(-0.8333)) - sin(degToRad(latitude)) * sin(degToRad(decl))) /
                    (cos(degToRad(latitude)) * cos(degToRad(decl)))
        )
        val duration = radToDeg(hourAngle) / 15.0

        val sunrise = zuhr - duration
        val maghrib = zuhr + duration

        // Asr (Shadow factor = 1)
        val asrAngle = radToDeg(atan(1.0 / (tan(abs(degToRad(latitude - decl))))))
        val asrHourAngle = safeAcos(
            (sin(degToRad(90 - asrAngle)) - sin(degToRad(latitude)) * sin(degToRad(decl))) /
                    (cos(degToRad(latitude)) * cos(degToRad(decl)))
        )
        val asr = zuhr + radToDeg(asrHourAngle) / 15.0

        // Isha
        val ishaArc = safeAcos(
            (sin(degToRad(ishaAngle)) - sin(degToRad(latitude)) * sin(degToRad(decl))) /
                    (cos(degToRad(latitude)) * cos(degToRad(decl)))
        )
        val isha = zuhr + radToDeg(ishaArc) / 15.0

        // Fajr
        val fajrArc = safeAcos(
            (sin(degToRad(fajrAngle)) - sin(degToRad(latitude)) * sin(degToRad(decl))) /
                    (cos(degToRad(latitude)) * cos(degToRad(decl)))
        )
        val fajr = zuhr - radToDeg(fajrArc) / 15.0

        return doubleArrayOf(fajr, sunrise, zuhr, asr, maghrib, isha)
    }
}