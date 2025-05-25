package com.optiroute.com.di

import com.optiroute.com.data.repository.CustomerRepositoryImpl
import com.optiroute.com.data.repository.DepotRepositoryImpl
import com.optiroute.com.data.repository.VehicleRepositoryImpl
import com.optiroute.com.domain.repository.CustomerRepository
import com.optiroute.com.domain.repository.DepotRepository
import com.optiroute.com.domain.repository.VehicleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module untuk menyediakan implementasi Repository.
 * Modul ini menggunakan @Binds untuk memberitahu Hilt implementasi mana yang harus
 * digunakan ketika sebuah interface Repository diminta sebagai dependensi.
 *
 * Ini mempromosikan loose coupling dan memudahkan pengujian dengan mengganti implementasi
 * jika diperlukan (misalnya, dengan mock repository).
 */
@Module
@InstallIn(SingletonComponent::class) // Scope Singleton untuk repository
abstract class RepositoryModule {

    /**
     * Mengikat implementasi DepotRepositoryImpl ke interface DepotRepository.
     * Ketika DepotRepository diinjeksi, Hilt akan menyediakan instance dari DepotRepositoryImpl.
     *
     * @param depotRepositoryImpl Implementasi konkret dari DepotRepository.
     * @return Instance yang diikat ke interface DepotRepository.
     */
    @Binds
    @Singleton
    abstract fun bindDepotRepository(
        depotRepositoryImpl: DepotRepositoryImpl
    ): DepotRepository

    /**
     * Mengikat implementasi VehicleRepositoryImpl ke interface VehicleRepository.
     *
     * @param vehicleRepositoryImpl Implementasi konkret dari VehicleRepository.
     * @return Instance yang diikat ke interface VehicleRepository.
     */
    @Binds
    @Singleton
    abstract fun bindVehicleRepository(
        vehicleRepositoryImpl: VehicleRepositoryImpl
    ): VehicleRepository

    /**
     * Mengikat implementasi CustomerRepositoryImpl ke interface CustomerRepository.
     *
     * @param customerRepositoryImpl Implementasi konkret dari CustomerRepository.
     * @return Instance yang diikat ke interface CustomerRepository.
     */
    @Binds
    @Singleton
    abstract fun bindCustomerRepository(
        customerRepositoryImpl: CustomerRepositoryImpl
    ): CustomerRepository
}
