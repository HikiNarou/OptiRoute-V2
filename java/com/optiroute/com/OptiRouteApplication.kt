package com.optiroute.com

import android.app.Application
import android.content.pm.PackageManager
import com.google.android.libraries.places.api.Places
import com.optiroute.com.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Kelas Aplikasi kustom untuk OptiRoute.
 *
 * Anotasi @HiltAndroidApp mengaktifkan injeksi dependensi Hilt di seluruh aplikasi.
 * Kelas ini merupakan titik masuk utama untuk aplikasi dan digunakan untuk inisialisasi
 * pustaka atau konfigurasi tingkat aplikasi, seperti Timber untuk logging dan Google Places SDK.
 */
@HiltAndroidApp
class OptiRouteApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inisialisasi Timber untuk logging.
        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    // Membuat tag log yang lebih informatif, menyertakan nama file dan nomor baris.
                    return "OptiRouteApp (${element.fileName}:${element.lineNumber})#${element.methodName}"
                }
            })
            Timber.d("OptiRouteApplication onCreate: Timber initialized for DEBUG build.")
        } else {
            // Untuk build RILIS, Anda mungkin ingin menanam Tree yang berbeda,
            // misalnya untuk melaporkan error ke layanan seperti Firebase Crashlytics.
            // Timber.plant(CrashReportingTree()) // Contoh jika menggunakan Crashlytics
            Timber.i("OptiRouteApplication onCreate: Production build, Timber configured for release (if any).")
        }

        // =====================================================================================
        // PERBAIKAN KRUSIAL: Inisialisasi Google Places SDK
        // =====================================================================================
        try {
            // Mengambil API Key dari metadata AndroidManifest.xml
            // Ini adalah cara yang lebih aman daripada hardcoding langsung di sini.
            // Pastikan meta-data 'com.google.android.geo.API_KEY' ada di AndroidManifest.xml
            val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY")

            if (apiKey.isNullOrEmpty()) {
                Timber.e("Google Maps API Key is missing or empty in AndroidManifest.xml. Places SDK will not be initialized.")
                // Anda bisa melempar exception di sini atau menangani kasus ini sesuai kebutuhan aplikasi
                // throw IllegalStateException("Google Maps API Key is missing from AndroidManifest.xml")
            } else {
                if (!Places.isInitialized()) {
                    Places.initialize(applicationContext, apiKey)
                    Timber.i("Google Places SDK initialized successfully with API Key from Manifest.")
                } else {
                    Timber.i("Google Places SDK was already initialized.")
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "Failed to load meta-data, NameNotFound: ${e.message}. Places SDK may not be initialized.")
            // Handle error jika meta-data tidak ditemukan
        } catch (e: NullPointerException) {
            Timber.e(e, "Failed to load meta-data, NullPointer: ${e.message}. API Key might be missing. Places SDK may not be initialized.")
            // Handle error jika API Key null dari meta-data
        } catch (e: Exception) {
            Timber.e(e, "An unexpected error occurred during Places SDK initialization.")
            // Handle error umum lainnya
        }
        // =====================================================================================
        // AKHIR DARI PERBAIKAN KRUSIAL
        // =====================================================================================


        Timber.i("OptiRouteApplication instance created. App Version: ${BuildConfig.VERSION_NAME} (Code: ${BuildConfig.VERSION_CODE})")
    }

    /**
     * Contoh Tree untuk Crashlytics (jika Anda menggunakannya di masa mendatang).
     * Ini hanya contoh dan tidak akan digunakan kecuali Anda mengintegrasikan Crashlytics.
     */
    // private class CrashReportingTree : Timber.Tree() {
    //     override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    //         if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
    //             return // Jangan log VERBOSE atau DEBUG ke Crashlytics
    //         }
    //
    //         val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
    //         crashlytics.log(message) // Log pesan kustom
    //
    //         if (t != null) {
    //             if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
    //                 crashlytics.recordException(t) // Catat exception
    //             }
    //         }
    //     }
    // }
}
