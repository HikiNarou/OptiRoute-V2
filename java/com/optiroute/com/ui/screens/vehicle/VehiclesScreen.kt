package com.optiroute.com.ui.screens.vehicle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel // Digunakan jika ViewModel di-scope ke NavGraph tertentu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.optiroute.com.R
import com.optiroute.com.data.local.entity.VehicleEntity
import com.optiroute.com.ui.theme.spacing
import com.optiroute.com.utils.ConfirmationDialog
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun VehiclesScreen(
    navController: NavController,
    viewModel: VehicleViewModel, // Terima ViewModel yang sudah di-scope dengan benar dari NavHost
    onNavigateToAddEditVehicle: (Int?) -> Unit // Callback untuk navigasi
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val vehiclesListState by viewModel.vehiclesListState.collectAsState()
    var showDeleteConfirmationDialog by remember { mutableStateOf<VehicleEntity?>(null) }

    LaunchedEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is VehicleUiEvent.ShowSnackbar -> {
                        val message = event.messageText ?: event.messageResId?.let {
                            if (event.args != null) context.getString(it, *event.args)
                            else context.getString(it)
                        } ?: ""
                        if (message.isNotBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                        Timber.d("VehiclesScreen Snackbar event: $message")
                    }
                    // NavigateBack dan MaxVehiclesReached ditangani di AddEditScreen atau oleh Snackbar
                    else -> { /* Tidak ada aksi spesifik di sini untuk event lain */ }
                }
            }
        }
    }

    if (showDeleteConfirmationDialog != null) {
        val vehicleToDelete = showDeleteConfirmationDialog!!
        ConfirmationDialog(
            title = stringResource(id = R.string.delete_confirmation_title),
            message = stringResource(id = R.string.confirm_delete_vehicle_message, vehicleToDelete.name),
            onConfirm = {
                viewModel.deleteVehicle(vehicleToDelete)
                showDeleteConfirmationDialog = null
            },
            onDismiss = {
                showDeleteConfirmationDialog = null
            },
            confirmButtonText = stringResource(id = R.string.delete),
            dismissButtonText = stringResource(id = R.string.cancel)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Panggil prepareNewVehicleForm dari ViewModel
                    // Jika berhasil (tidak melebihi batas), maka navigasi
                    if (viewModel.prepareNewVehicleForm()) {
                        onNavigateToAddEditVehicle(null) // null untuk ID berarti menambah baru
                    }
                    // Jika prepareNewVehicleForm mengembalikan false, Snackbar akan ditampilkan oleh ViewModel
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_vehicle_title))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = vehiclesListState) {
                is VehicleListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is VehicleListUiState.Error -> {
                    Text(
                        text = stringResource(id = state.messageResId),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(MaterialTheme.spacing.medium),
                        textAlign = TextAlign.Center
                    )
                }
                is VehicleListUiState.Empty -> {
                    Text(
                        text = stringResource(id = R.string.vehicle_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(MaterialTheme.spacing.medium),
                        textAlign = TextAlign.Center
                    )
                }
                is VehicleListUiState.Success -> {
                    // Meskipun state.vehicles mungkin kosong, kita sudah menangani ini di atas
                    // dengan VehicleListUiState.Empty. Namun, sebagai fallback:
                    if (state.vehicles.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.vehicle_list_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(MaterialTheme.spacing.medium),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                        ) {
                            items(state.vehicles, key = { it.id }) { vehicle ->
                                VehicleItem(
                                    vehicle = vehicle,
                                    onEditClick = {
                                        onNavigateToAddEditVehicle(vehicle.id)
                                    },
                                    onDeleteClick = {
                                        showDeleteConfirmationDialog = vehicle
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleItem(
    vehicle: VehicleEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() }, // Klik pada item untuk mengedit
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Filled.LocalShipping,
                    contentDescription = stringResource(R.string.content_description_vehicle_icon),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                Column(modifier = Modifier.weight(1f)) { // Memberi bobot agar teks tidak terpotong
                    Text(
                        text = vehicle.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            R.string.vehicle_capacity_label_display, // Gunakan string resource yang sesuai untuk tampilan
                            vehicle.capacity.toString(), // Format jika perlu (misal, "%.2f".format(vehicle.capacity))
                            vehicle.capacityUnit
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!vehicle.notes.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.notes) + ": ${vehicle.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tombol aksi di sisi kanan
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
