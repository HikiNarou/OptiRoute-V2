package com.optiroute.com.ui.screens.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiroute.com.R
import com.optiroute.com.data.local.entity.VehicleEntity
import com.optiroute.com.domain.model.AppResult
import com.optiroute.com.domain.repository.VehicleRepository
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

// Batasan dari PDF dan kebutuhan aplikasi
const val MAX_VEHICLES_ALLOWED = 10

/**
 * ViewModel untuk VehiclesScreen dan AddEditVehicleScreen.
 * Mengelola state dan logika bisnis terkait data kendaraan.
 */
@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _vehiclesListState = MutableStateFlow<VehicleListUiState>(VehicleListUiState.Loading)
    val vehiclesListState: StateFlow<VehicleListUiState> = _vehiclesListState.asStateFlow()

    private val _vehicleFormState = MutableStateFlow(VehicleFormState())
    val vehicleFormState: StateFlow<VehicleFormState> = _vehicleFormState.asStateFlow()

    // Tidak perlu _editingVehicle secara eksplisit jika formState sudah mencakup ID dan mode edit.
    // Cukup reset formState atau isi dari database saat loadVehicleForEditing.

    private val _uiEvent = MutableSharedFlow<VehicleUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var currentVehicleCount = 0

    init {
        Timber.d("VehicleViewModel initialized")
        // Muat data awal
        loadAllVehicles()
        observeVehicleCount()
    }

    private fun loadAllVehicles() {
        viewModelScope.launch {
            _vehiclesListState.value = VehicleListUiState.Loading
            vehicleRepository.getAllVehicles()
                .catch { e ->
                    Timber.e(e, "Error loading vehicles list")
                    _vehiclesListState.value = VehicleListUiState.Error(R.string.error_occurred)
                    _uiEvent.emit(VehicleUiEvent.ShowSnackbar(messageResId = R.string.error_occurred))
                }
                .collectLatest { vehicles ->
                    Timber.i("Vehicles loaded: ${vehicles.size} items")
                    _vehiclesListState.value = if (vehicles.isEmpty()) {
                        VehicleListUiState.Empty
                    } else {
                        VehicleListUiState.Success(vehicles)
                    }
                }
        }
    }

    private fun observeVehicleCount() {
        viewModelScope.launch {
            vehicleRepository.getVehiclesCount().collectLatest { count ->
                currentVehicleCount = count
                Timber.d("Current vehicle count updated: $currentVehicleCount")
            }
        }
    }

    fun prepareNewVehicleForm(): Boolean {
        if (currentVehicleCount >= MAX_VEHICLES_ALLOWED) {
            viewModelScope.launch {
                _uiEvent.emit(VehicleUiEvent.ShowSnackbar(
                    messageResId = R.string.max_vehicles_reached_message,
                    args = arrayOf(MAX_VEHICLES_ALLOWED) // Mengirim argumen untuk string placeholder
                ))
            }
            return false // Indikasikan bahwa form tidak bisa disiapkan
        }
        _vehicleFormState.value = VehicleFormState(isEditMode = false) // Reset form untuk kendaraan baru
        Timber.d("Prepared form for new vehicle.")
        return true // Form berhasil disiapkan
    }

    fun loadVehicleForEditing(vehicleId: Int) {
        viewModelScope.launch {
            _vehicleFormState.update { it.copy(isLoading = true) } // Tampilkan loading di form jika perlu
            vehicleRepository.getVehicleById(vehicleId).collectLatest { vehicleEntity ->
                if (vehicleEntity != null) {
                    _vehicleFormState.value = VehicleFormState(
                        id = vehicleEntity.id,
                        name = vehicleEntity.name,
                        capacity = vehicleEntity.capacity.toString(),
                        capacityUnit = vehicleEntity.capacityUnit,
                        notes = vehicleEntity.notes ?: "",
                        isEditMode = true,
                        isLoading = false
                    )
                    Timber.d("Loaded vehicle for editing: ${vehicleEntity.name}")
                } else {
                    Timber.w("Vehicle with ID $vehicleId not found for editing.")
                    _vehicleFormState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(VehicleUiEvent.ShowSnackbar(messageResId = R.string.error_vehicle_not_found))
                    _uiEvent.emit(VehicleUiEvent.NavigateBack)
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _vehicleFormState.update { it.copy(name = name, nameError = null) }
    }

    fun onCapacityChange(capacity: String) {
        _vehicleFormState.update { it.copy(capacity = capacity, capacityError = null) }
    }

    fun onCapacityUnitChange(unit: String) {
        _vehicleFormState.update { it.copy(capacityUnit = unit, capacityUnitError = null) }
    }

    fun onNotesChange(notes: String) {
        _vehicleFormState.update { it.copy(notes = notes) }
    }

    fun saveVehicle() {
        val currentForm = _vehicleFormState.value
        if (!validateForm(currentForm)) {
            return
        }

        if (!currentForm.isEditMode && currentVehicleCount >= MAX_VEHICLES_ALLOWED) {
            viewModelScope.launch {
                _uiEvent.emit(VehicleUiEvent.ShowSnackbar(
                    messageResId = R.string.max_vehicles_reached_message,
                    args = arrayOf(MAX_VEHICLES_ALLOWED)
                ))
            }
            return
        }

        val vehicleEntity = VehicleEntity(
            id = if (currentForm.isEditMode) currentForm.id!! else 0, // ID akan autoGenerate jika 0
            name = currentForm.name.trim(),
            capacity = currentForm.capacity.toDoubleOrNull() ?: 0.0, // Validasi sudah memastikan > 0
            capacityUnit = currentForm.capacityUnit.trim(),
            notes = currentForm.notes.trim().takeIf { it.isNotBlank() }
        )

        viewModelScope.launch {
            _vehicleFormState.update { it.copy(isSaving = true) }
            val result = if (currentForm.isEditMode) {
                vehicleRepository.updateVehicle(vehicleEntity)
            } else {
                vehicleRepository.addVehicle(vehicleEntity)
            }
            // Tidak perlu menunggu loadAllVehicles selesai untuk UI feedback cepat
            _vehicleFormState.update { it.copy(isSaving = false) }


            when (result) {
                is AppResult.Success -> {
                    val messageRes = if (currentForm.isEditMode) R.string.vehicle_updated_successfully else R.string.vehicle_added_successfully
                    Timber.i("Vehicle saved successfully. Mode: ${if (currentForm.isEditMode) "Edit" else "Add"}")
                    _uiEvent.emit(VehicleUiEvent.ShowSnackbar(messageResId = messageRes))
                    _uiEvent.emit(VehicleUiEvent.NavigateBack)
                    // loadAllVehicles() akan dipicu oleh perubahan data di repository jika menggunakan Flow
                    // Jika tidak, panggil secara eksplisit atau pastikan Flow DAO aktif.
                    // Untuk memastikan list diupdate, panggil loadAllVehicles() atau andalkan observasi count.
                }
                is AppResult.Error -> {
                    Timber.e(result.exception, "Error saving vehicle: ${result.message}")
                    val errorMessage = result.message ?: contextGetString(R.string.error_saving_vehicle) // Gunakan helper
                    _uiEvent.emit(VehicleUiEvent.ShowSnackbar(messageText = errorMessage))
                }
            }
        }
    }

    private fun validateForm(form: VehicleFormState): Boolean {
        var isValid = true
        var tempFormState = form.copy() // Buat salinan untuk update error

        if (form.name.isBlank()) {
            tempFormState = tempFormState.copy(nameError = R.string.required_field)
            isValid = false
        } else {
            tempFormState = tempFormState.copy(nameError = null)
        }

        val capacityDouble = form.capacity.toDoubleOrNull()
        if (capacityDouble == null || capacityDouble <= 0) {
            tempFormState = tempFormState.copy(capacityError = R.string.value_must_be_positive)
            isValid = false
        } else {
            tempFormState = tempFormState.copy(capacityError = null)
        }

        if (form.capacityUnit.isBlank()) {
            tempFormState = tempFormState.copy(capacityUnitError = R.string.required_field)
            isValid = false
        } else {
            tempFormState = tempFormState.copy(capacityUnitError = null)
        }
        _vehicleFormState.value = tempFormState // Update state dengan semua error sekaligus
        return isValid
    }

    fun deleteVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch {
            // Pertimbangkan menambahkan state isDeleting jika perlu
            val result = vehicleRepository.deleteVehicle(vehicle)
            when (result) {
                is AppResult.Success -> {
                    Timber.i("Vehicle deleted successfully: ${vehicle.name}")
                    _uiEvent.emit(VehicleUiEvent.ShowSnackbar(messageResId = R.string.vehicle_deleted_successfully))
                    // Data akan otomatis ter-refresh oleh Flow dari DAO
                }
                is AppResult.Error -> {
                    Timber.e(result.exception, "Error deleting vehicle: ${result.message}")
                    _uiEvent.emit(VehicleUiEvent.ShowSnackbar(messageResId = R.string.error_deleting_vehicle))
                }
            }
        }
    }

    // Helper untuk mendapatkan string dari context (jika ViewModel tidak memiliki akses langsung)
    // Ini sebaiknya dihindari; ViewModel idealnya tidak tahu tentang Context.
    // Lebih baik kirim ResId ke UI dan UI yang resolve.
    // Namun, jika pesan error dari AppResult.Error adalah String dinamis, maka itu dikirim apa adanya.
    private fun contextGetString(resId: Int): String {
        // Ini adalah placeholder, idealnya ViewModel tidak mengakses Context.
        // Jika pesan error dinamis dari repository, gunakan itu.
        // Jika pesan error statis, kirim resId ke UI.
        return "Error (ID: $resId)" // Fallback jika context tidak tersedia
    }
}

sealed interface VehicleListUiState {
    data object Loading : VehicleListUiState
    data class Success(val vehicles: List<VehicleEntity>) : VehicleListUiState
    data object Empty : VehicleListUiState
    data class Error(val messageResId: Int) : VehicleListUiState
}

data class VehicleFormState(
    val id: Int? = null,
    val name: String = "",
    val capacity: String = "",
    val capacityUnit: String = "",
    val notes: String = "",
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false, // Untuk memuat data edit
    val nameError: Int? = null,
    val capacityError: Int? = null,
    val capacityUnitError: Int? = null
)

sealed interface VehicleUiEvent {
    data class ShowSnackbar(
        val messageResId: Int? = null,
        val messageText: String? = null,
        val args: Array<Any>? = null // Untuk format string
    ) : VehicleUiEvent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ShowSnackbar
            if (messageResId != other.messageResId) return false
            if (messageText != other.messageText) return false
            if (args != null) {
                if (other.args == null) return false
                if (!args.contentEquals(other.args)) return false
            } else if (other.args != null) return false
            return true
        }
        override fun hashCode(): Int {
            var result = messageResId ?: 0
            result = 31 * result + (messageText?.hashCode() ?: 0)
            result = 31 * result + (args?.contentHashCode() ?: 0)
            return result
        }
    }
    data object NavigateBack : VehicleUiEvent
    data object MaxVehiclesReached : VehicleUiEvent // Event spesifik jika diperlukan
    // Tambahkan event lain jika perlu
}
