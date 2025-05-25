package com.optiroute.com.data.repository

import com.optiroute.com.data.local.dao.DepotDao
import com.optiroute.com.data.local.entity.DepotEntity
import com.optiroute.com.domain.model.AppResult
import com.optiroute.com.domain.model.LatLng
import com.optiroute.com.domain.repository.DepotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementasi konkret dari DepotRepository.
 * Mengelola operasi data untuk entitas Depot, berinteraksi dengan DepotDao.
 *
 * @param depotDao DAO untuk akses data depot.
 */
@Singleton
class DepotRepositoryImpl @Inject constructor(
    private val depotDao: DepotDao
) : DepotRepository {

    override fun getDepot(): Flow<DepotEntity?> {
        Timber.d("DepotRepository: Getting depot from DAO.")
        return depotDao.getDepot().flowOn(Dispatchers.IO)
    }

    override suspend fun saveDepot(
        name: String,
        location: LatLng,
        address: String?,
        notes: String?
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Validasi dasar sebelum menyimpan
            if (name.isBlank()) {
                Timber.w("DepotRepository: Save depot failed - name is blank.")
                return@withContext AppResult.Error(IllegalArgumentException("Nama depot tidak boleh kosong."), "Nama depot tidak boleh kosong.")
            }
            if (!location.isValid()) {
                Timber.w("DepotRepository: Save depot failed - location is invalid.")
                return@withContext AppResult.Error(IllegalArgumentException("Lokasi depot tidak valid."), "Lokasi depot tidak valid.")
            }

            val depotEntity = DepotEntity(
                id = DepotEntity.DEFAULT_DEPOT_ID, // Selalu gunakan ID default untuk depot tunggal
                name = name.trim(), // Trim spasi
                location = location,
                address = address?.trim()?.takeIf { it.isNotBlank() }, // Trim dan null jika kosong
                notes = notes?.trim()?.takeIf { it.isNotBlank() }
            )
            depotDao.upsertDepot(depotEntity)
            Timber.i("DepotRepository: Depot saved/updated successfully: %s", name)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "DepotRepository: Error saving/updating depot.")
            AppResult.Error(e, "Terjadi kesalahan saat menyimpan depot: ${e.localizedMessage}")
        }
    }

    override suspend fun deleteDepot(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Tidak perlu memeriksa apakah depot ada, DAO akan menangani jika tidak ada yang dihapus.
            depotDao.deleteDepot(DepotEntity.DEFAULT_DEPOT_ID) // Hapus berdasarkan ID default
            Timber.i("DepotRepository: Attempted to delete depot with default ID.")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "DepotRepository: Error deleting depot.")
            AppResult.Error(e, "Terjadi kesalahan saat menghapus depot: ${e.localizedMessage}")
        }
    }

    override suspend fun clearAllDepots(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            depotDao.clearAllDepots()
            Timber.i("DepotRepository: All depots cleared successfully.")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "DepotRepository: Error clearing all depots.")
            AppResult.Error(e, "Terjadi kesalahan saat membersihkan data depot: ${e.localizedMessage}")
        }
    }
}
