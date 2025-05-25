package com.optiroute.com.domain.vrp

import com.optiroute.com.domain.model.LatLng // Menggunakan model LatLng kustom
import timber.log.Timber
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object DistanceUtils {
    private const val EARTH_RADIUS_KM = 6371.0088 // Radius rata-rata Bumi dalam kilometer (lebih presisi)

    /**
     * Menghitung jarak antara dua titik koordinat LatLng menggunakan formula Haversine.
     * Formula ini memperhitungkan kelengkungan Bumi dan lebih akurat untuk jarak jauh.
     *
     * @param point1 Titik pertama (LatLng).
     * @param point2 Titik kedua (LatLng).
     * @return Jarak dalam kilometer. Mengembalikan Double.MAX_VALUE jika koordinat tidak valid.
     */
    fun calculateHaversineDistance(point1: LatLng, point2: LatLng): Double {
        // Validasi input koordinat
        if (!point1.isValid() || !point2.isValid()) {
            Timber.w("Invalid coordinates for distance calculation: P1: (${point1.latitude}, ${point1.longitude}), P2: (${point2.latitude}, ${point2.longitude})")
            return Double.MAX_VALUE // Atau throw IllegalArgumentException
        }

        // Konversi derajat ke radian
        val lat1Rad = Math.toRadians(point1.latitude)
        val lon1Rad = Math.toRadians(point1.longitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val lon2Rad = Math.toRadians(point2.longitude)

        // Selisih latitude dan longitude
        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad

        // Formula Haversine
        val a = sin(dLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * Menghitung jarak Euclidean (aproksimasi untuk area geografis terbatas).
     * Ini lebih sederhana tetapi kurang akurat dibandingkan Haversine, terutama untuk jarak jauh.
     * PDF menyebutkan area terbatas, jadi ini bisa jadi alternatif jika Haversine terlalu berat,
     * namun Haversine modern cukup efisien.
     *
     * **Peringatan:** Akurasi menurun signifikan dengan meningkatnya jarak dan perbedaan lintang.
     * Sebaiknya gunakan Haversine untuk aplikasi nyata.
     *
     * @param point1 Titik pertama (LatLng).
     * @param point2 Titik kedua (LatLng).
     * @return Jarak perkiraan dalam kilometer. Mengembalikan Double.MAX_VALUE jika koordinat tidak valid.
     */
    fun calculateEuclideanDistanceApproximation(point1: LatLng, point2: LatLng): Double {
        if (!point1.isValid() || !point2.isValid()) {
            Timber.w("Invalid coordinates for Euclidean distance: P1: (${point1.latitude}, ${point1.longitude}), P2: (${point2.latitude}, ${point2.longitude})")
            return Double.MAX_VALUE
        }

        // Faktor konversi kasar: 1 derajat lintang ~ 111.132 km
        // 1 derajat bujur ~ 111.320 * cos(lintang_rata_rata) km
        val avgLatRad = Math.toRadians((point1.latitude + point2.latitude) / 2.0)
        val degLatToKm = 111.132
        val degLonToKm = 111.320 * cos(avgLatRad)

        val dx = (point1.longitude - point2.longitude) * degLonToKm
        val dy = (point1.latitude - point2.latitude) * degLatToKm

        return sqrt(dx.pow(2) + dy.pow(2))
    }

    // Sesuai PDF, kita menggunakan koordinat GPS statis. Haversine lebih cocok dan direkomendasikan.
    // Variabel ini bisa digunakan di seluruh aplikasi untuk konsistensi.
    val calculateDistance: (LatLng, LatLng) -> Double = ::calculateHaversineDistance
}
