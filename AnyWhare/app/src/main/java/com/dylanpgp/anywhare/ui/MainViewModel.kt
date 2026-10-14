package com.dylanpgp.anywhare.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dylanpgp.anywhare.R
import com.dylanpgp.anywhare.data.AppSettings
import com.dylanpgp.anywhare.data.NominatimClient
import com.dylanpgp.anywhare.data.Place
import com.dylanpgp.anywhare.data.SettingsRepository
import com.dylanpgp.anywhare.data.ThemeMode
import com.dylanpgp.anywhare.domain.CoordinateParser
import com.dylanpgp.anywhare.domain.ParseResult
import com.dylanpgp.anywhare.service.LatLng
import com.dylanpgp.anywhare.service.MockLocationService
import com.dylanpgp.anywhare.service.SimMode
import com.dylanpgp.anywhare.service.SimState
import com.dylanpgp.anywhare.service.SimulationController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<Place> = emptyList(),
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)

    /** null mientras se cargan los ajustes (la splash sigue visible). */
    val settings: StateFlow<AppSettings?> = repo.settings
        .map<AppSettings, AppSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val sim: StateFlow<SimState> = SimulationController.state

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    private val _label = MutableStateFlow<String?>(null)
    val label: StateFlow<String?> = _label.asStateFlow()

    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message.asStateFlow()

    private val _recenter = MutableStateFlow(0)
    val recenter: StateFlow<Int> = _recenter.asStateFlow()

    private val _speedText = MutableStateFlow("5")
    val speedText: StateFlow<String> = _speedText.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Los errores del servicio (p. ej. app de ubicación simulada no seleccionada) llegan aquí.
        viewModelScope.launch {
            SimulationController.state.map { it.errorRes }.filterNotNull().collect { res ->
                _message.value = res
                SimulationController.clearError()
            }
        }
    }

    // --- Ajustes ---
    fun setTheme(mode: ThemeMode) { viewModelScope.launch { repo.setTheme(mode) } }
    fun markWelcomeSeen() { viewModelScope.launch { repo.markWelcomeSeen() } }

    // --- Mensajes ---
    fun postMessage(res: Int) { _message.value = res }
    fun consumeMessage() { _message.value = null }

    // --- Búsqueda ---
    fun onQueryChange(q: String) {
        _search.update { it.copy(query = q.take(200)) }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _search.value = SearchUiState()
    }

    fun submitSearch() {
        val q = _search.value.query.trim()
        if (q.isEmpty()) return

        when (val parsed = CoordinateParser.parse(q)) {
            is ParseResult.Valid -> {
                _search.update { it.copy(results = emptyList(), loading = false) }
                selectPoint(parsed.lat, parsed.lon, label = null, recenter = true)
                return
            }
            ParseResult.OutOfRange -> {
                postMessage(R.string.error_coordinates)
                return
            }
            ParseResult.NotCoordinates -> Unit
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _search.update { it.copy(loading = true) }
            try {
                val results = NominatimClient.search(q, Locale.getDefault().language)
                _search.update { it.copy(results = results) }
                if (results.isEmpty()) postMessage(R.string.error_no_results)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                postMessage(R.string.error_search)
            } finally {
                _search.update { it.copy(loading = false) }
            }
        }
    }

    fun pickPlace(place: Place) {
        _search.update { it.copy(results = emptyList(), query = place.name.take(80)) }
        selectPoint(place.lat, place.lon, place.name, recenter = true)
    }

    fun selectPoint(lat: Double, lon: Double, label: String? = null, recenter: Boolean = false) {
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return
        SimulationController.setPoint(LatLng(lat, lon))
        _label.value = label
        if (recenter) requestRecenter()
    }

    fun requestRecenter() { _recenter.update { it + 1 } }

    // --- Simulación ---
    fun setMode(mode: SimMode) = SimulationController.setMode(mode)

    fun onJoystick(x: Float, y: Float) = SimulationController.setJoystick(x, y)

    fun onSpeedChange(text: String) {
        val clean = text.filter { it.isDigit() || it == '.' || it == ',' }.take(6)
        _speedText.value = clean
        clean.replace(',', '.').toFloatOrNull()?.let {
            if (it in SimulationController.MIN_SPEED_KMH..SimulationController.MAX_SPEED_KMH) {
                SimulationController.setSpeed(it)
            }
        }
    }

    fun setPresetSpeed(kmh: Int) {
        _speedText.value = kmh.toString()
        SimulationController.setSpeed(kmh.toFloat())
    }

    fun startSimulation() {
        if (SimulationController.snapshot().point == null) {
            postMessage(R.string.error_no_point)
            return
        }
        SimulationController.clearError()
        val ctx = getApplication<Application>()
        try {
            ContextCompat.startForegroundService(ctx, Intent(ctx, MockLocationService::class.java))
        } catch (e: Exception) {
            postMessage(R.string.error_service)
        }
    }

    fun stopSimulation() {
        val ctx = getApplication<Application>()
        ctx.stopService(Intent(ctx, MockLocationService::class.java))
    }
}
