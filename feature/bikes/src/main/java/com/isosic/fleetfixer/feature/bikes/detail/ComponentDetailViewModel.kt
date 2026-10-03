package com.isosic.fleetfixer.feature.bikes.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isosic.fleetfixer.core.domain.BikeRepository
import com.isosic.fleetfixer.core.domain.ComponentMileageRefresher
import com.isosic.fleetfixer.core.domain.PendingWorkNotifier
import com.isosic.fleetfixer.core.domain.StravaAuthRepository
import com.isosic.fleetfixer.core.model.BikeComponent
import com.isosic.fleetfixer.core.model.CompletedWorkItem
import com.isosic.fleetfixer.core.model.ComponentType
import com.isosic.fleetfixer.core.model.PendingWorkItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ComponentDetailViewModel(
    private val bikeRepository: BikeRepository,
    private val stravaAuthRepository: StravaAuthRepository,
    private val componentMileageRefresher: ComponentMileageRefresher,
    private val pendingWorkNotifier: PendingWorkNotifier,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bikeId: String = checkNotNull(savedStateHandle[BIKE_ID_KEY])
    private val componentType: ComponentType = ComponentType.valueOf(
        checkNotNull(savedStateHandle[COMPONENT_TYPE_KEY])
    )

    val component: StateFlow<BikeComponent?> = bikeRepository.observeBike(bikeId)
        .map { bike -> bike?.componentFor(componentType) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val componentTypeLabel: String = componentType.displayName

    fun updatePurchaseDate(purchaseDateEpochMillis: Long?) {
        viewModelScope.launch {
            val existing = component.value ?: return@launch
            if (existing.dateAddedEpochMillis == purchaseDateEpochMillis) return@launch
            val updated = existing.copy(
                dateAddedEpochMillis = purchaseDateEpochMillis,
                lastServiceEpochMillis = existing.lastServiceEpochMillis
                    ?: purchaseDateEpochMillis
            )
            bikeRepository.updateComponent(bikeId, updated)
            if (stravaAuthRepository.isConnected.first()) {
                componentMileageRefresher.updateBike(bikeId)
            }
        }
    }

    fun addPendingWork(description: String) {
        val trimmed = description.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val existing = component.value ?: return@launch
            bikeRepository.updateComponent(
                bikeId,
                existing.copy(
                    pendingWork = existing.pendingWork + PendingWorkItem(description = trimmed)
                )
            )
            pendingWorkNotifier.notifyPendingWorkAdded(bikeId)
        }
    }

    fun addCompletedWork(notes: String, performedAtEpochMillis: Long) {
        val trimmedNotes = notes.trim()
        if (trimmedNotes.isEmpty()) return
        viewModelScope.launch {
            val existing = component.value ?: return@launch
            val entry = CompletedWorkItem(
                description = trimmedNotes.lineSequence().first().trim(),
                notes = trimmedNotes,
                completedAtEpochMillis = performedAtEpochMillis
            )
            bikeRepository.updateComponent(
                bikeId,
                existing.copy(
                    completedWork = listOf(entry) + existing.completedWork
                )
            )
        }
    }

    companion object {
        const val BIKE_ID_KEY = "bikeId"
        const val COMPONENT_TYPE_KEY = "componentType"
    }
}
