package com.isosic.fleetfixer.screens.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isosic.fleetfixer.auth.AuthTokenStore
import com.isosic.fleetfixer.auth.GoogleAuthClient
import com.isosic.fleetfixer.data.BikeRepository
import com.isosic.fleetfixer.models.Bike
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeScreenViewModel(
    bikeRepository: BikeRepository,
    private val authTokenStore: AuthTokenStore,
    private val googleAuthClient: GoogleAuthClient
) : ViewModel() {

    val bikes: StateFlow<List<Bike>> = bikeRepository.observeBikes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            runCatching { googleAuthClient.signOut() }
            authTokenStore.clearToken()
            onLoggedOut()
        }
    }
}
