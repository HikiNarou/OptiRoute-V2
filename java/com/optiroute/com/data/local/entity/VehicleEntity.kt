package com.optiroute.com.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas Room yang merepresentasikan kendaraan dalam armada.
 *
 * @property id ID unik yang dihasilkan secara otomatis untuk setiap kendaraan.
 * @property name Nama atau identifikasi kendaraan (misalnya, "Mobil Box 1", "Motor Cepat A").
 * @property capacity Kapasitas muatan kendaraan (misalnya, dalam kg, m³, atau unit barang).
 * Nilai ini harus positif.
 * @property capacityUnit Satuan untuk kapasitas (misalnya, "kg", "box", "item").
 * @property notes Catatan tambahan untuk kendaraan (opsional).
 */
@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val capacity: Double, // Menggunakan Double untuk fleksibilitas (misal 10.5 kg)
    val capacityUnit: String, // Satuan kapasitas, contoh: "kg", "box", "unit"
    val notes: String? = null
) {
    init {
        require(capacity > 0) { "Kapasitas kendaraan harus lebih besar dari 0" }
        require(name.isNotBlank()) { "Nama kendaraan tidak boleh kosong" }
        require(capacityUnit.isNotBlank()) { "Satuan kapasitas kendaraan tidak boleh kosong" }
    }
}
