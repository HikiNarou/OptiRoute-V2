package com.optiroute.com.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.optiroute.com.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) untuk entitas Customer.
 * Menyediakan metode untuk berinteraksi dengan tabel 'customers' di database.
 */
@Dao
interface CustomerDao {

    /**
     * Menyisipkan pelanggan baru ke dalam database.
     *
     * @param customer Entitas pelanggan yang akan disisipkan.
     * @return ID baris dari pelanggan yang baru disisipkan.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    /**
     * Memperbarui data pelanggan yang sudah ada di database.
     *
     * @param customer Entitas pelanggan dengan data yang diperbarui.
     */
    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    /**
     * Menghapus pelanggan dari database.
     *
     * @param customer Entitas pelanggan yang akan dihapus.
     */
    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    /**
     * Mengambil pelanggan berdasarkan ID-nya.
     *
     * @param customerId ID pelanggan yang akan diambil.
     * @return Flow yang berisi CustomerEntity, atau null jika tidak ditemukan.
     */
    @Query("SELECT * FROM customers WHERE id = :customerId")
    fun getCustomerById(customerId: Int): Flow<CustomerEntity?>

    /**
     * Mengambil semua pelanggan dari database.
     * Daftar diurutkan berdasarkan nama pelanggan secara ascending.
     *
     * @return Flow yang berisi daftar semua CustomerEntity.
     */
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    /**
     * Menghitung jumlah pelanggan yang ada di database.
     *
     * @return Flow yang berisi jumlah pelanggan.
     */
    @Query("SELECT COUNT(*) FROM customers")
    fun getCustomersCount(): Flow<Int>

    /**
     * Menghapus semua pelanggan dari tabel.
     * Berguna untuk fungsionalitas reset data.
     */
    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()

    /**
     * Mengambil daftar pelanggan berdasarkan ID yang diberikan.
     * Berguna saat pengguna memilih pelanggan tertentu untuk pengiriman.
     *
     * @param customerIds Daftar ID pelanggan yang akan diambil.
     * @return Flow yang berisi daftar CustomerEntity yang cocok.
     */
    @Query("SELECT * FROM customers WHERE id IN (:customerIds) ORDER BY name ASC")
    fun getCustomersByIds(customerIds: List<Int>): Flow<List<CustomerEntity>>
}
