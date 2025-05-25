package com.optiroute.com.domain.vrp

import com.optiroute.com.data.local.entity.CustomerEntity
import com.optiroute.com.data.local.entity.DepotEntity
import com.optiroute.com.data.local.entity.VehicleEntity
import com.optiroute.com.domain.model.LatLng
import timber.log.Timber
import java.util.PriorityQueue
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.random.Random

/**
 * Implementasi algoritma Clarke & Wright Savings dengan penyempurnaan.
 * Termasuk optimasi 2-Opt dengan early termination dan strategi penugasan kendaraan
 * yang lebih mempertimbangkan bin-packing.
 */
@Singleton
class ClarkeWrightSavings @Inject constructor() {

    private data class RouteSegment(
        var customers: MutableList<CustomerEntity>,
        var currentDemand: Double,
        var currentDistance: Double, // Jarak internal segmen, tidak termasuk ke/dari depot
        var vehicleId: Int? = null // Kendaraan yang mungkin sudah ditugaskan
    ) {
        val firstCustomer: CustomerEntity? get() = customers.firstOrNull()
        val lastCustomer: CustomerEntity? get() = customers.lastOrNull()

        fun addCustomerToStart(customer: CustomerEntity, depotLocation: LatLng, distanceCalc: (LatLng, LatLng) -> Double) {
            if (customers.isNotEmpty()) {
                currentDistance += distanceCalc(customer.location, customers.first().location)
            }
            customers.add(0, customer)
            currentDemand += customer.demand
        }

        fun addCustomerToEnd(customer: CustomerEntity, depotLocation: LatLng, distanceCalc: (LatLng, LatLng) -> Double) {
            if (customers.isNotEmpty()) {
                currentDistance += distanceCalc(customers.last().location, customer.location)
            }
            customers.add(customer)
            currentDemand += customer.demand
        }

        fun mergeSegmentAtEnd(segmentToMerge: RouteSegment, connectingCustomerOfThis: CustomerEntity, connectingCustomerOfSegmentToMerge: CustomerEntity, distanceCalc: (LatLng, LatLng) -> Double) {
            // Pastikan urutan pelanggan di segmentToMerge benar jika perlu dibalik
            if (segmentToMerge.firstCustomer != connectingCustomerOfSegmentToMerge) {
                segmentToMerge.customers.reverse()
                // Hitung ulang jarak internal segmentToMerge jika dibalik (opsional, tergantung bagaimana currentDistance dihitung)
                // Untuk CWS dasar, jarak internal tidak selalu dilacak secara detail di RouteSegment
            }
            currentDistance += distanceCalc(connectingCustomerOfThis.location, segmentToMerge.firstCustomer!!.location) + segmentToMerge.currentDistance
            customers.addAll(segmentToMerge.customers)
            currentDemand += segmentToMerge.currentDemand
        }

        fun mergeSegmentAtStart(segmentToMerge: RouteSegment, connectingCustomerOfThis: CustomerEntity, connectingCustomerOfSegmentToMerge: CustomerEntity, distanceCalc: (LatLng, LatLng) -> Double) {
            if (segmentToMerge.lastCustomer != connectingCustomerOfSegmentToMerge) {
                segmentToMerge.customers.reverse()
            }
            currentDistance += distanceCalc(segmentToMerge.lastCustomer!!.location, connectingCustomerOfThis.location) + segmentToMerge.currentDistance
            customers.addAll(0, segmentToMerge.customers)
            currentDemand += segmentToMerge.currentDemand
        }


        fun calculateTotalDistanceWithDepot(depotLocation: LatLng, distanceCalc: (LatLng, LatLng) -> Double): Double {
            if (customers.isEmpty()) return 0.0
            var dist = distanceCalc(depotLocation, customers.first().location) // Depot to first
            dist += currentDistance // Jarak internal antar pelanggan
            dist += distanceCalc(customers.last().location, depotLocation) // Last to Depot
            return dist
        }
    }

    private data class Saving(
        val cust1Id: Int,
        val cust2Id: Int,
        val value: Double,
        val cust1: CustomerEntity, // Menyimpan referensi objek untuk kemudahan
        val cust2: CustomerEntity
    )

