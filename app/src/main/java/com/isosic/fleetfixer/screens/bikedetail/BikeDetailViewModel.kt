package com.isosic.fleetfixer.screens.bikedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isosic.fleetfixer.data.BikeRepository
import com.isosic.fleetfixer.models.Bike
import com.isosic.fleetfixer.models.BikeComponent
import com.isosic.fleetfixer.models.ComponentType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BikeDetailViewModel(
    private val bikeRepository: BikeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bikeId: String = checkNotNull(savedStateHandle[BIKE_ID_KEY])

    val bike: StateFlow<Bike?> = bikeRepository.observeBike(bikeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun addComponent(type: ComponentType, name: String, notes: String, dateAddedEpochMillis: Long) {
        viewModelScope.launch {
            bikeRepository.addComponent(
                bikeId = bikeId,
                component = BikeComponent(
                    type = type,
                    name = name.trim(),
                    notes = notes.trim(),
                    dateAddedEpochMillis = dateAddedEpochMillis
                )
            )
        }
    }

    companion object {
        const val BIKE_ID_KEY = "bikeId"
    }
}
