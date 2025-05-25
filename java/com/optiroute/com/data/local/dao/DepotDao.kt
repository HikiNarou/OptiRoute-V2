package com.optiroute.com.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.optiroute.com.data.local.entity.DepotEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) untuk entitas Depot.
 * Menyediakan metode untuk berinteraksi dengan tabel 'depots' di database.
 */
@Dao
interface DepotDao {

    /**
     * Menyisipkan atau memperbarui depot. Jika depot dengan ID yang sama sudah ada,
     * data akan diperbarui (operasi Upsert).
     *
     * @param depot Entitas depot yang akan disimpan atau diperbarui.
     */
    @Upsert
    suspend fun upsertDepot(depot: DepotEntity)

    /**
     * Mengambil data depot dari database.
     * Karena diasumsikan hanya ada satu depot, kita mengambil berdasarkan ID default.
     * Mengembalikan Flow yang akan memancarkan data depot setiap kali ada perubahan.
     *
     * @return Flow yang berisi DepotEntity, atau null jika tidak ada depot yang diatur.
     */
    @Query("SELECT * FROM depots WHERE id = :depotId LIMIT 1")
    fun getDepot(depotId: Int = DepotEntity.DEFAULT_DEPOT_ID): Flow<DepotEntity?>

    /**
     * Menghapus depot dari database.
     * Operasi ini mungkin jarang digunakan karena depot biasanya bersifat tetap.
     *
     * @param depot Entitas depot yang akan dihapus.
     */
    @Query("DELETE FROM depots WHERE id = :depotId")
    suspend fun deleteDepot(depotId: Int = DepotEntity.DEFAULT_DEPOT_ID)

    /**
     * Menghapus semua data depot dari tabel.
     * Berguna untuk fungsionalitas reset data.
     */
    @Query("DELETE FROM depots")
    suspend fun clearAllDepots()
}
