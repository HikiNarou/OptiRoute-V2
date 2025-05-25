package com.optiroute.com.ui.screens.planroute

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.optiroute.com.R
import com.optiroute.com.data.local.entity.CustomerEntity
import com.optiroute.com.data.local.entity.DepotEntity
import com.optiroute.com.domain.vrp.VrpSolution
import com.optiroute.com.ui.screens.utils.bitmapDescriptorFromVector
import com.optiroute.com.ui.screens.utils.toGoogleLatLng
import com.optiroute.com.ui.theme.*
import timber.log.Timber

// Daftar warna untuk rute di peta (dari ui.theme.Color.kt)
val routeDisplayColorsList = listOf(
    MapRouteColor1, MapRouteColor2, MapRouteColor3, MapRouteColor4, MapRouteColor5,
    MapRouteColor6, MapRouteColor7, MapRouteColor8, MapRouteColor9, MapRouteColor10
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RouteResultsScreen(
    navController: NavController,
    routePlanId: String?, // ID rencana rute, bisa digunakan untuk verifikasi atau pengambilan data jika disimpan
    planRouteViewModel: PlanRouteViewModel // ViewModel dibagikan dari PlanRouteScreen
) {
    // Mengambil VrpSolution dan Depot dari PlanRouteViewModel
    val vrpSolutionState by planRouteViewModel.optimizationResult.collectAsState()
    val depotUiState by planRouteViewModel.uiState.collectAsState() // Untuk mendapatkan info depot

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.map_view) to Icons.Filled.Map,
        stringResource(R.string.list_view) to Icons.Filled.ListAlt
    )

    // Verifikasi apakah planId yang diterima sesuai dengan yang ada di ViewModel (opsional)
    LaunchedEffect(routePlanId, vrpSolutionState) {
        if (vrpSolutionState != null && vrpSolutionState?.planId != routePlanId) {
            Timber.w("RouteResultsScreen: planId mismatch or VrpSolution not for this planId. Nav arg: $routePlanId, ViewModel has: ${vrpSolutionState?.planId}.")
            // Pertimbangkan untuk menampilkan pesan error atau navigasi kembali jika ada ketidaksesuaian signifikan
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.route_results_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_navigate_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ) {
                tabs.forEachIndexed { index, tabInfo ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(tabInfo.first) },
                        icon = { Icon(tabInfo.second, contentDescription = tabInfo.first) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val currentVrpSolution = vrpSolutionState
            val currentDepot = (depotUiState as? PlanRouteUiState.Success)?.depot

            if (currentVrpSolution == null || currentDepot == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Cek apakah masih dalam proses optimasi dari state ViewModel
                    val isOptimizing = (depotUiState as? PlanRouteUiState.Success)?.isOptimizing ?: false
                    if (isOptimizing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(stringResource(R.string.calculating_routes), modifier = Modifier.padding(top = MaterialTheme.spacing.small))
                        }
                    } else {
                        Text(stringResource(R.string.no_routes_to_display), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    }
                }
                return@Scaffold // Keluar jika data tidak ada
            }

            // Tampilkan konten berdasarkan tab yang dipilih
            when (selectedTabIndex) {
                0 -> RouteResultsMapView(
                    vrpSolution = currentVrpSolution,
                    depot = currentDepot
                )
                1 -> RouteResultsListView(
                    vrpSolution = currentVrpSolution,
                    depot = currentDepot
                )
            }
        }
    }
}

