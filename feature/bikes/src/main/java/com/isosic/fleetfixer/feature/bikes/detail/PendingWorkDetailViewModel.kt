package com.isosic.fleetfixer.feature.bikes.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isosic.fleetfixer.core.domain.BikeRepository
import com.isosic.fleetfixer.core.model.ComponentType
import com.isosic.fleetfixer.core.model.PendingWorkItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PendingWorkDetailUi(
    val componentTypeLabel: String,
    val componentName: String,
    val item: PendingWorkItem
)

sealed interface PendingWorkDetailEvent {
    data object Completed : PendingWorkDetailEvent
}

class PendingWorkDetailViewModel(
    private val bikeRepository: BikeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bikeId: String = checkNotNull(savedStateHandle[BIKE_ID_KEY])
    private val componentType: ComponentType = ComponentType.valueOf(
        checkNotNull(savedStateHandle[COMPONENT_TYPE_KEY])
    )
    private val workId: String = checkNotNull(savedStateHandle[WORK_ID_KEY])

    val uiState: StateFlow<PendingWorkDetailUi?> = bikeRepository.observeBike(bikeId)
        .map { bike ->
            val component = bike?.componentFor(componentType) ?: return@map null
            val item = component.pendingWork.find { it.id == workId } ?: return@map null
            PendingWorkDetailUi(
                componentTypeLabel = componentType.displayName,
                componentName = component.name,
                item = item
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _events = MutableSharedFlow<PendingWorkDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PendingWorkDetailEvent> = _events.asSharedFlow()

    fun markAsDone() {
        viewModelScope.launch {
            val existing = bikeRepository.observeBike(bikeId).first()
                ?.componentFor(componentType)
                ?: return@launch
            val updated = existing.completePendingWork(workId) ?: return@launch
            bikeRepository.updateComponent(bikeId, updated)
            _events.emit(PendingWorkDetailEvent.Completed)
        }
    }

    companion object {
        const val BIKE_ID_KEY = "bikeId"
        const val COMPONENT_TYPE_KEY = "componentType"
        const val WORK_ID_KEY = "workId"
    }
}
