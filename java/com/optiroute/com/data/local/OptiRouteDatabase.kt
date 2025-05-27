package com.optiroute.com.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.optiroute.com.data.local.converter.LatLngConverter
import com.optiroute.com.data.local.dao.CustomerDao
import com.optiroute.com.data.local.dao.DepotDao
import com.optiroute.com.data.local.dao.VehicleDao
import com.optiroute.com.data.local.entity.CustomerEntity
import com.optiroute.com.data.local.entity.DepotEntity
import com.optiroute.com.data.local.entity.VehicleEntity
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * Kelas database utama untuk aplikasi OptiRoute menggunakan Room.
 *
 * Anotasi @Database mendefinisikan entitas yang termasuk dalam database dan versi database.
 * Setiap kali skema database diubah (misalnya, menambah kolom, mengubah tipe data),
 * versi database harus dinaikkan dan strategi migrasi harus disediakan.
 *
 * Anotasi @TypeConverters digunakan untuk mendaftarkan konverter tipe kustom.
 */
@Database(
    entities = [
        DepotEntity::class,
        VehicleEntity::class,
        CustomerEntity::class
    ],
    version = 1, // Versi awal database
    exportSchema = true // Disarankan true untuk produksi, false untuk pengembangan cepat
)
@TypeConverters(LatLngConverter::class)
abstract class OptiRouteDatabase : RoomDatabase() {

    abstract fun depotDao(): DepotDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun customerDao(): CustomerDao

    companion object {
        const val DATABASE_NAME = "optiroute_db"

        @Volatile
        private var INSTANCE: OptiRouteDatabase? = null

        fun getInstance(context: Context): OptiRouteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OptiRouteDatabase::class.java,
                    DATABASE_NAME
                )
                    // .addCallback(roomCallback) // Callback jika diperlukan untuk pre-populate
                    .fallbackToDestructiveMigration() // HATI-HATI: Hanya untuk pengembangan awal.
                    // Untuk rilis, sediakan strategi migrasi yang tepat.
                    // .setQueryCallback({ sqlQuery, bindArgs -> // Untuk logging query SQL jika perlu
                    //    Timber.tag("SQL_QUERY").v("Query: $sqlQuery Args: $bindArgs")
                    // }, Executors.newSingleThreadExecutor())
                    .build()
                INSTANCE = instance
                Timber.i("OptiRouteDatabase instance created or retrieved.")
                instance
            }
        }

        // Contoh callback untuk pre-populate database (jika diperlukan)
        private val roomCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Contoh: Isi data awal di sini menggunakan thread terpisah
                // Executors.newSingleThreadExecutor().execute {
                //    INSTANCE?.let { database ->
                //        // database.depotDao().upsertDepot(DepotEntity(...))
                //        Timber.d("Database pre-populated on create.")
                //    }
                // }
                Timber.d("OptiRouteDatabase onCreate callback triggered.")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Timber.d("OptiRouteDatabase onOpen callback triggered.")
            }
        }
    }
}