@Composable
fun RouteResultsMapView(
    vrpSolution: VrpSolution,
    depot: DepotEntity
) {
    val context = LocalContext.current
    val depotLatLngGms = depot.location.toGoogleLatLng() // Konversi LatLng kustom ke Google Maps LatLng

    // Kumpulkan semua titik untuk menentukan batas kamera
    val allPointsForBounds = remember(vrpSolution, depot) {
        mutableListOf(depotLatLngGms).apply {
            vrpSolution.routes.forEach { routeDetail ->
                routeDetail.stops.forEach { customer ->
                    add(customer.location.toGoogleLatLng())
                }
            }
            // Jika tidak ada rute atau stop, pastikan depot tetap masuk
            if (vrpSolution.routes.all { it.stops.isEmpty() } && this.size == 1) {
                // Tidak perlu aksi tambahan, depot sudah ada
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        // Posisi awal kamera, akan diupdate oleh LaunchedEffect
        position = CameraPosition.fromLatLngZoom(
            if (allPointsForBounds.isNotEmpty()) allPointsForBounds.first() else depotLatLngGms,
            12f // Zoom awal default
        )
    }

    // Animasikan kamera untuk mencakup semua titik saat data berubah
    LaunchedEffect(allPointsForBounds) {
        if (allPointsForBounds.size > 1) {
            val boundsBuilder = LatLngBounds.builder()
            allPointsForBounds.forEach { boundsBuilder.include(it) }
            try {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100), // Padding 100px
                    durationMs = 1000
                )
            } catch (e: IllegalStateException) { // Tangani jika bounds kosong atau hanya satu titik
                Timber.e(e, "Error animating camera to bounds. Points: ${allPointsForBounds.size}")
                if (allPointsForBounds.isNotEmpty()) {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(allPointsForBounds.first(), 15f))
                }
            }
        } else if (allPointsForBounds.size == 1) {
            // Jika hanya satu titik (misalnya hanya depot), zoom ke titik tersebut
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(allPointsForBounds.first(), 15f))
        }
        // Jika allPointsForBounds kosong (seharusnya tidak terjadi jika depot ada), kamera tetap di posisi default
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = true, // Membutuhkan izin lokasi
            mapType = MapType.NORMAL // Atau ambil dari pengaturan pengguna
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            mapToolbarEnabled = true, // Memungkinkan pengguna membuka di Google Maps
            myLocationButtonEnabled = true // Membutuhkan izin lokasi
        )
    ) {
        // Marker untuk Depot
        Marker(
            state = MarkerState(position = depotLatLngGms),
            title = depot.name,
            snippet = stringResource(R.string.nav_depot),
            icon = bitmapDescriptorFromVector(context, R.drawable.ic_depot_pin) // Ikon depot kustom
        )

        // Gambar Polyline dan Marker untuk setiap rute
        vrpSolution.routes.forEachIndexed { routeIndex, routeDetail ->
            val routeColor = routeDisplayColorsList[routeIndex % routeDisplayColorsList.size]
            val polylinePoints = remember(routeDetail, depotLatLngGms) { // Kunci agar hanya dihitung ulang jika data berubah
                mutableListOf<com.google.android.gms.maps.model.LatLng>().apply {
                    add(depotLatLngGms) // Mulai dari depot
                    routeDetail.stops.forEach { customer -> add(customer.location.toGoogleLatLng()) }
                    add(depotLatLngGms) // Kembali ke depot
                }
            }

            if (polylinePoints.size >= 2) { // Polyline butuh minimal 2 titik
                Polyline(
                    points = polylinePoints,
                    color = routeColor,
                    width = 10f, // Lebar garis rute
                    zIndex = routeIndex.toFloat() // Urutan tumpukan garis (jika tumpang tindih)
                )
            }

            // Marker untuk setiap pelanggan dalam rute
            routeDetail.stops.forEachIndexed { stopIndex, customer ->
                Marker(
                    state = MarkerState(position = customer.location.toGoogleLatLng()),
                    title = customer.name,
                    snippet = "Rute ${routeIndex + 1}, Stop ${stopIndex + 1} (Kendaraan: ${routeDetail.vehicle.name})",
                    // Variasi warna marker atau gunakan ikon kustom
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE + (routeIndex * 30) % 360)
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RouteResultsListView(
    vrpSolution: VrpSolution,
    depot: DepotEntity
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
    ) {
        // Informasi Umum Solusi
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                    Text(stringResource(R.string.route_summary_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Divider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.small))
                    Text(
                        stringResource(R.string.total_distance_label) + " ${"%.2f".format(vrpSolution.totalOverallDistance)} km",
                        style = MaterialTheme.typography.titleMedium
                    )
                    vrpSolution.calculationTimeMillis?.let {
                        Text(stringResource(R.string.calculation_time_label, it / 1000.0), style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(stringResource(R.string.total_routes_created_label, vrpSolution.routes.size), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Pelanggan yang Tidak Terlayani
        if (vrpSolution.unassignedCustomers.isNotEmpty()) {
            stickyHeader { // Membuat header tetap terlihat saat scroll
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = stringResource(R.string.unassigned_customers) + " (${vrpSolution.unassignedCustomers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(MaterialTheme.spacing.medium)
                    )
                }
            }
            itemsIndexed(vrpSolution.unassignedCustomers, key = { _, cust -> "unassigned-${cust.id}"}) { _, customer ->
                UnassignedCustomerItem(customer, modifier = Modifier.padding(top = MaterialTheme.spacing.small))
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f))
                ) {
                    Text(
                        stringResource(R.string.no_unassigned_customers),
                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Detail Setiap Rute
        if (vrpSolution.routes.isEmpty() && vrpSolution.unassignedCustomers.isNotEmpty()) {
            // Jika tidak ada rute tapi ada pelanggan tidak terlayani (sudah ditangani di atas)
        } else if (vrpSolution.routes.isEmpty() && vrpSolution.unassignedCustomers.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize().padding(top = MaterialTheme.spacing.extraLarge), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_routes_generated_results),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            vrpSolution.routes.forEachIndexed { index, routeDetail ->
                stickyHeader {
                    Surface(
                        color = routeDisplayColorsList[index % routeDisplayColorsList.size].copy(alpha = 0.2f), // Warna header rute
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = stringResource(R.string.route_header_title, index + 1, routeDetail.vehicle.name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(MaterialTheme.spacing.medium)
                        )
                    }
                }
                item {
                    RouteDetailCard(routeDetail = routeDetail, depotName = depot.name, routeIndex = index)
                    if (index < vrpSolution.routes.size - 1) {
                        Divider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium))
                    }
                }
            }
        }
    }
}

@Composable
fun UnassignedCustomerItem(customer: CustomerEntity, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.WarningAmber, contentDescription = stringResource(R.string.content_desc_unassigned_customer), tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Column {
                Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.customer_demand_label) + ": ${customer.demand}", style = MaterialTheme.typography.bodyMedium)
                customer.address?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun RouteDetailCard(routeDetail: com.optiroute.com.domain.vrp.RouteDetail, depotName: String, routeIndex: Int) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Text(
                stringResource(R.string.vehicle_label_results, routeDetail.vehicle.name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.vehicle_route_details_capacity,
                    "%.2f".format(routeDetail.totalDemand),
                    routeDetail.vehicle.capacity.toString(), // Format jika perlu
                    routeDetail.vehicle.capacityUnit
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                stringResource(R.string.total_distance_label) + " ${"%.2f".format(routeDetail.totalDistance)} km",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Divider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.small))
            Text(stringResource(R.string.stops_label).uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            StopItemView(
                stopName = stringResource(R.string.depot_start_label, depotName),
                index = 0, // Indeks khusus untuk depot awal
                isDepot = true,
                routeColor = routeDisplayColorsList[routeIndex % routeDisplayColorsList.size],
                isFirst = true
            )

            routeDetail.stops.forEachIndexed { index, customer ->
                StopItemView(
                    stopName = customer.name,
                    demand = customer.demand,
                    address = customer.address,
                    index = index + 1, // Indeks reguler untuk pelanggan
                    routeColor = routeDisplayColorsList[routeIndex % routeDisplayColorsList.size]
                )
            }
            StopItemView(
                stopName = stringResource(R.string.depot_finish_label, depotName),
                index = routeDetail.stops.size + 1, // Indeks setelah pelanggan terakhir
                isDepot = true,
                routeColor = routeDisplayColorsList[routeIndex % routeDisplayColorsList.size],
                isLast = true
            )
        }
    }
}

