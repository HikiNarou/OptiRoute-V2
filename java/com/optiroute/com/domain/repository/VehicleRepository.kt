package com.optiroute.com.domain.repository

import com.optiroute.com.data.local.entity.VehicleEntity
import com.optiroute.com.domain.model.AppResult // Impor AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Interface untuk VehicleRepository.
 * Mendefinisikan kontrak untuk operasi terkait data kendaraan.
 */
interface VehicleRepository {

    /**
     * Mengambil semua kendaraan.
     *
     * @return Flow yang memancarkan daftar VehicleEntity.
     */
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    /**
     * Mengambil kendaraan berdasarkan ID.
     *
     * @param vehicleId ID kendaraan.
     * @return Flow yang memancarkan VehicleEntity, atau null jika tidak ditemukan.
     */
    fun getVehicleById(vehicleId: Int): Flow<VehicleEntity?>

    /**
     * Menambahkan kendaraan baru.
     *
     * @param vehicle Entitas kendaraan yang akan ditambahkan.
     * @return AppResult yang berisi ID kendaraan yang baru ditambahkan jika berhasil.
     */
    suspend fun addVehicle(vehicle: VehicleEntity): AppResult<Long>

    /**
     * Memperbarui kendaraan yang sudah ada.
     *
     * @param vehicle Entitas kendaraan yang akan diperbarui.
     * @return AppResult yang menandakan keberhasilan.
     */
    suspend fun updateVehicle(vehicle: VehicleEntity): AppResult<Unit>

    /**
     * Menghapus kendaraan.
     *
     * @param vehicle Entitas kendaraan yang akan dihapus.
     * @return AppResult yang menandakan keberhasilan.
     */
    suspend fun deleteVehicle(vehicle: VehicleEntity): AppResult<Unit>

    /**
     * Mendapatkan jumlah total kendaraan.
     *
     * @return Flow yang memancarkan jumlah kendaraan.
     */
    fun getVehiclesCount(): Flow<Int>

    /**
     * Menghapus semua kendaraan (untuk reset).
     * @return AppResult yang menandakan keberhasilan.
     */
    suspend fun clearAllVehicles(): AppResult<Unit>

    /**
     * Mengambil daftar kendaraan berdasarkan ID yang diberikan.
     *
     * @param vehicleIds Daftar ID kendaraan.
     * @return Flow yang memancarkan daftar VehicleEntity yang cocok.
     */
    fun getVehiclesByIds(vehicleIds: List<Int>): Flow<List<VehicleEntity>>
}
