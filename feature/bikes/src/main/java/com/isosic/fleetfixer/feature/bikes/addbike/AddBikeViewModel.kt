package com.isosic.fleetfixer.feature.bikes.addbike

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isosic.fleetfixer.core.domain.BikeRepository
import com.isosic.fleetfixer.core.model.Bike
import kotlinx.coroutines.launch

class AddBikeViewModel(
    private val bikeRepository: BikeRepository
) : ViewModel() {

    fun saveBike(name: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            bikeRepository.addBike(Bike(name = name.trim()))
            onSaved()
        }
    }
}