@Composable
fun StopItemView(
    stopName: String,
    demand: Double? = null,
    address: String? = null,
    index: Int,
    isDepot: Boolean = false,
    routeColor: Color,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.extraSmall),
        verticalAlignment = Alignment.Top // Agar ikon dan teks sejajar di atas
    ) {
        // Kolom untuk ikon dan garis vertikal
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Garis vertikal atas (kecuali untuk item pertama)
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .height(12.dp) // Tinggi garis atas
                        .width(2.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            // Ikon bulat
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isDepot) MaterialTheme.colorScheme.tertiary else routeColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isDepot) "D" else (index).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDepot) MaterialTheme.colorScheme.onTertiary else Color.White, // Teks putih di atas warna rute
                    fontWeight = FontWeight.Bold
                )
            }
            // Garis vertikal bawah (kecuali untuk item terakhir)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .height(if (address != null && demand != null) 48.dp else 24.dp) // Sesuaikan tinggi garis
                        .width(2.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        // Kolom untuk detail teks
        Column(modifier = Modifier.weight(1f).padding(top = MaterialTheme.spacing.extraSmall)) { // Padding agar teks sejajar dengan tengah ikon
            Text(
                text = stopName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            address?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            demand?.let {
                Text(
                    text = stringResource(R.string.customer_demand_label) + ": ${"%.1f".format(it)}", // Format permintaan
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
