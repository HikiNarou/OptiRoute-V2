package com.optiroute.com.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.optiroute.com.domain.model.LatLng

/**
 * Entitas Room yang merepresentasikan depot (titik awal dan akhir pengiriman).
 *
 * @property id ID unik untuk depot. Karena biasanya hanya ada satu depot, kita bisa menggunakan ID tetap.
 * @property name Nama depot (misalnya, "Gudang Utama").
 * @property location Koordinat geografis depot. Menggunakan @Embedded untuk LatLng.
 * @property address Alamat depot (opsional).
 * @property notes Catatan tambahan untuk depot (opsional).
 */
@Entity(tableName = "depots")
data class DepotEntity(
    @PrimaryKey
    val id: Int = DEFAULT_DEPOT_ID, // ID default untuk depot tunggal
    val name: String,
    @Embedded(prefix = "location_") // Awalan untuk menghindari konflik nama kolom jika LatLng juga punya 'name' dll.
    val location: LatLng,
    val address: String? = null,
    val notes: String? = null
) {
    companion object {
        const val DEFAULT_DEPOT_ID = 1 // ID Konstan untuk depot
    }
}
