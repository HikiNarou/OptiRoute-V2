package com.optiroute.com.domain.repository

import com.optiroute.com.data.local.entity.CustomerEntity
import com.optiroute.com.domain.model.AppResult // Impor AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Interface untuk CustomerRepository.
 * Mendefinisikan kontrak untuk operasi terkait data pelanggan.
 */
interface CustomerRepository {

    /**
     * Mengambil semua pelanggan.
     *
     * @return Flow yang memancarkan daftar CustomerEntity.
     */
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    /**
     * Mengambil pelanggan berdasarkan ID.
     *
     * @param customerId ID pelanggan.
     * @return Flow yang memancarkan CustomerEntity, atau null jika tidak ditemukan.
     */
    fun getCustomerById(customerId: Int): Flow<CustomerEntity?>

    /**
     * Menambahkan pelanggan baru.
     *
     * @param customer Entitas pelanggan yang akan ditambahkan.
     * @return AppResult yang berisi ID pelanggan yang baru ditambahkan jika berhasil.
     */
    suspend fun addCustomer(customer: CustomerEntity): AppResult<Long>

    /**
     * Memperbarui pelanggan yang sudah ada.
     *
     * @param customer Entitas pelanggan yang akan diperbarui.
     * @return AppResult yang menandakan keberhasilan.
     */
    suspend fun updateCustomer(customer: CustomerEntity): AppResult<Unit>

    /**
     * Menghapus pelanggan.
     *
     * @param customer Entitas pelanggan yang akan dihapus.
     * @return AppResult yang menandakan keberhasilan.
     */
    suspend fun deleteCustomer(customer: CustomerEntity): AppResult<Unit>

    /**
     * Mendapatkan jumlah total pelanggan.
     *
     * @return Flow yang memancarkan jumlah pelanggan.
     */
    fun getCustomersCount(): Flow<Int>

    /**
     * Menghapus semua pelanggan (untuk reset).
     * @return AppResult yang menandakan keberhasilan.
     */
    suspend fun clearAllCustomers(): AppResult<Unit>

    /**
     * Mengambil daftar pelanggan berdasarkan ID yang diberikan.
     *
     * @param customerIds Daftar ID pelanggan.
     * @return Flow yang memancarkan daftar CustomerEntity yang cocok.
     */
    fun getCustomersByIds(customerIds: List<Int>): Flow<List<CustomerEntity>>
}
