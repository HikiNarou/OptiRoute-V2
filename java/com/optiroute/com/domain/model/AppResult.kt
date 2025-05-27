package com.optiroute.com.domain.model

/**
 * Sealed class generik untuk merepresentasikan hasil dari suatu operasi,
 * yang bisa berupa Success (berhasil) atau Error (gagal).
 * Ini adalah pola umum untuk menangani hasil operasi yang mungkin gagal,
 * terutama dalam operasi I/O atau jaringan.
 *
 * @param T Tipe data dari hasil jika operasi berhasil.
 */
sealed class AppResult<out T> {
    /**
     * Merepresentasikan hasil operasi yang berhasil.
     * @property data Data hasil operasi dengan tipe T.
     */
    data class Success<out T>(val data: T) : AppResult<T>()

    /**
     * Merepresentasikan hasil operasi yang gagal.
     * @property exception Throwable yang menyebabkan kegagalan (opsional, tapi sangat berguna untuk debugging).
     * @property message Pesan error kustom yang bisa ditampilkan ke pengguna (opsional).
     * Jika null, pesan dari exception bisa digunakan atau pesan default.
     */
    data class Error(
        val exception: Throwable? = null, // Jadikan Throwable opsional jika kadang hanya pesan yang relevan
        val message: String? = null
    ) : AppResult<Nothing>() // Nothing karena tidak ada data yang berhasil dikembalikan
}
