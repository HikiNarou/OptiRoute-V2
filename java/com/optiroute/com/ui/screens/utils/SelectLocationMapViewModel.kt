package com.optiroute.com.ui.screens.utils

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.optiroute.com.R
import com.optiroute.com.domain.model.LatLng // Menggunakan model LatLng kustom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

// Data class untuk state UI pencarian tempat
data class PlaceSearchUiState(
    val searchQuery: String = "",
    val suggestions: List<AutocompletePrediction> = emptyList(),
    val isLoading: Boolean = false,
    val showSuggestions: Boolean = false,
    val error: Int? = null, // Resource ID untuk pesan error
    val selectedPlaceLatLngFromSearch: LatLng? = null // Untuk memberi tahu layar tentang LatLng dari tempat yang baru dipilih via search
)

// Event sekali jalan untuk UI
sealed interface PlaceSearchUiEvent {
    data class ShowSnackbar(val messageResId: Int) : PlaceSearchUiEvent
    data class PanToLocation(val latLng: LatLng) : PlaceSearchUiEvent // Event untuk memindahkan kamera peta
}

// Batas perkiraan untuk Palembang, digunakan untuk bias pencarian.
// Sesuaikan jika aplikasi digunakan di area geografis yang berbeda.
// Koordinat: (Southwest Lat, Southwest Lng), (Northeast Lat, Northeast Lng)
val palembangBounds = RectangularBounds.newInstance(
    com.google.android.gms.maps.model.LatLng(-3.052, 104.680), // Titik Barat Daya Palembang
    com.google.android.gms.maps.model.LatLng(-2.911, 104.848)  // Titik Timur Laut Palembang
)

@HiltViewModel
class SelectLocationMapViewModel @Inject constructor(
    application: Application // Application context diperlukan untuk PlacesClient
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlaceSearchUiState())
    val uiState: StateFlow<PlaceSearchUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<PlaceSearchUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    // PlacesClient diinisialisasi di sini.
    // Pastikan Places SDK sudah diinisialisasi di kelas Application.
    private val placesClient = Places.createClient(application)
    private var searchJob: Job? = null
    private val token = AutocompleteSessionToken.newInstance() // Token untuk sesi autocomplete

    /**
     * Dipanggil ketika query pencarian berubah.
     * Membatalkan job pencarian sebelumnya dan memulai yang baru jika query valid.
     * Menggunakan debounce untuk efisiensi.
     * @param query Teks pencarian baru.
     */
    fun onSearchQueryChanged(query: String) {
        // Reset error dan LatLng terpilih sebelumnya dari hasil pencarian saat query berubah
        _uiState.update { it.copy(searchQuery = query, error = null, selectedPlaceLatLngFromSearch = null) }
        searchJob?.cancel() // Batalkan job pencarian yang sedang berjalan

        if (query.length > 2) { // Mulai pencarian setelah 3 karakter untuk efisiensi
            _uiState.update { it.copy(isLoading = true, showSuggestions = true) }
            searchJob = viewModelScope.launch {
                delay(350) // Debounce untuk mengurangi frekuensi panggilan API
                try {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setCountry("ID") // Prioritaskan hasil dari Indonesia
                        .setLocationBias(palembangBounds) // Beri bias pada area Palembang
                        .setSessionToken(token) // Gunakan token sesi
                        .setQuery(query)
                        .build()

                    val response = placesClient.findAutocompletePredictions(request).await() // Panggil API
                    _uiState.update {
                        it.copy(
                            suggestions = response.autocompletePredictions,
                            isLoading = false,
                            // Tampilkan pesan jika tidak ada hasil, tapi tetap tunjukkan list kosong
                            error = if (response.autocompletePredictions.isEmpty() && query.isNotBlank()) R.string.no_results_found else null
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error fetching place predictions for query: $query")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = R.string.error_fetching_suggestions,
                            suggestions = emptyList() // Kosongkan saran jika ada error
                        )
                    }
                }
            }
        } else {
            // Jika query terlalu pendek, kosongkan saran dan hentikan loading
            _uiState.update { it.copy(suggestions = emptyList(), isLoading = false, showSuggestions = false) }
        }
    }

    /**
     * Dipanggil ketika pengguna memilih salah satu saran lokasi.
     * Mengambil detail tempat (terutama LatLng) dan memicu event untuk memindahkan peta.
     * @param prediction Objek AutocompletePrediction yang dipilih.
     */
    fun onSuggestionClicked(prediction: AutocompletePrediction) {
        // Update UI: tampilkan loading, sembunyikan daftar saran, dan isi search bar dengan teks primer dari saran
        _uiState.update { it.copy(isLoading = true, showSuggestions = false, searchQuery = prediction.getPrimaryText(null).toString()) }
        viewModelScope.launch {
            try {
                // Tentukan field mana saja yang ingin diambil dari detail tempat
                val placeFields = listOf(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ADDRESS)
                val request = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
                val response = placesClient.fetchPlace(request).await() // Ambil detail tempat
                val place = response.place

                place.latLng?.let { gmsLatLng ->
                    val newLatLng = LatLng(gmsLatLng.latitude, gmsLatLng.longitude) // Konversi ke model LatLng kustom
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedPlaceLatLngFromSearch = newLatLng // Simpan LatLng hasil pencarian
                        )
                    }
                    // Kirim event untuk memindahkan kamera peta ke lokasi yang dipilih
                    _uiEvent.emit(PlaceSearchUiEvent.PanToLocation(newLatLng))
                } ?: run {
                    // Jika LatLng null (seharusnya jarang terjadi untuk hasil pencarian valid)
                    Timber.e("Place LatLng is null for place: ${place.name}, ID: ${place.id}")
                    _uiState.update { it.copy(isLoading = false, error = R.string.error_fetching_place_details) }
                    _uiEvent.emit(PlaceSearchUiEvent.ShowSnackbar(R.string.error_fetching_place_details))
                }
            } catch (e: ApiException) {
                Timber.e(e, "API Error fetching place details: Status Code: ${e.statusCode}, Message: ${e.statusMessage}")
                _uiState.update { it.copy(isLoading = false, error = R.string.error_fetching_place_details) }
                _uiEvent.emit(PlaceSearchUiEvent.ShowSnackbar(R.string.error_fetching_place_details))
            } catch (e: Exception) {
                Timber.e(e, "Generic error fetching place details for place ID: ${prediction.placeId}")
                _uiState.update { it.copy(isLoading = false, error = R.string.error_fetching_place_details) }
                _uiEvent.emit(PlaceSearchUiEvent.ShowSnackbar(R.string.error_fetching_place_details))
            }
        }
    }

    /**
     * Membersihkan query pencarian dan state terkait.
     */
    fun clearSearchQuery() {
        _uiState.update { PlaceSearchUiState() } // Reset state pencarian ke kondisi awal
        searchJob?.cancel() // Batalkan job pencarian yang mungkin masih berjalan
    }

    /**
     * Menyembunyikan daftar saran lokasi.
     */
    fun dismissSuggestionsOverlay() {
        _uiState.update { it.copy(showSuggestions = false) }
    }
}
