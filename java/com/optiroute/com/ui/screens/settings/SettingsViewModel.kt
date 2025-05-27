package com.optiroute.com.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiroute.com.BuildConfig
import com.optiroute.com.R
import com.optiroute.com.domain.model.AppResult
import com.optiroute.com.domain.repository.CustomerRepository
import com.optiroute.com.domain.repository.DepotRepository
import com.optiroute.com.domain.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val depotRepository: DepotRepository,
    private val vehicleRepository: VehicleRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(appVersion = BuildConfig.VERSION_NAME))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<SettingsUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        Timber.d("SettingsViewModel initialized")
    }

    fun onClearAllDataConfirmed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingData = true) }
            Timber.i("Attempting to clear all application data.")

            // Menjalankan semua operasi penghapusan secara paralel dan menunggu hasilnya
            val depotClearDeferred = async { depotRepository.clearAllDepots() }
            val vehicleClearDeferred = async { vehicleRepository.clearAllVehicles() }
            val customerClearDeferred = async { customerRepository.clearAllCustomers() }

            val results = awaitAll(depotClearDeferred, vehicleClearDeferred, customerClearDeferred)

            val depotResult = results[0]
            val vehicleResult = results[1]
            val customerResult = results[2]

            var allClearSuccess = true
            val errorMessages = mutableListOf<String>()

            if (depotResult is AppResult.Error) {
                allClearSuccess = false
                val msg = depotResult.message ?: "Gagal menghapus data depot."
                errorMessages.add(msg)
                Timber.e(depotResult.exception, "Error clearing depot data: $msg")
            } else {
                Timber.d("Depots cleared successfully.")
            }

            if (vehicleResult is AppResult.Error) {
                allClearSuccess = false
                val msg = vehicleResult.message ?: "Gagal menghapus data kendaraan."
                errorMessages.add(msg)
                Timber.e(vehicleResult.exception, "Error clearing vehicle data: $msg")
            } else {
                Timber.d("Vehicles cleared successfully.")
            }

            if (customerResult is AppResult.Error) {
                allClearSuccess = false
                val msg = customerResult.message ?: "Gagal menghapus data pelanggan."
                errorMessages.add(msg)
                Timber.e(customerResult.exception, "Error clearing customer data: $msg")
            } else {
                Timber.d("Customers cleared successfully.")
            }

            _uiState.update { it.copy(isClearingData = false) }

            if (allClearSuccess) {
                Timber.i("All application data cleared successfully.")
                _uiEvent.emit(SettingsUiEvent.ShowSnackbar(messageResId = R.string.data_cleared_successfully))
            } else {
                val combinedErrorMessage = if (errorMessages.isNotEmpty()) {
                    "Gagal membersihkan data: ${errorMessages.joinToString(separator = "; ")}"
                } else {
                    // Ini seharusnya tidak terjadi jika allClearSuccess false, tapi sebagai fallback
                    "Gagal membersihkan sebagian data."
                }
                Timber.e("Failed to clear all application data. Errors: $combinedErrorMessage")
                _uiEvent.emit(SettingsUiEvent.ShowSnackbar(messageText = combinedErrorMessage))
            }
        }
    }
}

data class SettingsUiState(
    val appVersion: String,
    val isClearingData: Boolean = false
)

sealed interface SettingsUiEvent {
    data class ShowSnackbar(val messageResId: Int? = null, val messageText: String? = null) : SettingsUiEvent
    // Tambahkan event lain jika perlu, misalnya untuk navigasi atau dialog spesifik
}
