package com.optiroute.com.ui.screens.customer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel // Untuk ViewModel yang di-scope ke NavGraph tertentu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.optiroute.com.R
import com.optiroute.com.domain.model.LatLng
import com.optiroute.com.ui.navigation.AppScreens
import com.optiroute.com.ui.theme.spacing
import com.optiroute.com.utils.PermissionRationaleDialog
import com.optiroute.com.utils.hasLocationPermission
import com.optiroute.com.utils.openAppSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerScreen(
    navController: NavController,
    viewModel: CustomerViewModel, // Terima ViewModel dari parent NavGraph (CustomersScreen)
    customerId: Int? // Null jika mode 'tambah', non-null jika mode 'edit'
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Ambil formState dari ViewModel. Ini akan merefleksikan data yang sudah ada
    // jika kembali dari peta, atau data yang dimuat untuk mode edit.
    val formState by viewModel.customerFormState.collectAsState()
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    val permissionDeniedMessage = stringResource(id = R.string.location_permission_denied_message)
    val permissionGrantedMessage = stringResource(id = R.string.success) + ": " + stringResource(id = R.string.location_permission_granted)

    // Launcher untuk meminta izin lokasi
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted || coarseLocationGranted) {
                Timber.d("AddEditCustomerScreen: Location permission granted.")
                coroutineScope.launch { snackbarHostState.showSnackbar(permissionGrantedMessage) }
                getCurrentCustomerLocation(context) { latLng ->
                    viewModel.onLocationSelected(latLng) // Update lokasi di ViewModel
                }
            } else {
                Timber.w("AddEditCustomerScreen: Location permission denied.")
                showPermissionRationaleDialog = true
            }
        }
    )

    // Mengamati hasil dari SelectLocationMapScreen
    LaunchedEffect(navController, lifecycleOwner) {
        navController.currentBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
            // Amati perubahan pada RESULT_LAT
            savedStateHandle.getLiveData<Double>(AppScreens.SelectLocationMap.RESULT_LAT)
                .observe(lifecycleOwner) { lat ->
                    // Amati perubahan pada RESULT_LNG HANYA JIKA LAT ADA
                    savedStateHandle.getLiveData<Double>(AppScreens.SelectLocationMap.RESULT_LNG)
                        .observe(lifecycleOwner) { lng ->
                            if (lat != null && lng != null) {
                                Timber.d("AddEditCustomerScreen: Received location from map: Lat=$lat, Lng=$lng")
                                viewModel.onLocationSelected(LatLng(lat, lng))
                                // Bersihkan state setelah dibaca untuk menghindari pemrosesan ulang saat re-compose
                                savedStateHandle.remove<Double>(AppScreens.SelectLocationMap.RESULT_LAT)
                                savedStateHandle.remove<Double>(AppScreens.SelectLocationMap.RESULT_LNG)
                            }
                        }
                }
        }
    }

    // Logika inisialisasi layar: hanya muat data untuk mode edit.
    // Untuk mode tambah, form sudah disiapkan oleh CustomersScreen.
    LaunchedEffect(customerId) {
        if (customerId != null) {
            // Mode Edit: Muat data pelanggan yang ada
            Timber.d("AddEditCustomerScreen: Initializing for EDIT mode, customer ID $customerId.")
            viewModel.loadCustomerForEditing(customerId)
        } else {
            // Mode Tambah: Form state sudah di-reset oleh prepareNewCustomerForm()
            // yang dipanggil dari CustomersScreen sebelum navigasi ke sini.
            // Tidak perlu melakukan apa-apa di sini untuk inisialisasi form.
            // Pastikan error lokasi dibersihkan jika lokasi sudah ada (misalnya dari pemilihan peta sebelumnya)
            Timber.d("AddEditCustomerScreen: Initializing for ADD mode. Form state should be from ViewModel: ${formState.name}, Loc: ${formState.selectedLocation}")
            if (formState.selectedLocation != null && formState.locationError != null) {
                viewModel.onLocationSelected(formState.selectedLocation!!) // Ini akan membersihkan error lokasi
            }
        }
    }

    // Mengamati event UI dari ViewModel
    LaunchedEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is CustomerUiEvent.ShowSnackbar -> {
                        val message = event.messageText ?: event.messageResId?.let { context.getString(it) } ?: ""
                        if (message.isNotBlank()) {
                            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    }
                    is CustomerUiEvent.NavigateBack -> {
                        navController.popBackStack()
                    }
                    is CustomerUiEvent.RequestLocationPermission -> {
                        if (!context.hasLocationPermission()) {
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        } else {
                            Timber.d("Location permission already granted when requested by CustomerVM.")
                            getCurrentCustomerLocation(context) { latLng ->
                                viewModel.onLocationSelected(latLng)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog untuk penjelasan izin
    if (showPermissionRationaleDialog) {
        PermissionRationaleDialog(
            title = stringResource(id = R.string.location_permission_rationale_title),
            message = stringResource(id = R.string.location_permission_rationale_message),
            onConfirm = {
                showPermissionRationaleDialog = false
                context.openAppSettings()
            },
            onDismiss = {
                showPermissionRationaleDialog = false
                coroutineScope.launch { snackbarHostState.showSnackbar(permissionDeniedMessage) }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(id = if (formState.isEditMode) R.string.edit_customer_title else R.string.add_customer_title))
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(MaterialTheme.spacing.medium)
                .verticalScroll(rememberScrollState()), // Memungkinkan scroll
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            // Indikator loading atau saving
            if (formState.isLoading || formState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = MaterialTheme.spacing.medium))
            }

            // Input Nama Pelanggan
            OutlinedTextField(
                value = formState.name,
                onValueChange = viewModel::onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.customer_name_label)) },
                placeholder = { Text(stringResource(R.string.customer_name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { errorResId -> { Text(stringResource(errorResId), color = MaterialTheme.colorScheme.error) } }
            )

            // Input Alamat
            OutlinedTextField(
                value = formState.address,
                onValueChange = viewModel::onAddressChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.address)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            )

            // Input Permintaan
            OutlinedTextField(
                value = formState.demand,
                onValueChange = viewModel::onDemandChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.customer_demand_label)) },
                placeholder = { Text(stringResource(R.string.customer_demand_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                isError = formState.demandError != null,
                supportingText = {
                    Column {
                        formState.demandError?.let { errorResId -> Text(stringResource(errorResId), color = MaterialTheme.colorScheme.error) }
                        Text(stringResource(R.string.customer_demand_unit_explanation), style = MaterialTheme.typography.bodySmall)
                    }
                }
            )

            // Bagian Pemilihan Lokasi
            Text(
                text = stringResource(R.string.customer_location_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.small)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            // Tampilkan LatLng yang terpilih
            formState.selectedLocation?.let {
                Text(
                    text = "Lat: ${"%.6f".format(it.latitude)}, Lng: ${"%.6f".format(it.longitude)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            } ?: Text(
                text = stringResource(R.string.not_set_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tampilkan error lokasi jika ada
            formState.locationError?.let { errorResId ->
                Text(
                    text = stringResource(id = errorResId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.extraSmall)
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            // Tombol untuk memilih lokasi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Button(
                    onClick = {
                        // Navigasi ke peta, kirim lokasi saat ini jika ada sebagai initialLatLng
                        val currentLoc = formState.selectedLocation
                        navController.navigate(
                            AppScreens.SelectLocationMap.createRoute(currentLoc?.latitude, currentLoc?.longitude)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = stringResource(R.string.select_location_on_map))
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text(stringResource(R.string.select_location_on_map))
                }
                Button(
                    onClick = {
                        if (context.hasLocationPermission()) {
                            getCurrentCustomerLocation(context) { latLng -> viewModel.onLocationSelected(latLng) }
                        } else {
                            viewModel.requestLocationPermission() // Minta izin melalui ViewModel
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.use_current_location))
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text(stringResource(R.string.use_current_location))
                }
            }

            // Input Catatan
            OutlinedTextField(
                value = formState.notes,
                onValueChange = viewModel::onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.notes)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Tombol Simpan
            Button(
                onClick = viewModel::saveCustomer,
                enabled = !formState.isSaving && !formState.isLoading, // Nonaktifkan saat menyimpan atau memuat
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

/**
 * Fungsi helper untuk mendapatkan lokasi pelanggan saat ini.
 * @param context Context aplikasi.
 * @param onLocationFetched Callback dengan LatLng yang didapatkan.
 */
private fun getCurrentCustomerLocation(context: android.content.Context, onLocationFetched: (LatLng) -> Unit) {
    if (!context.hasLocationPermission()) {
        Timber.w("Attempted to get current location for customer without permission.")
        // Pertimbangkan untuk memanggil callback dengan nilai null atau error state jika diperlukan
        return
    }

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    // Gunakan MainScope sementara, idealnya scope yang terikat lifecycle Composable atau ViewModel
    val tempCoroutineScope = kotlinx.coroutines.MainScope()

    tempCoroutineScope.launch {
        try {
            val locationResult = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null // CancellationToken
            ).await()

            if (locationResult != null) {
                Timber.d("Current location fetched for customer: Lat=${locationResult.latitude}, Lng=${locationResult.longitude}")
                onLocationFetched(LatLng(locationResult.latitude, locationResult.longitude))
            } else {
                Timber.w("Failed to get current location for customer, location is null.")
                // Pertimbangkan untuk menampilkan Snackbar atau pesan error di sini jika perlu
            }
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException in getCurrentCustomerLocation. Is permission properly handled?")
        } catch (e: Exception) {
            Timber.e(e, "Error getting current location for customer.")
        }
    }
}
