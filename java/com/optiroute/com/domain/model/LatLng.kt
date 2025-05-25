package com.optiroute.com.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Representasi data class sederhana untuk koordinat geografis (Latitude dan Longitude).
 *
 * @property latitude Garis lintang, dalam derajat. Valid range: -90.0 hingga +90.0.
 * @property longitude Garis bujur, dalam derajat. Valid range: -180.0 hingga +180.0.
 *
 * Mengimplementasikan Parcelable agar mudah dilewatkan antar komponen Android
 * (misalnya, antar Composable dalam NavHost atau antar Activity/Fragment).
 */
@Parcelize // Otomatis mengimplementasikan Parcelable
data class LatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Parcelable {

    /**
     * Memeriksa apakah koordinat latitude dan longitude berada dalam rentang yang valid.
     * Latitude: -90 hingga +90 (inklusif).
     * Longitude: -180 hingga +180 (inklusif).
     *
     * @return True jika koordinat valid, false sebaliknya.
     */
    fun isValid(): Boolean {
        return latitude >= -90.0 && latitude <= 90.0 &&
                longitude >= -180.0 && longitude <= 180.0
    }

    companion object {
        /**
         * Lokasi default (0.0, 0.0) jika tidak ada lokasi spesifik yang diatur.
         * Bisa digunakan sebagai fallback atau nilai awal.
         */
        val DEFAULT = LatLng(0.0, 0.0)
    }

    /**
     * Representasi string kustom untuk logging atau tampilan debug.
     */
    override fun toString(): String {
        return "LatLng(lat=${String.format("%.6f", latitude)}, lng=${String.format("%.6f", longitude)})"
    }
}
