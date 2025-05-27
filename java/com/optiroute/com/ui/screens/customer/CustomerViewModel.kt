package com.optiroute.com.ui.screens.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiroute.com.R
import com.optiroute.com.data.local.entity.CustomerEntity
import com.optiroute.com.domain.model.AppResult
import com.optiroute.com.domain.model.LatLng
import com.optiroute.com.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel untuk CustomersScreen dan AddEditCustomerScreen.
 * Mengelola state dan logika bisnis terkait data pelanggan.
 */
@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _customersListState = MutableStateFlow<CustomerListUiState>(CustomerListUiState.Loading)
    val customersListState: StateFlow<CustomerListUiState> = _customersListState.asStateFlow()

    // CustomerFormState akan menyimpan data form saat ini, termasuk saat navigasi ke peta
    private val _customerFormState = MutableStateFlow(CustomerFormState())
    val customerFormState: StateFlow<CustomerFormState> = _customerFormState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<CustomerUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        Timber.d("CustomerViewModel initialized")
        loadAllCustomers()
    }

    private fun loadAllCustomers() {
        viewModelScope.launch {
            _customersListState.value = CustomerListUiState.Loading
            customerRepository.getAllCustomers()
                .catch { e ->
                    Timber.e(e, "Error loading customers list")
                    _customersListState.value = CustomerListUiState.Error(R.string.error_occurred)
                    _uiEvent.emit(CustomerUiEvent.ShowSnackbar(messageResId = R.string.error_occurred))
                }
                .collectLatest { customers ->
                    Timber.i("Customers loaded: ${customers.size} items")
                    _customersListState.value = if (customers.isEmpty()) {
                        CustomerListUiState.Empty
                    } else {
                        CustomerListUiState.Success(customers)
                    }
                }
        }
    }

    /**
     * Menyiapkan form untuk entri pelanggan baru.
     * Ini harus dipanggil SEBELUM navigasi ke AddEditCustomerScreen untuk mode 'tambah'.
     */
    fun prepareNewCustomerForm() {
        _customerFormState.value = CustomerFormState(isEditMode = false) // Reset form ke state awal untuk tambah baru
        Timber.d("Prepared form for new customer. Current state: ${_customerFormState.value}")
    }

    /**
     * Memuat data pelanggan yang ada untuk diedit ke dalam form.
     * @param customerId ID pelanggan yang akan diedit.
     */
    fun loadCustomerForEditing(customerId: Int) {
        viewModelScope.launch {
            _customerFormState.update { it.copy(isLoading = true) } // Tampilkan loading di form
            customerRepository.getCustomerById(customerId).collectLatest { customerEntity ->
                if (customerEntity != null) {
                    _customerFormState.value = CustomerFormState(
                        id = customerEntity.id,
                        name = customerEntity.name,
                        address = customerEntity.address ?: "",
                        demand = customerEntity.demand.toString(),
                        selectedLocation = customerEntity.location,
                        notes = customerEntity.notes ?: "",
                        isEditMode = true, // Set mode edit
                        isLoading = false
                    )
                    Timber.d("Loaded customer for editing: ${customerEntity.name}")
                } else {
                    Timber.w("Customer with ID $customerId not found for editing.")
                    _customerFormState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(CustomerUiEvent.ShowSnackbar(messageResId = R.string.error_customer_not_found))
                    _uiEvent.emit(CustomerUiEvent.NavigateBack) // Kembali jika pelanggan tidak ditemukan
                }
            }
        }
    }

    // Metode untuk memperbarui field form individual.
    // Ini memastikan hanya field yang relevan yang diubah, mempertahankan sisa state.
    fun onNameChange(name: String) {
        _customerFormState.update { it.copy(name = name, nameError = null) }
    }

    fun onAddressChange(address: String) {
        _customerFormState.update { it.copy(address = address) }
    }

    fun onDemandChange(demand: String) {
        _customerFormState.update { it.copy(demand = demand, demandError = null) }
    }

    fun onNotesChange(notes: String) {
        _customerFormState.update { it.copy(notes = notes) }
    }

    /**
     * Dipanggil ketika lokasi dipilih dari peta.
     * Hanya memperbarui selectedLocation dan locationError, field lain tetap.
     * @param latLng Lokasi LatLng yang baru dipilih.
     */
    fun onLocationSelected(latLng: LatLng) {
        Timber.d("Location selected/updated for customer in ViewModel: $latLng. Current form state before update: ${_customerFormState.value}")
        _customerFormState.update { currentState ->
            currentState.copy(selectedLocation = latLng, locationError = null)
        }
        Timber.d("Form state after location update: ${_customerFormState.value}")
    }

    /**
     * Menyimpan data pelanggan (baik baru maupun yang diedit).
     */
    fun saveCustomer() {
        val currentForm = _customerFormState.value
        if (!validateForm(currentForm)) { // Validasi form sebelum menyimpan
            return
        }

        // Buat CustomerEntity dari state form saat ini
        val customerEntity = CustomerEntity(
            id = if (currentForm.isEditMode) currentForm.id!! else 0, // ID 0 untuk autoGenerate oleh Room
            name = currentForm.name.trim(),
            address = currentForm.address.trim().takeIf { it.isNotBlank() },
            demand = currentForm.demand.toDoubleOrNull() ?: 0.0,
            location = currentForm.selectedLocation!!, // Validasi memastikan ini tidak null
            notes = currentForm.notes.trim().takeIf { it.isNotBlank() }
        )

        viewModelScope.launch {
            _customerFormState.update { it.copy(isSaving = true) } // Tampilkan status menyimpan
            val result = if (currentForm.isEditMode) {
                customerRepository.updateCustomer(customerEntity)
            } else {
                customerRepository.addCustomer(customerEntity)
            }
            _customerFormState.update { it.copy(isSaving = false) } // Selesai menyimpan

            when (result) {
                is AppResult.Success -> {
                    val messageRes = if (currentForm.isEditMode) R.string.customer_updated_successfully else R.string.customer_added_successfully
                    Timber.i("Customer saved successfully. Mode: ${if (currentForm.isEditMode) "Edit" else "Add"}")
                    _uiEvent.emit(CustomerUiEvent.ShowSnackbar(messageResId = messageRes))
                    _uiEvent.emit(CustomerUiEvent.NavigateBack) // Kembali ke layar daftar pelanggan
                }
                is AppResult.Error -> {
                    Timber.e(result.exception, "Error saving customer: ${result.message}")
                    val errorMessage = result.message ?: "Gagal menyimpan pelanggan."
                    _uiEvent.emit(CustomerUiEvent.ShowSnackbar(messageText = errorMessage))
                }
            }
        }
    }

    /**
     * Memvalidasi input form pelanggan.
     * @param form State form yang akan divalidasi.
     * @return true jika valid, false jika tidak.
     */
    private fun validateForm(form: CustomerFormState): Boolean {
        var isValid = true
        var tempForm = form.copy() // Salinan untuk memperbarui error

        if (form.name.isBlank()) {
            tempForm = tempForm.copy(nameError = R.string.required_field)
            isValid = false
        } else {
            tempForm = tempForm.copy(nameError = null)
        }

        val demandDouble = form.demand.toDoubleOrNull()
        if (demandDouble == null || demandDouble < 0) {
            tempForm = tempForm.copy(demandError = R.string.invalid_number) // Permintaan bisa 0
            isValid = false
        } else {
            tempForm = tempForm.copy(demandError = null)
        }

        if (form.selectedLocation == null || !form.selectedLocation.isValid()) {
            tempForm = tempForm.copy(locationError = R.string.location_not_selected_error)
            isValid = false
        } else {
            tempForm = tempForm.copy(locationError = null)
        }
        // Update state form dengan pesan error jika ada
        _customerFormState.value = tempForm
        return isValid
    }

    /**
     * Menghapus pelanggan.
     * @param customer Entitas pelanggan yang akan dihapus.
     */
    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            val result = customerRepository.deleteCustomer(customer)
            when (result) {
                is AppResult.Success -> {
                    Timber.i("Customer deleted successfully: ${customer.name}")
                    _uiEvent.emit(CustomerUiEvent.ShowSnackbar(messageResId = R.string.customer_deleted_successfully))
                    // Daftar akan diperbarui secara otomatis oleh Flow dari DAO
                }
                is AppResult.Error -> {
                    Timber.e(result.exception, "Error deleting customer: ${result.message}")
                    _uiEvent.emit(CustomerUiEvent.ShowSnackbar(messageResId = R.string.error_deleting_customer))
                }
            }
        }
    }

    /**
     * Meminta izin lokasi (jika belum diberikan).
     */
    fun requestLocationPermission() {
        viewModelScope.launch {
            _uiEvent.emit(CustomerUiEvent.RequestLocationPermission)
        }
    }
}

// Sealed interface untuk state UI daftar pelanggan
sealed interface CustomerListUiState {
    data object Loading : CustomerListUiState
    data class Success(val customers: List<CustomerEntity>) : CustomerListUiState
    data object Empty : CustomerListUiState
    data class Error(val messageResId: Int) : CustomerListUiState
}

// Data class untuk state form tambah/ubah pelanggan
data class CustomerFormState(
    val id: Int? = null,
    val name: String = "",
    val address: String = "",
    val demand: String = "", // Disimpan sebagai String untuk input TextField
    val selectedLocation: LatLng? = null,
    val notes: String = "",
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false, // Untuk memuat data saat mode edit
    val nameError: Int? = null, // Resource ID untuk pesan error nama
    val demandError: Int? = null, // Resource ID untuk pesan error permintaan
    val locationError: Int? = null // Resource ID untuk pesan error lokasi
)

// Sealed interface untuk event UI sekali jalan
sealed interface CustomerUiEvent {
    data class ShowSnackbar(val messageResId: Int? = null, val messageText: String? = null) : CustomerUiEvent
    data object NavigateBack : CustomerUiEvent // Event untuk navigasi kembali
    data object RequestLocationPermission : CustomerUiEvent // Event untuk meminta izin lokasi
}
