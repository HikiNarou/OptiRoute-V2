package com.optiroute.com.data.repository

import com.optiroute.com.data.local.dao.CustomerDao
import com.optiroute.com.data.local.entity.CustomerEntity
import com.optiroute.com.domain.model.AppResult
import com.optiroute.com.domain.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementasi konkret dari CustomerRepository.
 * Mengelola operasi data untuk entitas Pelanggan, berinteraksi dengan CustomerDao.
 *
 * @param customerDao DAO untuk akses data pelanggan.
 */
@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao
) : CustomerRepository {

    override fun getAllCustomers(): Flow<List<CustomerEntity>> {
        Timber.d("CustomerRepository: Getting all customers from DAO.")
        return customerDao.getAllCustomers().flowOn(Dispatchers.IO)
    }

    override fun getCustomerById(customerId: Int): Flow<CustomerEntity?> {
        Timber.d("CustomerRepository: Getting customer by ID %d from DAO.", customerId)
        return customerDao.getCustomerById(customerId).flowOn(Dispatchers.IO)
    }

    override suspend fun addCustomer(customer: CustomerEntity): AppResult<Long> = withContext(Dispatchers.IO) {
        try {
            // Validasi dasar (nama, demand, lokasi) sebaiknya sudah ditangani di ViewModel atau Entity.
            // Di sini kita fokus pada interaksi DB.
            if (!customer.location.isValid()) {
                Timber.w("CustomerRepository: Add customer failed - location is invalid for %s.", customer.name)
                return@withContext AppResult.Error(IllegalArgumentException("Lokasi pelanggan tidak valid."), "Lokasi pelanggan tidak valid.")
            }
            if (customer.name.isBlank()) {
                Timber.w("CustomerRepository: Add customer failed - name is blank.")
                return@withContext AppResult.Error(IllegalArgumentException("Nama pelanggan tidak boleh kosong."), "Nama pelanggan tidak boleh kosong.")
            }
            if (customer.demand < 0) {
                Timber.w("CustomerRepository: Add customer failed - demand is negative for %s.", customer.name)
                return@withContext AppResult.Error(IllegalArgumentException("Permintaan pelanggan tidak boleh negatif."), "Permintaan pelanggan tidak boleh negatif.")
            }


            val newId = customerDao.insertCustomer(customer)
            if (newId > 0) {
                Timber.i("CustomerRepository: Customer added successfully with ID %d: %s", newId, customer.name)
                AppResult.Success(newId)
            } else {
                Timber.w("CustomerRepository: Failed to add customer, DAO returned non-positive ID %d for %s.", newId, customer.name)
                AppResult.Error(Exception("Gagal menambahkan pelanggan, ID tidak valid dari DB."), "Gagal menambahkan pelanggan ke database.")
            }
        } catch (e: Exception) {
            Timber.e(e, "CustomerRepository: Error adding customer %s.", customer.name)
            AppResult.Error(e, "Terjadi kesalahan saat menambahkan pelanggan: ${e.localizedMessage}")
        }
    }

    override suspend fun updateCustomer(customer: CustomerEntity): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!customer.location.isValid()) {
                Timber.w("CustomerRepository: Update customer failed - location is invalid for %s.", customer.name)
                return@withContext AppResult.Error(IllegalArgumentException("Lokasi pelanggan tidak valid."), "Lokasi pelanggan tidak valid.")
            }
            if (customer.name.isBlank()) {
                Timber.w("CustomerRepository: Update customer failed - name is blank for ID %d.", customer.id)
                return@withContext AppResult.Error(IllegalArgumentException("Nama pelanggan tidak boleh kosong."), "Nama pelanggan tidak boleh kosong.")
            }
            if (customer.demand < 0) {
                Timber.w("CustomerRepository: Update customer failed - demand is negative for %s.", customer.name)
                return@withContext AppResult.Error(IllegalArgumentException("Permintaan pelanggan tidak boleh negatif."), "Permintaan pelanggan tidak boleh negatif.")
            }

            customerDao.updateCustomer(customer)
            Timber.i("CustomerRepository: Customer updated successfully: ID %d, %s", customer.id, customer.name)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "CustomerRepository: Error updating customer %s.", customer.name)
            AppResult.Error(e, "Terjadi kesalahan saat memperbarui pelanggan: ${e.localizedMessage}")
        }
    }

    override suspend fun deleteCustomer(customer: CustomerEntity): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            customerDao.deleteCustomer(customer)
            Timber.i("CustomerRepository: Customer deleted successfully: ID %d, %s", customer.id, customer.name)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "CustomerRepository: Error deleting customer %s.", customer.name)
            AppResult.Error(e, "Terjadi kesalahan saat menghapus pelanggan: ${e.localizedMessage}")
        }
    }

    override fun getCustomersCount(): Flow<Int> {
        Timber.d("CustomerRepository: Getting customers count from DAO.")
        return customerDao.getCustomersCount().flowOn(Dispatchers.IO)
    }

    override suspend fun clearAllCustomers(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            customerDao.clearAllCustomers()
            Timber.i("CustomerRepository: All customers cleared successfully.")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "CustomerRepository: Error clearing all customers.")
            AppResult.Error(e, "Terjadi kesalahan saat membersihkan data pelanggan: ${e.localizedMessage}")
        }
    }

    override fun getCustomersByIds(customerIds: List<Int>): Flow<List<CustomerEntity>> {
        if (customerIds.isEmpty()) {
            Timber.d("CustomerRepository: getCustomersByIds called with empty list, returning empty Flow.")
            return kotlinx.coroutines.flow.flowOf(emptyList()) // Kembalikan Flow kosong jika list ID kosong
        }
        Timber.d("CustomerRepository: Getting customers by IDs from DAO: %s", customerIds.joinToString())
        return customerDao.getCustomersByIds(customerIds).flowOn(Dispatchers.IO)
    }
}
