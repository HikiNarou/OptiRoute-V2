package com.optiroute.com.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.optiroute.com.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) untuk entitas Vehicle.
 * Menyediakan metode untuk berinteraksi dengan tabel 'vehicles' di database.
 */
@Dao
interface VehicleDao {

    /**
     * Menyisipkan kendaraan baru ke dalam database.
     * Jika terjadi konflik (misalnya, ID yang sama, meskipun di sini autoGenerate),
     * operasi akan diabaikan (OnConflictStrategy.IGNORE).
     *
     * @param vehicle Entitas kendaraan yang akan disisipkan.
     * @return ID baris dari kendaraan yang baru disisipkan.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    /**
     * Memperbarui data kendaraan yang sudah ada di database.
     *
     * @param vehicle Entitas kendaraan dengan data yang diperbarui.
     */
    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    /**
     * Menghapus kendaraan dari database.
     *
     * @param vehicle Entitas kendaraan yang akan dihapus.
     */
    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)

    /**
     * Mengambil kendaraan berdasarkan ID-nya.
     * Mengembalikan Flow yang akan memancarkan data kendaraan setiap kali ada perubahan.
     *
     * @param vehicleId ID kendaraan yang akan diambil.
     * @return Flow yang berisi VehicleEntity, atau null jika tidak ditemukan.
     */
    @Query("SELECT * FROM vehicles WHERE id = :vehicleId")
    fun getVehicleById(vehicleId: Int): Flow<VehicleEntity?>

    /**
     * Mengambil semua kendaraan dari database.
     * Mengembalikan Flow yang akan memancarkan daftar kendaraan setiap kali ada perubahan.
     * Daftar diurutkan berdasarkan ID secara ascending.
     *
     * @return Flow yang berisi daftar semua VehicleEntity.
     */
    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    /**
     * Menghitung jumlah kendaraan yang ada di database.
     * Mengembalikan Flow yang akan memancarkan jumlah kendaraan.
     *
     * @return Flow yang berisi jumlah kendaraan.
     */
    @Query("SELECT COUNT(*) FROM vehicles")
    fun getVehiclesCount(): Flow<Int>

    /**
     * Menghapus semua kendaraan dari tabel.
     * Berguna untuk fungsionalitas reset data.
     */
    @Query("DELETE FROM vehicles")
    suspend fun clearAllVehicles()

    /**
     * Mengambil daftar kendaraan berdasarkan ID yang diberikan.
     * Berguna saat pengguna memilih kendaraan tertentu untuk perencanaan rute.
     *
     * @param vehicleIds Daftar ID kendaraan yang akan diambil.
     * @return Flow yang berisi daftar VehicleEntity yang cocok.
     */
    @Query("SELECT * FROM vehicles WHERE id IN (:vehicleIds)")
    fun getVehiclesByIds(vehicleIds: List<Int>): Flow<List<VehicleEntity>>
}
