package com.optiroute.com.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.optiroute.com.domain.model.LatLng
import timber.log.Timber

/**
 * TypeConverter untuk Room Database.
 * Mengonversi objek LatLng kustom menjadi String (format JSON) untuk disimpan di database,
 * dan sebaliknya, mengonversi String kembali menjadi objek LatLng saat dibaca dari database.
 *
 * Ini diperlukan karena Room hanya dapat menyimpan tipe data primitif atau tipe yang dikenali secara default.
 * Penggunaan Gson untuk serialisasi/deserialisasi.
 */
class LatLngConverter {

    private val gson = Gson()

    /**
     * Mengonversi objek LatLng menjadi representasi String (JSON).
     *
     * @param latLng Objek LatLng yang akan dikonversi. Bisa null.
     * @return String JSON yang merepresentasikan LatLng, atau null jika inputnya null.
     */
    @TypeConverter
    fun fromLatLng(latLng: LatLng?): String? {
        return try {
            latLng?.let { gson.toJson(it) }
        } catch (e: Exception) {
            Timber.e(e, "Error serializing LatLng to JSON: $latLng")
            null // Kembalikan null jika terjadi error serialisasi
        }
    }

    /**
     * Mengonversi representasi String (JSON) kembali menjadi objek LatLng.
     *
     * @param jsonString String JSON yang akan dikonversi. Bisa null atau kosong.
     * @return Objek LatLng hasil konversi, atau null jika input String null, kosong, atau tidak valid.
     */
    @TypeConverter
    fun toLatLng(jsonString: String?): LatLng? {
        if (jsonString.isNullOrEmpty()) {
            return null
        }
        return try {
            // Mendefinisikan tipe target untuk deserialisasi Gson.
            val type = object : TypeToken<LatLng>() {}.type
            gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            Timber.e(e, "Error deserializing JSON to LatLng: $jsonString")
            null // Kembalikan null jika terjadi error deserialisasi
        }
    }
}
