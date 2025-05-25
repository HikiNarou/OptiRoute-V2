package com.optiroute.com.data.repository

import com.optiroute.com.data.local.dao.VehicleDao
import com.optiroute.com.data.local.entity.VehicleEntity
import com.optiroute.com.domain.model.AppResult
import com.optiroute.com.domain.repository.VehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementasi konkret dari VehicleRepository.
 * Mengelola operasi data untuk entitas Kendaraan, berinteraksi dengan VehicleDao.
 *
 * @param vehicleDao DAO untuk akses data kendaraan.
 */
@Singleton
class VehicleRepositoryImpl @Inject constructor(
    private val vehicleDao: VehicleDao
) : VehicleRepository {

    override fun getAllVehicles(): Flow<List<VehicleEntity>> {
        Timber.d("VehicleRepository: Getting all vehicles from DAO.")
        return vehicleDao.getAllVehicles().flowOn(Dispatchers.IO)
    }

    override fun getVehicleById(vehicleId: Int): Flow<VehicleEntity?> {
        Timber.d("VehicleRepository: Getting vehicle by ID %d from DAO.", vehicleId)
        return vehicleDao.getVehicleById(vehicleId).flowOn(Dispatchers.IO)
    }

    override suspend fun addVehicle(vehicle: VehicleEntity): AppResult<Long> = withContext(Dispatchers.IO) {
        try {
            // Validasi dasar (nama, kapasitas, satuan) sebaiknya sudah ditangani di ViewModel atau Entity.
            if (vehicle.name.isBlank()) {
                Timber.w("VehicleRepository: Add vehicle failed - name is blank.")
                return@withContext AppResult.Error(IllegalArgumentException("Nama kendaraan tidak boleh kosong."), "Nama kendaraan tidak boleh kosong.")
            }
            if (vehicle.capacity <= 0) {
                Timber.w("VehicleRepository: Add vehicle failed - capacity is not positive for %s.", vehicle.name)
                return@withContext AppResult.Error(IllegalArgumentException("Kapasitas kendaraan harus positif."), "Kapasitas kendaraan harus positif.")
            }
            if (vehicle.capacityUnit.isBlank()) {
                Timber.w("VehicleRepository: Add vehicle failed - capacity unit is blank for %s.", vehicle.name)
                return@withContext AppResult.Error(IllegalArgumentException("Satuan kapasitas tidak boleh kosong."), "Satuan kapasitas tidak boleh kosong.")
            }

            val newId = vehicleDao.insertVehicle(vehicle)
            if (newId > 0) {
                Timber.i("VehicleRepository: Vehicle added successfully with ID %d: %s", newId, vehicle.name)
                AppResult.Success(newId)
            } else {
                Timber.w("VehicleRepository: Failed to add vehicle, DAO returned non-positive ID %d for %s.", newId, vehicle.name)
                AppResult.Error(Exception("Gagal menambahkan kendaraan, ID tidak valid dari DB."), "Gagal menambahkan kendaraan ke database.")
            }
        } catch (e: Exception) {
            Timber.e(e, "VehicleRepository: Error adding vehicle %s.", vehicle.name)
            AppResult.Error(e, "Terjadi kesalahan saat menambahkan kendaraan: ${e.localizedMessage}")
        }
    }

    override suspend fun updateVehicle(vehicle: VehicleEntity): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (vehicle.name.isBlank()) {
                Timber.w("VehicleRepository: Update vehicle failed - name is blank for ID %d.", vehicle.id)
                return@withContext AppResult.Error(IllegalArgumentException("Nama kendaraan tidak boleh kosong."), "Nama kendaraan tidak boleh kosong.")
            }
            if (vehicle.capacity <= 0) {
                Timber.w("VehicleRepository: Update vehicle failed - capacity is not positive for %s.", vehicle.name)
                return@withContext AppResult.Error(IllegalArgumentException("Kapasitas kendaraan harus positif."), "Kapasitas kendaraan harus positif.")
            }
            if (vehicle.capacityUnit.isBlank()) {
                Timber.w("VehicleRepository: Update vehicle failed - capacity unit is blank for %s.", vehicle.name)
                return@withContext AppResult.Error(IllegalArgumentException("Satuan kapasitas tidak boleh kosong."), "Satuan kapasitas tidak boleh kosong.")
            }

            vehicleDao.updateVehicle(vehicle)
            Timber.i("VehicleRepository: Vehicle updated successfully: ID %d, %s", vehicle.id, vehicle.name)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "VehicleRepository: Error updating vehicle %s.", vehicle.name)
            AppResult.Error(e, "Terjadi kesalahan saat memperbarui kendaraan: ${e.localizedMessage}")
        }
    }

    override suspend fun deleteVehicle(vehicle: VehicleEntity): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            vehicleDao.deleteVehicle(vehicle)
            Timber.i("VehicleRepository: Vehicle deleted successfully: ID %d, %s", vehicle.id, vehicle.name)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "VehicleRepository: Error deleting vehicle %s.", vehicle.name)
            AppResult.Error(e, "Terjadi kesalahan saat menghapus kendaraan: ${e.localizedMessage}")
        }
    }

    override fun getVehiclesCount(): Flow<Int> {
        Timber.d("VehicleRepository: Getting vehicles count from DAO.")
        return vehicleDao.getVehiclesCount().flowOn(Dispatchers.IO)
    }

    override suspend fun clearAllVehicles(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            vehicleDao.clearAllVehicles()
            Timber.i("VehicleRepository: All vehicles cleared successfully.")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "VehicleRepository: Error clearing all vehicles.")
            AppResult.Error(e, "Terjadi kesalahan saat membersihkan data kendaraan: ${e.localizedMessage}")
        }
    }

    override fun getVehiclesByIds(vehicleIds: List<Int>): Flow<List<VehicleEntity>> {
        if (vehicleIds.isEmpty()) {
            Timber.d("VehicleRepository: getVehiclesByIds called with empty list, returning empty Flow.")
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        Timber.d("VehicleRepository: Getting vehicles by IDs from DAO: %s", vehicleIds.joinToString())
        return vehicleDao.getVehiclesByIds(vehicleIds).flowOn(Dispatchers.IO)
    }
}
