package com.isosic.fleetfixer.screens.homescreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.isosic.fleetfixer.auth.AuthTokenStore
import com.isosic.fleetfixer.auth.GoogleAuthClient
import com.isosic.fleetfixer.data.BikeRepository
import com.isosic.fleetfixer.models.Bike
import com.isosic.fleetfixer.strava.StravaApiClient
import com.isosic.fleetfixer.strava.StravaAuthClient
import com.isosic.fleetfixer.strava.StravaAuthEvent
import com.isosic.fleetfixer.strava.StravaBike
import com.isosic.fleetfixer.strava.StravaConfig
import com.isosic.fleetfixer.strava.StravaTokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StravaLinkPrompt(
    val stravaBike: StravaBike,
    val candidates: List<Bike>
)

data class HomeUiState(
    val isStravaConnected: Boolean = false,
    val isStravaBusy: Boolean = false,
    val stravaMessage: String? = null,
    val stravaLinkPrompt: StravaLinkPrompt? = null
)

class HomeScreenViewModel(
    private val bikeRepository: BikeRepository,
    private val authTokenStore: AuthTokenStore,
    private val googleAuthClient: GoogleAuthClient,
    private val firebaseAuth: FirebaseAuth,
    private val stravaTokenStore: StravaTokenStore,
    private val stravaAuthClient: StravaAuthClient,
    private val stravaApiClient: StravaApiClient
) : ViewModel() {

    val bikes: StateFlow<List<Bike>> = bikeRepository.observeBikes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val pendingStravaBikes = ArrayDeque<StravaBike>()
    private val consumedLocalBikeIds = mutableSetOf<String>()

    init {
        bikeRepository.startSync(viewModelScope)
        viewModelScope.launch {
            stravaTokenStore.isConnectedFlow.collect { connected ->
                _uiState.update { it.copy(isStravaConnected = connected) }
            }
        }
        viewModelScope.launch {
            stravaAuthClient.authEvents.collect { event ->
                when (event) {
                    StravaAuthEvent.Connected -> {
                        _uiState.update { it.copy(stravaMessage = "Connected to Strava") }
                        importStravaBikes()
                    }
                    StravaAuthEvent.Cancelled -> {
                        _uiState.update {
                            it.copy(
                                stravaMessage = "Strava did not return an auth code. " +
                                    "Check callback domain matches ${StravaConfig.REDIRECT_HOST}."
                            )
                        }
                    }
                    is StravaAuthEvent.Failed -> {
                        _uiState.update { it.copy(stravaMessage = event.message) }
                    }
                }
            }
        }
    }

    fun onStravaClick(context: Context) {
        if (_uiState.value.isStravaBusy) return
        if (_uiState.value.isStravaConnected) {
            disconnectStrava()
        } else {
            connectStrava(context)
        }
    }

    fun importStravaBikes() {
        if (_uiState.value.isStravaBusy && _uiState.value.stravaLinkPrompt != null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isStravaBusy = true, stravaMessage = null, stravaLinkPrompt = null)
            }
            stravaApiClient.fetchAthleteBikes()
                .onSuccess { beginLinkFlow(it) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isStravaBusy = false,
                            stravaMessage = error.localizedMessage ?: "Failed to fetch Strava bikes"
                        )
                    }
                }
        }
    }

    fun linkSelectedLocalBike(localBikeId: String) {
        val prompt = _uiState.value.stravaLinkPrompt ?: return
        viewModelScope.launch {
            val localBike = prompt.candidates.firstOrNull { it.id == localBikeId } ?: return@launch
            bikeRepository.replaceBikeId(
                oldBikeId = localBike.id,
                updatedBike = localBike.copy(id = prompt.stravaBike.id)
            )
            consumedLocalBikeIds.add(localBike.id)
            advanceLinkQueue()
        }
    }

    fun importStravaBikeAsNew() {
        val prompt = _uiState.value.stravaLinkPrompt ?: return
        viewModelScope.launch {
            bikeRepository.addBike(
                Bike(
                    id = prompt.stravaBike.id,
                    name = prompt.stravaBike.name
                )
            )
            advanceLinkQueue()
        }
    }

    fun skipStravaBike() {
        if (_uiState.value.stravaLinkPrompt == null) return
        viewModelScope.launch { advanceLinkQueue() }
    }

    private suspend fun beginLinkFlow(stravaBikes: List<StravaBike>) {
        val localBikes = bikeRepository.observeBikes().first()
        val existingIds = localBikes.map { it.id }.toSet()
        val bikesToProcess = stravaBikes.filterNot { it.id in existingIds }
        val linkableLocalBikes = localBikes.filterNot { isLikelyStravaId(it.id) }

        pendingStravaBikes.clear()
        pendingStravaBikes.addAll(bikesToProcess)
        consumedLocalBikeIds.clear()

        when {
            bikesToProcess.isEmpty() -> {
                _uiState.update {
                    it.copy(
                        isStravaBusy = false,
                        stravaLinkPrompt = null,
                        stravaMessage = if (stravaBikes.isEmpty()) {
                            "No bikes found on Strava"
                        } else {
                            "All Strava bikes are already linked"
                        }
                    )
                }
            }
            linkableLocalBikes.isEmpty() -> {
                bikesToProcess.forEach { stravaBike ->
                    bikeRepository.addBike(Bike(id = stravaBike.id, name = stravaBike.name))
                }
                _uiState.update {
                    it.copy(
                        isStravaBusy = false,
                        stravaLinkPrompt = null,
                        stravaMessage = "Imported ${bikesToProcess.size} bike(s) from Strava"
                    )
                }
            }
            else -> showNextPrompt()
        }
    }

    private suspend fun advanceLinkQueue() {
        pendingStravaBikes.removeFirstOrNull()
        if (pendingStravaBikes.isEmpty()) {
            _uiState.update {
                it.copy(
                    isStravaBusy = false,
                    stravaLinkPrompt = null,
                    stravaMessage = "Strava bikes updated"
                )
            }
            return
        }
        showNextPrompt()
    }

    private suspend fun showNextPrompt() {
        val next = pendingStravaBikes.firstOrNull() ?: run {
            _uiState.update { it.copy(isStravaBusy = false, stravaLinkPrompt = null) }
            return
        }

        val candidates = bikeRepository.observeBikes().first().filter { local ->
            local.id !in consumedLocalBikeIds && !isLikelyStravaId(local.id)
        }

        if (candidates.isEmpty()) {
            while (pendingStravaBikes.isNotEmpty()) {
                val stravaBike = pendingStravaBikes.removeFirst()
                bikeRepository.addBike(Bike(id = stravaBike.id, name = stravaBike.name))
            }
            _uiState.update {
                it.copy(
                    isStravaBusy = false,
                    stravaLinkPrompt = null,
                    stravaMessage = "Imported remaining Strava bikes"
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isStravaBusy = true,
                stravaLinkPrompt = StravaLinkPrompt(
                    stravaBike = next,
                    candidates = candidates
                )
            )
        }
    }

    private fun isLikelyStravaId(id: String): Boolean =
        id.startsWith("b") && id.length > 8 && id.all { it.isLetterOrDigit() }

    private fun connectStrava(context: Context) {
        if (!StravaConfig.hasCredentials()) {
            _uiState.update {
                it.copy(
                    stravaMessage = "Add Strava client id/secret to local.properties and rebuild"
                )
            }
            return
        }
        _uiState.update { it.copy(stravaMessage = null) }
        stravaAuthClient.launchAuthorization(context)
    }

    private fun disconnectStrava() {
        viewModelScope.launch {
            _uiState.update { it.copy(isStravaBusy = true, stravaMessage = null) }
            stravaAuthClient.disconnect()
                .onSuccess {
                    pendingStravaBikes.clear()
                    consumedLocalBikeIds.clear()
                    _uiState.update {
                        it.copy(
                            isStravaBusy = false,
                            stravaLinkPrompt = null,
                            stravaMessage = "Disconnected from Strava"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isStravaBusy = false,
                            stravaMessage = error.localizedMessage ?: "Failed to disconnect Strava"
                        )
                    }
                }
        }
    }

    fun clearStravaMessage() {
        _uiState.update { it.copy(stravaMessage = null) }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            bikeRepository.clearLocalAndStopSync()
            runCatching { firebaseAuth.signOut() }
            runCatching { googleAuthClient.signOut() }
            authTokenStore.clearToken()
            stravaTokenStore.clearTokens()
            onLoggedOut()
        }
    }

    override fun onCleared() {
        bikeRepository.stopSync()
        super.onCleared()
    }
}
