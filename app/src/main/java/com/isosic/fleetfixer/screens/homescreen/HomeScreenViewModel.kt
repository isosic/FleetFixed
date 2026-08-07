package com.isosic.fleetfixer.screens.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.isosic.fleetfixer.auth.AuthTokenStore
import com.isosic.fleetfixer.auth.GoogleAuthClient
import com.isosic.fleetfixer.data.BikeRepository
import com.isosic.fleetfixer.models.Bike
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeScreenViewModel(
    private val bikeRepository: BikeRepository,
    private val authTokenStore: AuthTokenStore,
    private val googleAuthClient: GoogleAuthClient,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    val bikes: StateFlow<List<Bike>> = bikeRepository.observeBikes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        bikeRepository.startSync(viewModelScope)
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            bikeRepository.clearLocalAndStopSync()
            runCatching { firebaseAuth.signOut() }
            runCatching { googleAuthClient.signOut() }
            authTokenStore.clearToken()
            onLoggedOut()
        }
    }

    override fun onCleared() {
        bikeRepository.stopSync()
        super.onCleared()
    }
}
