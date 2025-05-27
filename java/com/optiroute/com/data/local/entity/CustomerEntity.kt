package com.optiroute.com.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.optiroute.com.domain.model.LatLng

/**
 * Entitas Room yang merepresentasikan pelanggan atau titik pengiriman.
 *
 * @property id ID unik yang dihasilkan secara otomatis untuk setiap pelanggan.
 * @property name Nama pelanggan atau titik pengiriman.
 * @property location Koordinat geografis pelanggan.
 * @property address Alamat pelanggan (opsional, bisa digunakan untuk tampilan).
 * @property demand Jumlah permintaan dari pelanggan (misalnya, dalam satuan yang sama dengan kapasitas kendaraan).
 * Nilai ini harus positif.
 * @property notes Catatan tambahan untuk pelanggan (opsional).
 */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    @Embedded(prefix = "location_")
    val location: LatLng,
    val address: String? = null,
    val demand: Double, // Menggunakan Double untuk fleksibilitas
    val notes: String? = null
) {
    init {
        require(demand >= 0) { "Permintaan pelanggan tidak boleh negatif (0 jika tidak ada permintaan khusus untuk dijemput)" } // Bisa 0 jika ini adalah titik jemput tanpa bawaan awal
        require(name.isNotBlank()) { "Nama pelanggan tidak boleh kosong" }
    }
}
