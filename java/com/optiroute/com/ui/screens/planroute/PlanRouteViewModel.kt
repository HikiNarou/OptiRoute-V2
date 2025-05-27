package com.optiroute.com.ui.screens.planroute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiroute.com.R
import com.optiroute.com.data.local.entity.CustomerEntity
import com.optiroute.com.data.local.entity.DepotEntity
import com.optiroute.com.data.local.entity.VehicleEntity
import com.optiroute.com.domain.repository.CustomerRepository
import com.optiroute.com.domain.repository.DepotRepository
import com.optiroute.com.domain.repository.VehicleRepository
import com.optiroute.com.domain.vrp.ClarkeWrightSavings
import com.optiroute.com.domain.vrp.VrpSolution
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlanRouteViewModel @Inject constructor(
    private val depotRepository: DepotRepository,
    private val vehicleRepository: VehicleRepository,
    private val customerRepository: CustomerRepository,
    private val clarkeWrightSavings: ClarkeWrightSavings
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlanRouteUiState>(PlanRouteUiState.Loading)
    val uiState: StateFlow<PlanRouteUiState> = _uiState.asStateFlow()

    private val _selectedCustomerIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedCustomerIds: StateFlow<Set<Int>> = _selectedCustomerIds.asStateFlow()

    private val _selectedVehicleIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedVehicleIds: StateFlow<Set<Int>> = _selectedVehicleIds.asStateFlow()

    // Menyimpan VrpSolution terakhir yang berhasil dihitung
    private val _optimizationResult = MutableStateFlow<VrpSolution?>(null)
    val optimizationResult: StateFlow<VrpSolution?> = _optimizationResult.asStateFlow()

    private val _uiEvent = MutableSharedFlow<PlanRouteUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var calculationJob: Job? = null

    init {
        Timber.d("PlanRouteViewModel initialized")
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = PlanRouteUiState.Loading
            // Menggabungkan Flow untuk mendapatkan semua data awal sekaligus
            combine(
                depotRepository.getDepot(),
                vehicleRepository.getAllVehicles(),
                customerRepository.getAllCustomers()
            ) { depot, vehicles, customers ->
                InitialData(depot, vehicles, customers)
            }.catch { e ->
                Timber.e(e, "Error loading initial data for planning")
                _uiState.value = PlanRouteUiState.Error(R.string.error_occurred)
                _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(messageResId = R.string.error_occurred))
            }.collectLatest { initialData ->
                if (initialData.depot == null) {
                    _uiState.value = PlanRouteUiState.Error(R.string.no_depot_set_for_planning)
                } else {
                    _uiState.value = PlanRouteUiState.Success(
                        depot = initialData.depot,
                        allVehicles = initialData.vehicles,
                        allCustomers = initialData.customers,
                        isOptimizing = false // Pastikan isOptimizing false saat data dimuat
                    )
                }
                // Reset selections and previous results when data reloads
                _selectedCustomerIds.value = emptySet()
                _selectedVehicleIds.value = emptySet()
                _optimizationResult.value = null // Hapus hasil optimasi sebelumnya
            }
        }
    }

    fun toggleCustomerSelection(customerId: Int) {
        _selectedCustomerIds.update { currentSelection ->
            if (currentSelection.contains(customerId)) {
                currentSelection - customerId
            } else {
                currentSelection + customerId
            }
        }
        _optimizationResult.value = null // Reset hasil jika pilihan berubah
    }

    fun toggleVehicleSelection(vehicleId: Int) {
        _selectedVehicleIds.update { currentSelection ->
            if (currentSelection.contains(vehicleId)) {
                currentSelection - vehicleId
            } else {
                currentSelection + vehicleId
            }
        }
        _optimizationResult.value = null // Reset hasil jika pilihan berubah
    }

    fun selectAllCustomers(customers: List<CustomerEntity>) {
        _selectedCustomerIds.value = customers.map { it.id }.toSet()
        _optimizationResult.value = null
    }

    fun deselectAllCustomers() {
        _selectedCustomerIds.value = emptySet()
        _optimizationResult.value = null
    }

    fun selectAllVehicles(vehicles: List<VehicleEntity>) {
        _selectedVehicleIds.value = vehicles.map { it.id }.toSet()
        _optimizationResult.value = null
    }

    fun deselectAllVehicles() {
        _selectedVehicleIds.value = emptySet()
        _optimizationResult.value = null
    }


    fun optimizeRoutes() {
        calculationJob?.cancel() // Batalkan kalkulasi sebelumnya jika ada
        calculationJob = viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is PlanRouteUiState.Success) {
                _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(messageResId = R.string.error_occurred))
                return@launch
            }

            if (currentState.depot == null) {
                _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(messageResId = R.string.no_depot_set_for_planning))
                return@launch
            }

            val selectedCustomers = currentState.allCustomers.filter { it.id in _selectedCustomerIds.value }
            val selectedVehicles = currentState.allVehicles.filter { it.id in _selectedVehicleIds.value }

            if (selectedVehicles.isEmpty()) {
                _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(messageResId = R.string.no_vehicles_available_for_planning))
                return@launch
            }
            if (selectedCustomers.isEmpty()) {
                _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(messageResId = R.string.no_customers_selected_for_planning))
                return@launch
            }

            // Validasi kapasitas yang lebih ketat
            val totalDemand = selectedCustomers.sumOf { it.demand }
            val totalCapacityOfSelectedVehicles = selectedVehicles.sumOf { it.capacity }

            if (totalDemand > totalCapacityOfSelectedVehicles) {
                _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(
                    messageText = "Total permintaan (${"%.2f".format(totalDemand)}) melebihi total kapasitas kendaraan terpilih (${"%.2f".format(totalCapacityOfSelectedVehicles)})."
                ))
                return@launch
            }

            val maxCapacityOfSelectedVehicles = selectedVehicles.maxOfOrNull { it.capacity } ?: 0.0
            selectedCustomers.firstOrNull { it.demand > maxCapacityOfSelectedVehicles }?.let { customer ->
                _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(
                    messageText = "Permintaan pelanggan \"${customer.name}\" (${"%.2f".format(customer.demand)}) melebihi kapasitas kendaraan terbesar yang DIPILIH (${"%.2f".format(maxCapacityOfSelectedVehicles)})."
                ))
                return@launch
            }


            _uiState.update {
                if (it is PlanRouteUiState.Success) it.copy(isOptimizing = true) else it
            }
            _optimizationResult.value = null // Hapus hasil lama sebelum memulai kalkulasi baru

            Timber.d("Starting VRP optimization with ${selectedCustomers.size} customers and ${selectedVehicles.size} vehicles.")

            try {
                val solution = withContext(Dispatchers.Default) { // Gunakan Dispatchers.Default untuk CPU-bound task
                    clarkeWrightSavings.solve(
                        depot = currentState.depot,
                        customers = selectedCustomers,
                        vehicles = selectedVehicles // Kirim hanya kendaraan yang dipilih
                    )
                }

                _uiState.update {
                    if (it is PlanRouteUiState.Success) it.copy(isOptimizing = false) else it
                }
                _optimizationResult.value = solution // Simpan solusi, baik berhasil sebagian atau penuh

                if (solution.routes.isEmpty() && selectedCustomers.isNotEmpty()) {
                    if (solution.unassignedCustomers.size == selectedCustomers.size) {
                        Timber.w("VRP: No routes generated, all selected customers are unassigned.")
                        _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(messageResId = R.string.solution_not_found_all_unassigned))
                    } else if (solution.unassignedCustomers.isNotEmpty()) {
                        Timber.w("VRP: Some routes generated but also unassigned customers: ${solution.unassignedCustomers.map { it.name }}")
                        _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(messageText = "Beberapa pelanggan tidak dapat dilayani dengan kendaraan yang dipilih. Lihat hasil untuk detail."))
                        _uiEvent.emit(PlanRouteUiEvent.NavigateToResults(solution.planId)) // Tetap navigasi untuk melihat hasil parsial
                    } else {
                        Timber.w("VRP: No routes generated, though customers were selected and no unassigned reported (edge case).")
                        _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(messageResId = R.string.solution_not_found))
                    }
                } else if (solution.routes.isNotEmpty()) {
                    Timber.i("VRP Optimization successful. Routes: ${solution.routes.size}, Unassigned: ${solution.unassignedCustomers.size}")
                    if (solution.unassignedCustomers.isNotEmpty()) {
                        _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(messageText = "Rute berhasil dioptimalkan, namun beberapa pelanggan tidak terlayani."))
                    } else {
                        _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(messageResId = R.string.routes_calculated_successfully))
                    }
                    _uiEvent.emit(PlanRouteUiEvent.NavigateToResults(solution.planId))
                } else { // Tidak ada rute dan tidak ada pelanggan (kasus aneh, tapi tangani)
                    Timber.i("VRP: No routes and no customers, likely an initial empty state or error prior.")
                    _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(messageResId = R.string.no_data_for_planning))
                }

            } catch (e: Exception) {
                Timber.e(e, "Error during VRP optimization")
                _uiState.update {
                    if (it is PlanRouteUiState.Success) it.copy(isOptimizing = false) else it
                }
                _optimizationResult.value = VrpSolution(emptyList(), selectedCustomers, 0.0, 0, UUID.randomUUID().toString()) // Tandai semua sebagai tidak terlayani
                _uiEvent.emit(PlanRouteUiEvent.ShowSnackbar(messageResId = R.string.error_calculating_routes))
                // Pertimbangkan untuk tetap navigasi ke hasil untuk menunjukkan pelanggan tidak terlayani
                _uiEvent.emit(PlanRouteUiEvent.NavigateToResults(_optimizationResult.value!!.planId))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        calculationJob?.cancel()
        Timber.d("PlanRouteViewModel cleared")
    }
}

// Data class untuk menampung data awal yang diambil secara bersamaan
private data class InitialData(
    val depot: DepotEntity?,
    val vehicles: List<VehicleEntity>,
    val customers: List<CustomerEntity>
)

// UI State untuk PlanRouteScreen
sealed interface PlanRouteUiState {
    data object Loading : PlanRouteUiState
    data class Success(
        val depot: DepotEntity?,
        val allVehicles: List<VehicleEntity>,
        val allCustomers: List<CustomerEntity>,
        val isOptimizing: Boolean = false
    ) : PlanRouteUiState
    data class Error(val messageResId: Int) : PlanRouteUiState
}


// Event UI sekali jalan
sealed interface PlanRouteUiEvent {
    data class ShowSnackbar(val messageResId: Int? = null, val messageText: String? = null) : PlanRouteUiEvent
    data class NavigateToResults(val planId: String) : PlanRouteUiEvent
}
