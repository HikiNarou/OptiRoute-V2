package com.optiroute.com.di

import android.content.Context
import com.optiroute.com.data.local.OptiRouteDatabase
import com.optiroute.com.data.local.dao.CustomerDao
import com.optiroute.com.data.local.dao.DepotDao
import com.optiroute.com.data.local.dao.VehicleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module untuk menyediakan dependensi terkait database.
 * Modul ini bertanggung jawab untuk membuat dan menyediakan instance singleton
 * dari database dan DAO (Data Access Objects) terkait.
 */
@Module
@InstallIn(SingletonComponent::class) // Dependensi akan hidup selama aplikasi berjalan
object DatabaseModule {

    /**
     * Menyediakan instance singleton dari OptiRouteDatabase.
     * Menggunakan @ApplicationContext untuk mendapatkan context aplikasi.
     *
     * @param context Context aplikasi.
     * @return Instance singleton dari OptiRouteDatabase.
     */
    @Singleton
    @Provides
    fun provideOptiRouteDatabase(@ApplicationContext context: Context): OptiRouteDatabase {
        return OptiRouteDatabase.getInstance(context)
    }

    /**
     * Menyediakan instance dari DepotDao.
     * Hilt akan secara otomatis menyediakan instance OptiRouteDatabase yang dibutuhkan.
     *
     * @param database Instance OptiRouteDatabase.
     * @return Instance dari DepotDao.
     */
    @Provides
    @Singleton // DAO biasanya singleton karena terikat dengan database singleton
    fun provideDepotDao(database: OptiRouteDatabase): DepotDao {
        return database.depotDao()
    }

    /**
     * Menyediakan instance dari VehicleDao.
     *
     * @param database Instance OptiRouteDatabase.
     * @return Instance dari VehicleDao.
     */
    @Provides
    @Singleton
    fun provideVehicleDao(database: OptiRouteDatabase): VehicleDao {
        return database.vehicleDao()
    }

    /**
     * Menyediakan instance dari CustomerDao.
     *
     * @param database Instance OptiRouteDatabase.
     * @return Instance dari CustomerDao.
     */
    @Provides
    @Singleton
    fun provideCustomerDao(database: OptiRouteDatabase): CustomerDao {
        return database.customerDao()
    }
}
