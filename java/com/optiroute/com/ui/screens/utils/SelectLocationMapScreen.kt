package com.optiroute.com.ui.screens.utils

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng as GoogleLatLng // Alias untuk Google Maps LatLng
import com.google.maps.android.compose.*
import com.optiroute.com.R
import com.optiroute.com.domain.model.LatLng // Model LatLng kustom aplikasi
import com.optiroute.com.ui.theme.spacing
import com.optiroute.com.utils.PermissionRationaleDialog
import com.optiroute.com.utils.hasLocationPermission
import com.optiroute.com.utils.openAppSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

// Konstanta untuk konfigurasi peta
private const val DEFAULT_MAP_ZOOM = 15f
// Lokasi fallback jika lokasi awal tidak tersedia atau izin ditolak
private val FALLBACK_INITIAL_LOCATION = LatLng(-2.976074, 104.775429) // Palembang, Indonesia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectLocationMapScreen(
    navController: NavController,
    initialLatLng: LatLng?, // Lokasi awal yang mungkin diterima dari layar sebelumnya
    onLocationSelected: (LatLng) -> Unit, // Callback untuk mengirim lokasi terpilih kembali
    viewModel: SelectLocationMapViewModel = hiltViewModel() // ViewModel untuk logika pencarian
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // State dari ViewModel untuk UI pencarian
    val searchUiState by viewModel.uiState.collectAsState()

    // State internal layar untuk lokasi yang saat ini dipilih di peta (baik manual maupun dari search)
    // Diinisialisasi dengan initialLatLng atau fallback. Ini adalah SUMBER KEBENARAN untuk pin.
    var currentPinnedLocation by remember { mutableStateOf(initialLatLng ?: FALLBACK_INITIAL_LOCATION) }

    // State untuk marker di peta Google Maps, selalu sinkron dengan currentPinnedLocation
    val markerState = rememberMarkerState(position = currentPinnedLocation.toGoogleLatLng())
    // State untuk posisi kamera peta
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentPinnedLocation.toGoogleLatLng(), DEFAULT_MAP_ZOOM)
    }

    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    val permissionDeniedMessage = stringResource(id = R.string.location_permission_denied_message_map)

    // Mengamati event dari ViewModel (misalnya, untuk memindahkan peta setelah pencarian)
    LaunchedEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is PlaceSearchUiEvent.ShowSnackbar -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(context.getString(event.messageResId))
                        }
                    }
                    is PlaceSearchUiEvent.PanToLocation -> {
                        // Ketika lokasi dipilih dari hasil pencarian, update pin dan pindahkan kamera
                        currentPinnedLocation = event.latLng // Update pin utama
                        markerState.position = event.latLng.toGoogleLatLng() // Sinkronkan marker
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(event.latLng.toGoogleLatLng(), DEFAULT_MAP_ZOOM),
                            700 // Durasi animasi
                        )
                        focusManager.clearFocus() // Hilangkan fokus dari search bar
                        viewModel.dismissSuggestionsOverlay() // Tutup overlay saran
                    }
                }
            }
        }
    }

    // Jika ViewModel mengindikasikan LatLng baru dari hasil pencarian, update pin utama kita.
    // Ini penting jika viewModel.uiState.selectedPlaceLatLngFromSearch di-observe langsung.
    // Namun, dengan event PanToLocation, ini mungkin redundan tapi aman.
    LaunchedEffect(searchUiState.selectedPlaceLatLngFromSearch) {
        searchUiState.selectedPlaceLatLngFromSearch?.let { newLatLngFromSearch ->
            if (currentPinnedLocation != newLatLngFromSearch) { // Hanya update jika berbeda
                currentPinnedLocation = newLatLngFromSearch
                // Marker dan kamera sudah diurus oleh event PanToLocation
            }
        }
    }

    // Sinkronkan currentPinnedLocation jika marker di-drag oleh pengguna
    LaunchedEffect(markerState.position) {
        val newMarkerPos = markerState.position
        val newCustomLatLng = LatLng(newMarkerPos.latitude, newMarkerPos.longitude)
        if (currentPinnedLocation != newCustomLatLng) {
            currentPinnedLocation = newCustomLatLng // Update pin utama
            Timber.d("Marker dragged to: $currentPinnedLocation")
            viewModel.dismissSuggestionsOverlay() // Tutup saran jika ada interaksi peta
        }
    }

    // Launcher untuk meminta izin lokasi
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineLocationGranted || coarseLocationGranted) {
                Timber.d("Location permission granted via launcher.")
                coroutineScope.launch { // Pindah ke lokasi pengguna saat ini
                    fetchAndMoveToDeviceLocation(context, cameraPositionState, markerState) { latLng ->
                        currentPinnedLocation = latLng // Update pin utama
                    }
                }
            } else {
                Timber.w("Location permission denied via launcher.")
                showPermissionRationaleDialog = true // Tampilkan dialog penjelasan jika ditolak
            }
        }
    )

    // Setup awal posisi kamera dan marker, dan coba dapatkan lokasi saat ini jika perlu
    LaunchedEffect(initialLatLng) { // Hanya dijalankan saat initialLatLng berubah (pertama kali)
        val targetLocation = initialLatLng ?: FALLBACK_INITIAL_LOCATION
        currentPinnedLocation = targetLocation
        markerState.position = targetLocation.toGoogleLatLng()
        cameraPositionState.move( // Pindah langsung, tanpa animasi untuk setup awal
            CameraUpdateFactory.newLatLngZoom(targetLocation.toGoogleLatLng(), DEFAULT_MAP_ZOOM)
        )

        // Jika tidak ada lokasi awal yang diberikan, coba dapatkan lokasi perangkat saat ini
        if (initialLatLng == null) {
            if (context.hasLocationPermission()) {
                fetchAndMoveToDeviceLocation(context, cameraPositionState, markerState) { latLng ->
                    currentPinnedLocation = latLng
                }
            } else {
                // Minta izin jika belum ada
                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
    }

    // Tampilkan dialog penjelasan izin jika diperlukan
    if (showPermissionRationaleDialog) {
        PermissionRationaleDialog(
            title = stringResource(id = R.string.location_permission_rationale_title),
            message = stringResource(id = R.string.location_permission_rationale_message_map),
            onConfirm = { // Pengguna setuju membuka pengaturan
                showPermissionRationaleDialog = false
                context.openAppSettings()
            },
            onDismiss = { // Pengguna menolak
                showPermissionRationaleDialog = false
                coroutineScope.launch { snackbarHostState.showSnackbar(permissionDeniedMessage) }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.select_location_on_map)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_navigate_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            Column( // Kelompokkan FAB secara vertikal
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                FloatingActionButton( // FAB untuk "Lokasi Saya"
                    onClick = {
                        viewModel.dismissSuggestionsOverlay() // Tutup saran
                        focusManager.clearFocus() // Hilangkan fokus dari search bar
                        if (context.hasLocationPermission()) {
                            fetchAndMoveToDeviceLocation(context, cameraPositionState, markerState) { latLng ->
                                currentPinnedLocation = latLng
                            }
                        } else {
                            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.use_current_location))
                }
                FloatingActionButton( // FAB untuk Konfirmasi Pilihan
                    onClick = {
                        Timber.i("Location confirmed by user: $currentPinnedLocation")
                        onLocationSelected(currentPinnedLocation) // Kirim lokasi terpilih (dari pin utama)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.confirm))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End // Posisi FAB di kanan bawah
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues) // Padding dari Scaffold
                .fillMaxSize()
        ) {
            // Google Map sebagai latar belakang utama
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = MapType.NORMAL, // Bisa dibuat konfigurasi di Settings nanti
                    isBuildingEnabled = true,
                    isTrafficEnabled = false, // Lalu lintas bisa membebani, nonaktifkan jika tidak krusial
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true, // Kontrol zoom standar peta
                    myLocationButtonEnabled = false, // Sudah ada FAB kustom
                    mapToolbarEnabled = true, // Toolbar untuk buka di Google Maps App
                    compassEnabled = true,
                    rotationGesturesEnabled = true,
                    scrollGesturesEnabled = true,
                    tiltGesturesEnabled = true,
                    zoomGesturesEnabled = true,
                ),
                // Fungsi Penitikan Manual: Tap pada peta untuk memindahkan marker/pin
                onMapClick = { gmsLatLng ->
                    val newPos = LatLng(gmsLatLng.latitude, gmsLatLng.longitude)
                    currentPinnedLocation = newPos // Update pin utama
                    markerState.position = newPos.toGoogleLatLng() // Update posisi marker visual
                    viewModel.dismissSuggestionsOverlay() // Tutup saran jika ada
                    focusManager.clearFocus() // Hilangkan fokus dari search bar
                    Timber.d("Map clicked, new pin location: $currentPinnedLocation")
                },
                // Fungsi Penitikan Manual: Long press juga bisa untuk memindahkan marker/pin
                onMapLongClick = { gmsLatLng ->
                    val newPos = LatLng(gmsLatLng.latitude, gmsLatLng.longitude)
                    currentPinnedLocation = newPos
                    markerState.position = newPos.toGoogleLatLng()
                    viewModel.dismissSuggestionsOverlay()
                    focusManager.clearFocus()
                    Timber.d("Map long-clicked, new pin location: $currentPinnedLocation")
                }
            ) {
                // Marker yang menunjukkan lokasi terpilih, bisa di-drag (Penitikan Manual)
                Marker(
                    state = markerState, // State dikontrol oleh currentPinnedLocation (via LaunchedEffect)
                    title = stringResource(R.string.selected_location),
                    snippet = "Lat: ${"%.4f".format(currentPinnedLocation.latitude)}, Lng: ${"%.4f".format(currentPinnedLocation.longitude)}",
                    draggable = true, // Penitikan Manual: Marker bisa digeser
                    icon = bitmapDescriptorFromVector(context, R.drawable.ic_map_pin_filled_red) // Ikon pin kustom
                )
            }

            // UI untuk Pencarian Lokasi (di atas peta)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter) // Posisikan di atas tengah
                    .padding(MaterialTheme.spacing.medium)
            ) {
                OutlinedTextField(
                    value = searchUiState.searchQuery,
                    onValueChange = { query -> viewModel.onSearchQueryChanged(query) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester) // Untuk mengelola fokus
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)), // Bentuk lebih modern
                    placeholder = { Text(stringResource(R.string.search_location_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.content_description_search_icon)) },
                    trailingIcon = {
                        if (searchUiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearchQuery() }) { // Ganti ke clearSearchQuery
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.content_description_clear_field))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus() // Sembunyikan keyboard saat search
                        // viewModel.onSearchQueryChanged(searchUiState.searchQuery) // Bisa memicu search lagi jika perlu
                    }),
                    colors = OutlinedTextFieldDefaults.colors( // Gunakan OutlinedTextFieldDefaults
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) // Sedikit transparan saat tidak fokus
                    ),
                    shape = RoundedCornerShape(12.dp) // Bentuk konsisten
                )

                // Indikator loading untuk pencarian
                if (searchUiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.extraSmall))
                }

                // Daftar saran hasil pencarian
                if (searchUiState.showSuggestions && searchUiState.suggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MaterialTheme.spacing.extraSmall)
                            .heightIn(max = 240.dp), // Batasi tinggi daftar saran
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // Elevasi lebih jelas
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp) // Bentuk konsisten
                    ) {
                        LazyColumn {
                            items(searchUiState.suggestions, key = { it.placeId }) { prediction ->
                                SuggestionItem(
                                    predictionText = prediction.getFullText(null).toString(),
                                    onClick = {
                                        viewModel.onSuggestionClicked(prediction)
                                        // Fokus dan overlay diurus oleh ViewModel/event
                                    }
                                )
                                HorizontalDivider() // Pemisah antar item
                            }
                        }
                    }
                } else if (searchUiState.showSuggestions && searchUiState.error != null && !searchUiState.isLoading && searchUiState.searchQuery.length > 2) {
                    // Tampilkan pesan jika tidak ada hasil atau error (setelah query cukup panjang)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MaterialTheme.spacing.extraSmall),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = stringResource(id = searchUiState.error!!),
                            modifier = Modifier.padding(MaterialTheme.spacing.medium).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Informasi LatLng terpilih (opsional, bisa di-toggle)
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.medium),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = stringResource(R.string.location_selected_display, currentPinnedLocation.latitude, currentPinnedLocation.longitude),
                    modifier = Modifier.padding(MaterialTheme.spacing.medium).fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Composable untuk menampilkan satu item saran lokasi.
 */
