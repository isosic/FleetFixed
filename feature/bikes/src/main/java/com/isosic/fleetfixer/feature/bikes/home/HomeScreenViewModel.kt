package com.isosic.fleetfixer.feature.bikes.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isosic.fleetfixer.core.domain.AppAuth
import com.isosic.fleetfixer.core.domain.AuthTokenStore
import com.isosic.fleetfixer.core.domain.BikeRepository
import com.isosic.fleetfixer.core.domain.GoogleSignInGateway
import com.isosic.fleetfixer.core.domain.SelectedBikeStore
import com.isosic.fleetfixer.core.domain.StravaAuthRepository
import com.isosic.fleetfixer.core.domain.StravaBikeRemoteSource
import com.isosic.fleetfixer.core.domain.StravaTokenStore
import com.isosic.fleetfixer.core.model.Bike
import com.isosic.fleetfixer.core.model.StravaAuthEvent
import com.isosic.fleetfixer.core.model.StravaBike
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val googleSignInGateway: GoogleSignInGateway,
    private val appAuth: AppAuth,
    private val stravaTokenStore: StravaTokenStore,
    private val stravaAuthRepository: StravaAuthRepository,
    private val stravaBikeRemoteSource: StravaBikeRemoteSource,
    private val selectedBikeStore: SelectedBikeStore
) : ViewModel() {

    val bikes: StateFlow<List<Bike>> = bikeRepository.observeBikes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val selectedBike: StateFlow<Bike?> = combine(
        bikes,
        selectedBikeStore.selectedBikeId
    ) { bikeList, selectedId ->
        when {
            bikeList.isEmpty() -> null
            bikeList.size == 1 -> bikeList.first()
            selectedId != null -> bikeList.find { it.id == selectedId }
            else -> null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val pendingStravaBikes = ArrayDeque<StravaBike>()
    private val consumedLocalBikeIds = mutableSetOf<String>()

    init {
        bikeRepository.startSync(viewModelScope)
        viewModelScope.launch {
            stravaAuthRepository.isConnected.collect { connected ->
                _uiState.update { it.copy(isStravaConnected = connected) }
            }
        }
        viewModelScope.launch {
            stravaAuthRepository.authEvents.collect { event ->
                when (event) {
                    StravaAuthEvent.Connected -> {
                        _uiState.update { it.copy(stravaMessage = "Connected to Strava") }
                        importStravaBikes()
                    }
                    StravaAuthEvent.Cancelled -> {
                        _uiState.update {
                            it.copy(
                                stravaMessage = "Strava did not return an auth code. " +
                                    "Check callback domain matches ${stravaAuthRepository.redirectHost()}."
                            )
                        }
                    }
                    is StravaAuthEvent.Failed -> {
                        _uiState.update { it.copy(stravaMessage = event.message) }
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(bikes, selectedBikeStore.selectedBikeId) { bikeList, selectedId ->
                bikeList to selectedId
            }.collect { (bikeList, selectedId) ->
                when {
                    bikeList.isEmpty() -> {
                        if (selectedId != null) selectedBikeStore.clear()
                    }
                    bikeList.size == 1 && selectedId != bikeList.first().id -> {
                        selectedBikeStore.select(bikeList.first().id)
                    }
                    selectedId != null && bikeList.none { it.id == selectedId } -> {
                        selectedBikeStore.clear()
                    }
                }
            }
        }
    }

    fun selectBike(bikeId: String) {
        viewModelScope.launch {
            selectedBikeStore.select(bikeId)
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
            stravaBikeRemoteSource.fetchAthleteBikes()
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
            val wasSelected = selectedBikeStore.selectedBikeId.first() == localBike.id
            bikeRepository.replaceBikeId(
                oldBikeId = localBike.id,
                updatedBike = localBike.copy(
                    id = prompt.stravaBike.id,
                    distanceMeters = prompt.stravaBike.distanceMeters
                )
            )
            if (wasSelected) {
                selectedBikeStore.select(prompt.stravaBike.id)
            }
            consumedLocalBikeIds.add(localBike.id)
            advanceLinkQueue()
        }
    }

    fun importStravaBikeAsNew() {
        val prompt = _uiState.value.stravaLinkPrompt ?: return
        viewModelScope.launch {
            bikeRepository.addBike(prompt.stravaBike.toBike())
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

        syncDistanceForLinkedBikes(localBikes, stravaBikes)

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
                    bikeRepository.addBike(stravaBike.toBike())
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

    private suspend fun syncDistanceForLinkedBikes(
        localBikes: List<Bike>,
        stravaBikes: List<StravaBike>
    ) {
        val localById = localBikes.associateBy { it.id }
        stravaBikes.forEach { stravaBike ->
            val local = localById[stravaBike.id] ?: return@forEach
            if (local.distanceMeters != stravaBike.distanceMeters) {
                bikeRepository.addBike(local.copy(distanceMeters = stravaBike.distanceMeters))
            }
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
                bikeRepository.addBike(stravaBike.toBike())
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

    private fun StravaBike.toBike(): Bike =
        Bike(id = id, name = name, distanceMeters = distanceMeters)

    private fun connectStrava(context: Context) {
        if (!stravaAuthRepository.hasCredentials()) {
            _uiState.update {
                it.copy(
                    stravaMessage = "Add Strava client id/secret to local.properties and rebuild"
                )
            }
            return
        }
        _uiState.update { it.copy(stravaMessage = null) }
        stravaAuthRepository.launchAuthorization(context)
    }

    private fun disconnectStrava() {
        viewModelScope.launch {
            _uiState.update { it.copy(isStravaBusy = true, stravaMessage = null) }
            stravaAuthRepository.disconnect()
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
            selectedBikeStore.clear()
            runCatching { appAuth.signOut() }
            runCatching { googleSignInGateway.signOut() }
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
