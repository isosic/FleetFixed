package com.isosic.fleetfixer.feature.bikes.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isosic.fleetfixer.core.domain.BikeRepository
import com.isosic.fleetfixer.core.domain.ComponentMileageRefresher
import com.isosic.fleetfixer.core.domain.StravaAuthRepository
import com.isosic.fleetfixer.core.model.Bike
import com.isosic.fleetfixer.core.model.BikeComponent
import com.isosic.fleetfixer.core.model.ComponentType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BikeDetailViewModel(
    private val bikeRepository: BikeRepository,
    private val stravaAuthRepository: StravaAuthRepository,
    private val componentMileageRefresher: ComponentMileageRefresher,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bikeId: String = checkNotNull(savedStateHandle[BIKE_ID_KEY])

    val bike: StateFlow<Bike?> = bikeRepository.observeBike(bikeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _events = MutableSharedFlow<BikeDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<BikeDetailEvent> = _events.asSharedFlow()

    fun onDetailVisible() {
        viewModelScope.launch {
            refreshComponentMileageIfConnected()
        }
    }

    fun onComponentsVisible() {
        viewModelScope.launch {
            refreshComponentMileageIfConnected()
        }
    }

    fun addComponent(
        type: ComponentType,
        name: String,
        notes: String,
        dateAddedEpochMillis: Long?,
        onAdded: () -> Unit = {}
    ) {
        viewModelScope.launch {
            bikeRepository.addComponent(
                bikeId = bikeId,
                component = BikeComponent(
                    type = type,
                    name = name.trim(),
                    notes = notes.trim(),
                    dateAddedEpochMillis = dateAddedEpochMillis,
                    lastServiceEpochMillis = dateAddedEpochMillis
                )
            )
            refreshComponentMileageIfConnected()
            onAdded()
        }
    }

    fun deleteBike() {
        viewModelScope.launch {
            bikeRepository.deleteBike(bikeId)
            _events.emit(BikeDetailEvent.Deleted)
        }
    }

    private suspend fun refreshComponentMileageIfConnected() {
        val connected = stravaAuthRepository.isConnected.first()
        if (!connected) return
        componentMileageRefresher.updateBike(bikeId)
    }

    companion object {
        const val BIKE_ID_KEY = "bikeId"
    }
}

sealed interface BikeDetailEvent {
    data object Deleted : BikeDetailEvent
}