    // Konstanta untuk 2-Opt
    private companion object {
        const val TWO_OPT_MAX_ITERATIONS_NO_IMPROVEMENT = 100 // Iterasi maks tanpa perbaikan
        const val TWO_OPT_MIN_IMPROVEMENT_THRESHOLD = 0.01 // Perbaikan minimal agar dianggap signifikan
    }

    suspend fun solve(
        depot: DepotEntity,
        customers: List<CustomerEntity>,
        vehicles: List<VehicleEntity>
    ): VrpSolution {
        val startTime = System.currentTimeMillis()
        Timber.d("Clarke-Wright (V2): Starting. Customers: ${customers.size}, Vehicles: ${vehicles.size}")

        if (customers.isEmpty()) {
            return VrpSolution(emptyList(), emptyList(), 0.0, 0, UUID.randomUUID().toString())
        }
        if (vehicles.isEmpty()) {
            Timber.w("Clarke-Wright (V2): No vehicles provided. All customers will be unassigned.")
            return VrpSolution(emptyList(), customers, 0.0, 0, UUID.randomUUID().toString())
        }

        val depotLoc = depot.location
        val distFunc = DistanceUtils.calculateDistance

        // 0. Filter pelanggan yang demandnya melebihi kapasitas kendaraan terbesar
        val maxCapacityOfAnyVehicle = vehicles.maxOfOrNull { it.capacity } ?: 0.0
        val (serviceableCustomers, initiallyUnassignedCustomers) = customers.partition { it.demand <= maxCapacityOfAnyVehicle }
        val unassignedCustomersList = initiallyUnassignedCustomers.toMutableList()

        if (serviceableCustomers.isEmpty()) {
            Timber.w("Clarke-Wright (V2): No serviceable customers after capacity check.")
            return VrpSolution(emptyList(), unassignedCustomersList, 0.0, System.currentTimeMillis() - startTime, UUID.randomUUID().toString())
        }

        // 1. Hitung semua penghematan (savings)
        val savingsList = mutableListOf<Saving>()
        for (i in serviceableCustomers.indices) {
            for (j in (i + 1) until serviceableCustomers.size) {
                val cust1 = serviceableCustomers[i]
                val cust2 = serviceableCustomers[j]
                val savingVal = distFunc(depotLoc, cust1.location) + distFunc(depotLoc, cust2.location) - distFunc(cust1.location, cust2.location)
                if (savingVal > 0) { // Hanya pertimbangkan penghematan positif
                    savingsList.add(Saving(cust1.id, cust2.id, savingVal, cust1, cust2))
                }
            }
        }
        // Urutkan penghematan dari terbesar ke terkecil
        savingsList.sortByDescending { it.value }
        Timber.d("Calculated ${savingsList.size} positive savings for ${serviceableCustomers.size} serviceable customers.")

        // 2. Inisialisasi rute: setiap pelanggan dalam rute sendiri (RouteSegment)
        // Kunci: ID Pelanggan, Nilai: RouteSegment yang mengandung pelanggan tersebut
        val customerToRouteSegmentMap = serviceableCustomers.associateTo(HashMap()) { customer ->
            customer.id to RouteSegment(
                customers = mutableListOf(customer),
                currentDemand = customer.demand,
                currentDistance = 0.0 // Awalnya jarak internal 0
            )
        }

        // 3. Proses penggabungan berdasarkan penghematan
        for (saving in savingsList) {
            val routeI = customerToRouteSegmentMap[saving.cust1Id]
            val routeJ = customerToRouteSegmentMap[saving.cust2Id]

            // Lewati jika salah satu pelanggan sudah tidak ada di map (sudah digabung) atau mereka berada di rute yang sama
            if (routeI == null || routeJ == null || routeI === routeJ) continue

            val custI = saving.cust1
            val custJ = saving.cust2

            // Cek apakah penggabungan valid (kapasitas dan posisi)
            // Kondisi 1: custI adalah akhir dari routeI, dan custJ adalah awal dari routeJ
            if (routeI.lastCustomer == custI && routeJ.firstCustomer == custJ) {
                if (routeI.currentDemand + routeJ.currentDemand <= maxCapacityOfAnyVehicle) { // Cek dengan kapasitas terbesar dulu
                    routeI.mergeSegmentAtEnd(routeJ, custI, custJ, distFunc)
                    // Update map: semua pelanggan di routeJ sekarang menunjuk ke routeI
                    routeJ.customers.forEach { customerToRouteSegmentMap[it.id] = routeI }
                    customerToRouteSegmentMap.remove(saving.cust2Id) // Hapus entri lama untuk routeJ (cukup satu perwakilan)
                    Timber.v("Merged J to I (I_last, J_first): ${custI.name} -> ${custJ.name}")
                }
            }
            // Kondisi 2: custJ adalah akhir dari routeJ, dan custI adalah awal dari routeI
            else if (routeJ.lastCustomer == custJ && routeI.firstCustomer == custI) {
                if (routeJ.currentDemand + routeI.currentDemand <= maxCapacityOfAnyVehicle) {
                    routeJ.mergeSegmentAtEnd(routeI, custJ, custI, distFunc)
                    routeI.customers.forEach { customerToRouteSegmentMap[it.id] = routeJ }
                    customerToRouteSegmentMap.remove(saving.cust1Id)
                    Timber.v("Merged I to J (J_last, I_first): ${custJ.name} -> ${custI.name}")
                }
            }
            // Kondisi 3: custI adalah awal dari routeI, dan custJ adalah awal dari routeJ (perlu membalik salah satu)
            // Gabungkan routeJ (dibalik) ke awal routeI
            else if (routeI.firstCustomer == custI && routeJ.firstCustomer == custJ) {
                if (routeI.currentDemand + routeJ.currentDemand <= maxCapacityOfAnyVehicle) {
                    // routeJ akan dibalik sehingga custJ menjadi akhir
                    routeI.mergeSegmentAtStart(routeJ, custI, custJ, distFunc) // custJ adalah lastCustomer dari routeJ (setelah dibalik)
                    routeJ.customers.forEach { customerToRouteSegmentMap[it.id] = routeI }
                    customerToRouteSegmentMap.remove(saving.cust2Id)
                    Timber.v("Merged reversed J to I's start (I_first, J_first->J_last): ${custI.name} <- ${custJ.name}")
                }
            }
            // Kondisi 4: custI adalah akhir dari routeI, dan custJ adalah akhir dari routeJ (perlu membalik salah satu)
            // Gabungkan routeJ (dibalik) ke akhir routeI
            else if (routeI.lastCustomer == custI && routeJ.lastCustomer == custJ) {
                if (routeI.currentDemand + routeJ.currentDemand <= maxCapacityOfAnyVehicle) {
                    // routeJ akan dibalik sehingga custJ menjadi awal
                    routeI.mergeSegmentAtEnd(routeJ, custI, custJ, distFunc) // custJ adalah firstCustomer dari routeJ (setelah dibalik)
                    routeJ.customers.forEach { customerToRouteSegmentMap[it.id] = routeI }
                    customerToRouteSegmentMap.remove(saving.cust2Id)
                    Timber.v("Merged reversed J to I's end (I_last, J_last->J_first): ${custI.name} -> ${custJ.name}")
                }
            }
        }

        val distinctRouteSegments = customerToRouteSegmentMap.values.distinct().toMutableList()
        Timber.d("Finished C&W merging. Distinct route segments: ${distinctRouteSegments.size}")

        // 4. Penugasan Kendaraan (Bin Packing - First Fit Decreasing) dan Optimasi 2-Opt
        val finalRouteDetails = mutableListOf<RouteDetail>()
        val assignedCustomerIdsInFinalRoutes = mutableSetOf<Int>()

        // Urutkan segmen berdasarkan permintaan (decreasing) untuk FFD
        distinctRouteSegments.sortByDescending { it.currentDemand }

        // Urutkan kendaraan berdasarkan kapasitas (ascending) untuk mencoba kendaraan terkecil dulu yang muat
        val availableVehicles = vehicles.sortedBy { it.capacity }.toMutableList()
        val usedVehicles = mutableSetOf<Int>() // Untuk melacak kendaraan yang sudah dipakai

        for (segment in distinctRouteSegments) {
            if (segment.customers.isEmpty()) continue

            var assignedVehicle: VehicleEntity? = null
            // Cari kendaraan yang belum dipakai dan cocok kapasitasnya
            for (vehicle in availableVehicles) {
                if (vehicle.id !in usedVehicles && vehicle.capacity >= segment.currentDemand) {
                    assignedVehicle = vehicle
                    break // Found a suitable vehicle
                }
            }

            if (assignedVehicle != null) {
                usedVehicles.add(assignedVehicle.id) // Tandai kendaraan sudah dipakai

                // Terapkan 2-opt untuk menyempurnakan urutan dalam segmen
                val optimizedCustomerOrder = applyTwoOpt(segment.customers.toMutableList(), depotLoc, distFunc)
                segment.customers = optimizedCustomerOrder // Update segmen dengan urutan baru
                // Hitung ulang jarak internal segmen setelah 2-opt
                segment.currentDistance = if (optimizedCustomerOrder.size > 1) {
                    (0 until optimizedCustomerOrder.size - 1).sumOf { i ->
                        distFunc(optimizedCustomerOrder[i].location, optimizedCustomerOrder[i+1].location)
                    }
                } else 0.0

                val routeDistanceWithDepot = segment.calculateTotalDistanceWithDepot(depotLoc, distFunc)

                finalRouteDetails.add(
                    RouteDetail(
                        vehicle = assignedVehicle,
                        stops = optimizedCustomerOrder.toList(), // Buat salinan
                        totalDistance = routeDistanceWithDepot,
                        totalDemand = segment.currentDemand
                    )
                )
                optimizedCustomerOrder.forEach { assignedCustomerIdsInFinalRoutes.add(it.id) }
            } else {
                // Jika tidak ada kendaraan yang cocok untuk segmen ini, pelanggannya menjadi tidak terlayani
                Timber.w("No suitable UNUSED vehicle for segment with demand ${segment.currentDemand}. Customers: ${segment.customers.map { it.name }}. Adding to unassigned.")
                segment.customers.forEach { unassignedCustomersList.add(it) }
            }
        }

        // Jika jumlah rute yang terbentuk melebihi jumlah kendaraan yang tersedia (misalnya karena batasan PDF 10 kendaraan)
        // atau jika pengguna memilih lebih sedikit kendaraan daripada rute yang terbentuk.
        // Ini seharusnya ditangani oleh logika pemilihan kendaraan di ViewModel,
        // tapi sebagai fallback, kita bisa memotongnya di sini.
        // Untuk saat ini, kita asumsikan `vehicles` yang di-pass ke `solve` adalah yang DIPILIH pengguna.
        val limitedFinalRoutes = if (finalRouteDetails.size > vehicles.size) {
            Timber.w("Generated ${finalRouteDetails.size} routes, but only ${vehicles.size} vehicles were initially provided/selected. Truncating by total demand.")
            finalRouteDetails.sortedByDescending { it.totalDemand }.take(vehicles.size)
        } else {
            finalRouteDetails
        }

        // Update kembali unassignedCustomerList jika ada pemotongan rute
        val trulyAssignedCustomerIds = mutableSetOf<Int>()
        limitedFinalRoutes.forEach { route -> route.stops.forEach { customer -> trulyAssignedCustomerIds.add(customer.id) } }

        // Semua pelanggan yang awalnya serviceable tapi tidak masuk ke trulyAssignedCustomerIds adalah unassigned
        val finalUnassignedCustomers = serviceableCustomers.filterNot { it.id in trulyAssignedCustomerIds }.toMutableList()
        finalUnassignedCustomers.addAll(initiallyUnassignedCustomers) // Tambahkan yang tidak serviceable dari awal


        val totalOverallDistance = limitedFinalRoutes.sumOf { it.totalDistance }
        val calculationTime = System.currentTimeMillis() - startTime

        Timber.i("Clarke-Wright (V2) Solution: Routes: ${limitedFinalRoutes.size}, Unassigned: ${finalUnassignedCustomers.distinctBy { it.id }.size}, Total Distance: $totalOverallDistance, Time: $calculationTime ms")

        return VrpSolution(
            routes = limitedFinalRoutes,
            unassignedCustomers = finalUnassignedCustomers.distinctBy { it.id }, // Pastikan unik
            totalOverallDistance = totalOverallDistance,
            calculationTimeMillis = calculationTime,
            planId = UUID.randomUUID().toString()
        )
    }