@Composable
private fun SuggestionItem(predictionText: String, onClick: () -> Unit) {
    Text(
        text = predictionText,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Aksi saat item diklik
            .padding(MaterialTheme.spacing.medium), // Padding standar
        style = MaterialTheme.typography.bodyLarge // Ukuran teks yang sesuai
    )
}

/**
 * Fungsi helper untuk mengambil lokasi perangkat saat ini dan memindahkan kamera serta marker.
 * @param context Context aplikasi.
 * @param cameraPositionState State kamera peta.
 * @param markerState State marker peta.
 * @param onLocationUpdated Callback dengan LatLng baru.
 */
private fun fetchAndMoveToDeviceLocation(
    context: Context,
    cameraPositionState: CameraPositionState,
    markerState: MarkerState,
    onLocationUpdated: (LatLng) -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    // Menggunakan GlobalScope di sini kurang ideal, sebaiknya gunakan scope dari Composable jika memungkinkan
    // atau ViewModel scope. Namun, untuk one-shot fetch ini masih bisa diterima.
    // Untuk kasus yang lebih kompleks, pertimbangkan coroutine scope yang lebih terikat lifecycle.
    val coroutineScope = kotlinx.coroutines.MainScope() // Scope sementara

    coroutineScope.launch {
        try {
            if (!context.hasLocationPermission()) { // Double check izin
                Timber.w("Attempting to fetch device location without permission.")
                // Bisa memicu permintaan izin lagi atau menampilkan pesan
                return@launch
            }
            val locationResult = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null // CancellationToken
            ).await() // Menggunakan await untuk operasi asinkron

            locationResult?.let {
                val deviceLatLng = LatLng(it.latitude, it.longitude)
                Timber.d("Device location fetched: $deviceLatLng")
                onLocationUpdated(deviceLatLng) // Update state utama
                markerState.position = deviceLatLng.toGoogleLatLng() // Update marker visual
                cameraPositionState.animate( // Animasikan kamera ke lokasi baru
                    CameraUpdateFactory.newLatLngZoom(deviceLatLng.toGoogleLatLng(), DEFAULT_MAP_ZOOM),
                    700 // Durasi animasi
                )
            } ?: Timber.w("Fetched device location is null.")
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException fetching device location. Is permission granted and checked properly?")
            // Handle SecurityException, biasanya karena izin belum diberikan atau dicabut
        } catch (e: Exception) {
            Timber.e(e, "Error fetching device location.")
            // Handle error umum lainnya
        }
    }
}


// Fungsi utilitas untuk konversi LatLng kustom ke GoogleMaps LatLng
fun LatLng.toGoogleLatLng(): GoogleLatLng {
    return GoogleLatLng(this.latitude, this.longitude)
}

// Fungsi utilitas untuk membuat BitmapDescriptor dari vector drawable
fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
    return ContextCompat.getDrawable(context, vectorResId)?.run {
        // Tentukan ukuran bitmap yang diinginkan. Sesuaikan jika perlu.
        val desiredWidth = intrinsicWidth.coerceAtLeast(48) // Minimal 48px
        val desiredHeight = intrinsicHeight.coerceAtLeast(48)

        setBounds(0, 0, desiredWidth, desiredHeight)
        val bitmap = Bitmap.createBitmap(desiredWidth, desiredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap) // Menggunakan android.graphics.Canvas
        draw(canvas)
        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
