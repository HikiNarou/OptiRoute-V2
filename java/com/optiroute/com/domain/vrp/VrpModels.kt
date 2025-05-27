package com.optiroute.com.domain.vrp

import com.optiroute.com.data.local.entity.CustomerEntity
import com.optiroute.com.data.local.entity.VehicleEntity
import com.optiroute.com.domain.model.LatLng // Menggunakan model LatLng kustom

/**
 * Detail satu rute dalam solusi VRP.
 *
 * @property vehicle Kendaraan yang ditugaskan untuk rute ini.
 * @property stops Daftar pelanggan yang dikunjungi secara berurutan dalam rute ini (tidak termasuk depot).
 * @property totalDistance Total jarak tempuh untuk rute ini (termasuk dari/ke depot).
 * @property totalDemand Total permintaan yang dilayani oleh rute ini.
 * @property routePath Daftar LatLng yang merepresentasikan jalur rute (opsional, untuk penggambaran di peta).
 * Ini akan mencakup depot sebagai titik awal dan akhir jika diisi.
 */
data class RouteDetail(
    val vehicle: VehicleEntity,
    val stops: List<CustomerEntity>, // Hanya pelanggan, depot implisit
    val totalDistance: Double,
    val totalDemand: Double,
    // routePath bisa di-generate saat menampilkan di peta jika diperlukan,
    // atau diisi oleh algoritma jika menghasilkan path eksplisit.
    // Untuk CWS dasar, ini mungkin tidak langsung dihasilkan.
    val routePath: List<LatLng>? = null
)

/**
 * Solusi keseluruhan untuk Vehicle Routing Problem.
 *
 * @property routes Daftar semua rute yang dihasilkan.
 * @property unassignedCustomers Daftar pelanggan yang tidak dapat dimasukkan ke dalam rute mana pun.
 * @property totalOverallDistance Total jarak dari semua rute yang dihasilkan.
 * @property calculationTimeMillis Waktu yang dibutuhkan untuk menghitung solusi (dalam milidetik).
 * @property planId ID unik untuk rencana rute ini, berguna untuk identifikasi.
 */
data class VrpSolution(
    val routes: List<RouteDetail>,
    val unassignedCustomers: List<CustomerEntity>,
    val totalOverallDistance: Double,
    val calculationTimeMillis: Long?,
    val planId: String // ID unik untuk setiap solusi yang dihasilkan
)
