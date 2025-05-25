package com.optiroute.com.domain.repository

import com.optiroute.com.data.local.entity.DepotEntity
import com.optiroute.com.domain.model.LatLng
import com.optiroute.com.domain.model.AppResult // Impor AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Interface untuk DepotRepository.
 * Mendefinisikan kontrak untuk operasi terkait data depot.
 * Ini mengabstraksi sumber data dari use case atau ViewModel.
 */
interface DepotRepository {

    /**
     * Mengambil data depot.
     *
     * @return Flow yang memancarkan DepotEntity, atau null jika tidak ada depot.
     */
    fun getDepot(): Flow<DepotEntity?>

    /**
     * Menyimpan atau memperbarui data depot.
     *
     * @param name Nama depot.
     * @param location Lokasi (LatLng) depot.
     * @param address Alamat depot (opsional).
     * @param notes Catatan untuk depot (opsional).
     * @return AppResult<Unit> yang menandakan keberhasilan operasi.
     */
    suspend fun saveDepot(name: String, location: LatLng, address: String?, notes: String?): AppResult<Unit>

    /**
     * Menghapus data depot.
     * @return AppResult<Unit> yang menandakan keberhasilan.
     */
    suspend fun deleteDepot(): AppResult<Unit>

    /**
     * Menghapus semua data depot (untuk reset).
     * @return AppResult<Unit> yang menandakan keberhasilan.
     */
    suspend fun clearAllDepots(): AppResult<Unit>
}
