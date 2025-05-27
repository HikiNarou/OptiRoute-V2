package com.optiroute.com.ui.screens.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
// Hapus hiltViewModel jika ViewModel di-pass sebagai argumen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.optiroute.com.R
import com.optiroute.com.ui.theme.spacing
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVehicleScreen(
    navController: NavController,
    viewModel: VehicleViewModel, // Terima ViewModel sebagai parameter
    vehicleId: Int?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val formState by viewModel.vehicleFormState.collectAsState()

    LaunchedEffect(vehicleId) {
        if (vehicleId != null) {
            Timber.d("AddEditVehicleScreen: Edit mode for vehicle ID $vehicleId")
            viewModel.loadVehicleForEditing(vehicleId)
        } else {
            Timber.d("AddEditVehicleScreen: Add mode - form should be prepared by caller or ViewModel init logic for new.")
            // Jika navigasi ke sini untuk 'tambah baru', ViewModel (yang di-scope ke parent)
            // harus sudah di-reset atau formState-nya disiapkan untuk entri baru.
            // Pemanggilan prepareNewVehicleForm() idealnya dilakukan sebelum navigasi ke sini.
            // Jika tidak, pastikan formState default di ViewModel sesuai untuk 'tambah baru'.
            // Atau, jika ini adalah entry point tunggal, bisa panggil prepare di sini:
            // viewModel.prepareNewVehicleForm() // Panggil jika ini adalah satu-satunya cara masuk ke 'add'
        }
    }

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
                    }
                    is VehicleUiEvent.NavigateBack -> {
                        navController.popBackStack()
                    }
                    is VehicleUiEvent.MaxVehiclesReached -> {
                        // Ditangani oleh Snackbar dari ViewModel
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            id = if (formState.isEditMode) R.string.edit_vehicle_title else R.string.add_vehicle_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_navigate_back)
                        )
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            if (formState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = MaterialTheme.spacing.large))
            } else if (formState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = MaterialTheme.spacing.large))
            }

            OutlinedTextField(
                value = formState.name,
                onValueChange = viewModel::onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.vehicle_name_label)) },
                placeholder = { Text(stringResource(R.string.vehicle_name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { errorResId -> { Text(stringResource(errorResId), color = MaterialTheme.colorScheme.error) } }
            )

            OutlinedTextField(
                value = formState.capacity,
                onValueChange = viewModel::onCapacityChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.vehicle_capacity_label)) },
                placeholder = { Text(stringResource(R.string.vehicle_capacity_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                isError = formState.capacityError != null,
                supportingText = formState.capacityError?.let { errorResId -> { Text(stringResource(errorResId), color = MaterialTheme.colorScheme.error) } }
            )

            OutlinedTextField(
                value = formState.capacityUnit,
                onValueChange = viewModel::onCapacityUnitChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.vehicle_capacity_unit_label)) },
                placeholder = { Text(stringResource(R.string.vehicle_capacity_unit_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next
                ),
                isError = formState.capacityUnitError != null,
                supportingText = formState.capacityUnitError?.let { errorResId -> { Text(stringResource(errorResId), color = MaterialTheme.colorScheme.error) } }
            )

            OutlinedTextField(
                value = formState.notes,
                onValueChange = viewModel::onNotesChange,
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
                onClick = {
                    if (!formState.isLoading && !formState.isSaving) {
                        viewModel.saveVehicle()
                    }
                },
                enabled = !formState.isSaving && !formState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