    /**
     * Optimasi 2-Opt untuk satu rute.
     * @param currentRouteCustomers Daftar pelanggan dalam rute (tanpa depot).
     * @param depotLocation Lokasi depot.
     * @param distanceCalc Fungsi untuk menghitung jarak.
     * @return Daftar pelanggan yang sudah dioptimalkan urutannya.
     */
    private fun applyTwoOpt(
        currentRouteCustomers: MutableList<CustomerEntity>,
        depotLocation: LatLng,
        distanceCalc: (LatLng, LatLng) -> Double
    ): MutableList<CustomerEntity> {
        if (currentRouteCustomers.size < 2) return currentRouteCustomers

        var bestRoute = currentRouteCustomers.toMutableList()
        var bestDistance = calculateRouteDistanceForOpt(bestRoute, depotLocation, distanceCalc)
        var improved = true
        var iterationsWithoutImprovement = 0
        var totalIterations = 0

        while (improved && iterationsWithoutImprovement < TWO_OPT_MAX_ITERATIONS_NO_IMPROVEMENT && totalIterations < bestRoute.size * 100) { // Batas iterasi total
            totalIterations++
            improved = false
            outerLoop@ for (i in 0 until bestRoute.size - 1) { // Edge (i, i+1)
                for (j in i + 2 until bestRoute.size) { // Edge (j, j+1), j+1 tidak boleh melebihi size
                    // Ini memastikan segmen yang dibalik (i+1 sampai j) memiliki setidaknya satu node
                    if (j + 1 >= bestRoute.size && i == 0 && j == bestRoute.size -1) continue // Kasus membalik seluruh rute, tidak perlu

                    val newRoute = bestRoute.toMutableList()
                    // Balik segmen dari (i+1) sampai j (inklusif)
                    val segmentToReverse = newRoute.subList(i + 1, j + 1).asReversed()
                    var k = 0
                    for (idx in i + 1..j) {
                        newRoute[idx] = segmentToReverse[k++]
                    }

                    val newDistance = calculateRouteDistanceForOpt(newRoute, depotLocation, distanceCalc)

                    // Periksa apakah perbaikan signifikan
                    if (newDistance < bestDistance - TWO_OPT_MIN_IMPROVEMENT_THRESHOLD) {
                        bestDistance = newDistance
                        bestRoute = newRoute
                        improved = true
                        iterationsWithoutImprovement = 0 // Reset counter
                        // Timber.v("2-opt improvement: New distance $bestDistance by swapping edges involving indices $i and $j")
                        break@outerLoop // Mulai lagi dari awal karena rute berubah (Best Improvement strategy)
                    }
                }
            }
            if (!improved) {
                iterationsWithoutImprovement++
            }
        }
        if (totalIterations > 1 && bestRoute.size > 1) Timber.d("2-opt finished for a route of size ${bestRoute.size}. Total Iterations: $totalIterations, Iterations w/o improvement: $iterationsWithoutImprovement, Final distance: $bestDistance")
        return bestRoute
    }

    /**
     * Menghitung total jarak rute termasuk perjalanan dari/ke depot.
     */
    private fun calculateRouteDistanceForOpt(
        customerList: List<CustomerEntity>,
        depotLocation: LatLng,
        distanceCalc: (LatLng, LatLng) -> Double
    ): Double {
        if (customerList.isEmpty()) return 0.0
        var totalDist = distanceCalc(depotLocation, customerList.first().location) // Depot -> C1
        for (k in 0 until customerList.size - 1) {
            totalDist += distanceCalc(customerList[k].location, customerList[k + 1].location) // Ck -> Ck+1
        }
        totalDist += distanceCalc(customerList.last().location, depotLocation) // Cn -> Depot
        return totalDist
    }
}
