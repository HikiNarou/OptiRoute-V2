package com.optiroute.com.ui.screens.depot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiroute.com.data.local.entity.DepotEntity
import com.optiroute.com.domain.model.AppResult
import com.optiroute.com.domain.model.LatLng
import com.optiroute.com.domain.repository.DepotRepository
import com.optiroute.com.R
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

@HiltViewModel
class DepotViewModel @Inject constructor(
    private val depotRepository: DepotRepository
) : ViewModel() {

    private val _depotUiState = MutableStateFlow<DepotUiState>(DepotUiState.Loading)
    val depotUiState: StateFlow<DepotUiState> = _depotUiState.asStateFlow()

    // Form state dipisahkan agar bisa di-reset tanpa mempengaruhi DepotUiState (misal saat loading)
    private val _formState = MutableStateFlow(DepotFormState())
    val formState: StateFlow<DepotFormState> = _formState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<DepotUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        Timber.d("DepotViewModel initialized")
        loadDepot()
    }

    private fun loadDepot() {
        viewModelScope.launch {
            _depotUiState.value = DepotUiState.Loading // Set loading state untuk UI utama
            depotRepository.getDepot()
                .catch { e ->
                    Timber.e(e, "Error loading depot")
                    _depotUiState.value = DepotUiState.Error(R.string.error_occurred)
                    _formState.update { it.copy(isLoading = false) } // Pastikan form tidak loading
                    _uiEvent.emit(DepotUiEvent.ShowSnackbar(R.string.error_occurred))
                }
                .collectLatest { depotEntity ->
                    if (depotEntity != null) {
                        Timber.i("Depot loaded: ${depotEntity.name}")
                        _depotUiState.value = DepotUiState.Success(depotEntity)
                        _formState.value = DepotFormState( // Isi form dengan data depot
                            name = depotEntity.name,
                            address = depotEntity.address ?: "",
                            notes = depotEntity.notes ?: "",
                            selectedLocation = depotEntity.location,
                            isLoading = false
                        )
                    } else {
                        Timber.i("No depot found, presenting empty state for UI, form is reset.")
                        _depotUiState.value = DepotUiState.Empty
                        _formState.value = DepotFormState(isLoading = false) // Reset form
                    }
                }
        }
    }

    fun onNameChange(name: String) {
        _formState.update { it.copy(name = name, nameError = null) }
    }

    fun onAddressChange(address: String) {
        _formState.update { it.copy(address = address) }
    }

    fun onNotesChange(notes: String) {
        _formState.update { it.copy(notes = notes) }
    }

    fun onLocationSelected(latLng: LatLng) {
        Timber.d("Location selected/updated for depot: $latLng")
        _formState.update { it.copy(selectedLocation = latLng, locationError = null) }
    }

    fun saveDepot() {
        val currentForm = _formState.value
        if (!validateForm(currentForm)) {
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            val result = depotRepository.saveDepot(
                name = currentForm.name.trim(),
                location = currentForm.selectedLocation!!, // Validasi memastikan ini tidak null
                address = currentForm.address.trim().takeIf { it.isNotBlank() },
                notes = currentForm.notes.trim().takeIf { it.isNotBlank() }
            )
            _formState.update { it.copy(isSaving = false) }

            when (result) {
                is AppResult.Success -> {
                    Timber.i("Depot saved successfully.")
                    _uiEvent.emit(DepotUiEvent.ShowSnackbar(R.string.depot_updated_successfully))
                    // Data akan otomatis ter-refresh oleh Flow dari DAO,
                    // yang akan memicu `loadDepot` (atau lebih tepatnya, kolektor di `loadDepot`).
                    // Jika depot baru dibuat (dari state Empty), UI state akan berubah.
                }
                is AppResult.Error -> {
                    Timber.e(result.exception, "Error saving depot: ${result.message}")
                    val errorMessage = result.message ?: "Gagal menyimpan depot" // Fallback message
                    _uiEvent.emit(DepotUiEvent.ShowSnackbar(messageText = errorMessage))
                }
            }
        }
    }

    private fun validateForm(form: DepotFormState): Boolean {
        var isValid = true
        var tempForm = form.copy()

        if (form.name.isBlank()) {
            tempForm = tempForm.copy(nameError = R.string.required_field)
            isValid = false
        } else {
            tempForm = tempForm.copy(nameError = null)
        }

        if (form.selectedLocation == null || !form.selectedLocation.isValid()) {
            tempForm = tempForm.copy(locationError = R.string.location_not_selected_error)
            isValid = false
        } else {
            tempForm = tempForm.copy(locationError = null)
        }
        _formState.value = tempForm // Update state dengan semua error sekaligus
        return isValid
    }

    fun requestLocationPermission() {
        viewModelScope.launch {
            _uiEvent.emit(DepotUiEvent.RequestLocationPermission)
        }
    }
}

sealed interface DepotUiState {
    data object Loading : DepotUiState
    data class Success(val depot: DepotEntity) : DepotUiState
    data object Empty : DepotUiState
    data class Error(val messageResId: Int) : DepotUiState
}

data class DepotFormState(
    val name: String = "",
    val address: String = "",
    val notes: String = "",
    val selectedLocation: LatLng? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true, // Awalnya true sampai data depot dimuat
    val nameError: Int? = null,
    val locationError: Int? = null
)

sealed interface DepotUiEvent {
    data class ShowSnackbar(val messageResId: Int? = null, val messageText: String? = null) : DepotUiEvent
    data object RequestLocationPermission : DepotUiEvent
}
