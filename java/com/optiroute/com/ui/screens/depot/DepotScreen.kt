package com.optiroute.com.ui.screens.depot

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel // Digunakan jika ViewModel di-scope ke NavGraph tertentu
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

@Composable
fun DepotScreen(
    navController: NavController,
    viewModel: DepotViewModel // Terima ViewModel yang sudah di-scope dengan benar dari NavHost
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val depotUiState by viewModel.depotUiState.collectAsState()
    val formState by viewModel.formState.collectAsState() // Ambil formState dari ViewModel

    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    val permissionDeniedMessage = stringResource(id = R.string.location_permission_denied_message)
    val permissionGrantedMessage = stringResource(id = R.string.success) + ": " + stringResource(id = R.string.location_permission_granted)


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted || coarseLocationGranted) {
                Timber.d("DepotScreen: Location permission granted.")
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(permissionGrantedMessage)
                }
                getCurrentDepotLocation(context) { latLng ->
                    viewModel.onLocationSelected(latLng)
                }
            } else {
                Timber.w("DepotScreen: Location permission denied.")
                showPermissionRationaleDialog = true
            }
        }
    )

    // Mengamati perubahan LatLng dari SelectLocationMapScreen
    LaunchedEffect(navController, lifecycleOwner) {
        navController.currentBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
            savedStateHandle.getLiveData<Double>(AppScreens.SelectLocationMap.RESULT_LAT)
                .observe(lifecycleOwner) { lat ->
                    savedStateHandle.getLiveData<Double>(AppScreens.SelectLocationMap.RESULT_LNG)
                        .observe(lifecycleOwner) { lng ->
                            if (lat != null && lng != null) {
                                Timber.d("DepotScreen: Received location from map: Lat=$lat, Lng=$lng")
                                viewModel.onLocationSelected(LatLng(lat, lng))
                                // Bersihkan state setelah dibaca
                                savedStateHandle.remove<Double>(AppScreens.SelectLocationMap.RESULT_LAT)
                                savedStateHandle.remove<Double>(AppScreens.SelectLocationMap.RESULT_LNG)
                            }
                        }
                }
        }
    }

    LaunchedEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is DepotUiEvent.ShowSnackbar -> {
                        val message = event.messageText ?: event.messageResId?.let { context.getString(it) } ?: ""
                        if (message.isNotBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    }
                    is DepotUiEvent.RequestLocationPermission -> {
                        if (!context.hasLocationPermission()) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            Timber.d("DepotScreen: Location permission already granted when requested by VM.")
                            getCurrentDepotLocation(context) { latLng ->
                                viewModel.onLocationSelected(latLng)
                            }
                        }
                    }
                }
            }
        }
    }

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
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(permissionDeniedMessage)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(MaterialTheme.spacing.medium)
                .verticalScroll(rememberScrollState()), // Memungkinkan scroll jika konten melebihi layar
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tampilkan form input berdasarkan formState, bukan depotUiState
            // depotUiState digunakan untuk menampilkan status global (Loading, Error, Empty)
            // sedangkan formState untuk input pengguna.

            if (formState.isLoading && depotUiState is DepotUiState.Loading) { // Hanya loading awal
                CircularProgressIndicator(modifier = Modifier.padding(vertical = MaterialTheme.spacing.large))
            } else if (depotUiState is DepotUiState.Error) {
                val errorState = depotUiState as DepotUiState.Error
                Text(
                    stringResource(id = errorState.messageResId),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium)
                )
            } else {
                // Form selalu ditampilkan, baik depot sudah ada atau belum (untuk input baru)
                DepotInputFormContent(
                    formState = formState, // Gunakan formState dari ViewModel
                    onNameChange = viewModel::onNameChange,
                    onAddressChange = viewModel::onAddressChange,
                    onNotesChange = viewModel::onNotesChange,
                    onSaveClick = viewModel::saveDepot,
                    onSelectOnMapClick = {
                        val currentLoc = formState.selectedLocation
                        navController.navigate(
                            AppScreens.SelectLocationMap.createRoute(currentLoc?.latitude, currentLoc?.longitude)
                        )
                    },
                    onUseCurrentLocationClick = {
                        viewModel.requestLocationPermission() // Minta izin melalui ViewModel
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepotInputFormContent(
    formState: DepotFormState,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onSelectOnMapClick: () -> Unit,
    onUseCurrentLocationClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        if (formState.isSaving) { // Loading saat menyimpan
            CircularProgressIndicator(modifier = Modifier.padding(bottom = MaterialTheme.spacing.medium))
        }

        OutlinedTextField(
            value = formState.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.depot_name_label)) },
            placeholder = { Text(stringResource(R.string.depot_name_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            isError = formState.nameError != null,
            supportingText = formState.nameError?.let { errorResId -> { Text(stringResource(errorResId), color = MaterialTheme.colorScheme.error) } }
        )

        Text(
            text = stringResource(R.string.depot_location_label),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.small)
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

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

        formState.locationError?.let { errorResId ->
            Text(
                text = stringResource(id = errorResId),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = MaterialTheme.spacing.extraSmall)
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Button(
                onClick = onSelectOnMapClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = stringResource(R.string.select_location_on_map))
                Spacer(Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.select_location_on_map))
            }
            Button(
                onClick = onUseCurrentLocationClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.use_current_location))
                Spacer(Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.use_current_location))
            }
        }

        OutlinedTextField(
            value = formState.address,
            onValueChange = onAddressChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.address)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
        )

        OutlinedTextField(
            value = formState.notes,
            onValueChange = onNotesChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.notes)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Button(
            onClick = onSaveClick,
            enabled = !formState.isSaving && !formState.isLoading, // Nonaktifkan juga saat form loading data awal
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save))
        }
    }
}

private fun getCurrentDepotLocation(context: android.content.Context, onLocationFetched: (LatLng) -> Unit) {
    if (!context.hasLocationPermission()) {
        Timber.w("DepotScreen: Attempted to get current location without permission.")
        return
    }
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val coroutineScope = kotlinx.coroutines.MainScope() // Scope sementara jika fungsi ini tidak suspend

    coroutineScope.launch {
        try {
            val locationResult = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()

            if (locationResult != null) {
                Timber.d("DepotScreen: Current location fetched: Lat=${locationResult.latitude}, Lng=${locationResult.longitude}")
                onLocationFetched(LatLng(locationResult.latitude, locationResult.longitude))
            } else {
                Timber.w("DepotScreen: Failed to get current location, location is null.")
            }
        } catch (e: Exception) {
            Timber.e(e, "DepotScreen: Error getting current location.")
        }
    }
}
