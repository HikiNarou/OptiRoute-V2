package com.optiroute.com.ui.screens.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonPinCircle
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.optiroute.com.R
import com.optiroute.com.data.local.entity.CustomerEntity
import com.optiroute.com.ui.theme.spacing
import com.optiroute.com.utils.ConfirmationDialog
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun CustomersScreen(
    navController: NavController,
    viewModel: CustomerViewModel, // Terima ViewModel dari NavHost
    onNavigateToAddEditCustomer: (Int?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val customersListState by viewModel.customersListState.collectAsState()
    var showDeleteConfirmationDialog by remember { mutableStateOf<CustomerEntity?>(null) }

    LaunchedEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is CustomerUiEvent.ShowSnackbar -> {
                        val message = event.messageText ?: event.messageResId?.let { context.getString(it) } ?: ""
                        if (message.isNotBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                        Timber.d("CustomersScreen Snackbar event: $message")
                    }
                    // Event lain ditangani di AddEditCustomerScreen atau tidak relevan di sini
                    CustomerUiEvent.NavigateBack -> { /* Tidak relevan di sini */ }
                    CustomerUiEvent.RequestLocationPermission -> { /* Tidak relevan di sini */ }
                }
            }
        }
    }

    if (showDeleteConfirmationDialog != null) {
        val customerToDelete = showDeleteConfirmationDialog!!
        ConfirmationDialog(
            title = stringResource(id = R.string.delete_confirmation_title),
            message = stringResource(id = R.string.confirm_delete_customer_message, customerToDelete.name),
            onConfirm = {
                viewModel.deleteCustomer(customerToDelete)
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
                    // PERBAIKAN: Panggil prepareNewCustomerForm di ViewModel SEBELUM navigasi
                    viewModel.prepareNewCustomerForm()
                    onNavigateToAddEditCustomer(null) // Navigasi dengan ID null untuk tambah baru
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_customer_title))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = customersListState) {
                is CustomerListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is CustomerListUiState.Error -> {
                    Text(
                        text = stringResource(id = state.messageResId),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(MaterialTheme.spacing.medium),
                        textAlign = TextAlign.Center
                    )
                }
                is CustomerListUiState.Empty -> {
                    Text(
                        text = stringResource(id = R.string.customer_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(MaterialTheme.spacing.medium),
                        textAlign = TextAlign.Center
                    )
                }
                is CustomerListUiState.Success -> {
                    if (state.customers.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.customer_list_empty),
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
                            items(state.customers, key = { customer -> "customer-${customer.id}" }) { customer ->
                                CustomerItem(
                                    customer = customer,
                                    onEditClick = {
                                        // Untuk mode edit, ViewModel akan memuat data di AddEditCustomerScreen
                                        onNavigateToAddEditCustomer(customer.id)
                                    },
                                    onDeleteClick = {
                                        showDeleteConfirmationDialog = customer
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
private fun CustomerItem(
    customer: CustomerEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp) // Bentuk lebih modern
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
                    imageVector = Icons.Filled.PersonPinCircle,
                    contentDescription = stringResource(R.string.content_description_customer_icon),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                Column(modifier = Modifier.weight(1f).padding(start = MaterialTheme.spacing.small)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.customer_demand_label) + ": ${customer.demand}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    customer.address?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Lat: ${"%.4f".format(customer.location.latitude)}, Lng: ${"%.4f".format(customer.location.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!customer.notes.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.notes) + ": ${customer.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

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
