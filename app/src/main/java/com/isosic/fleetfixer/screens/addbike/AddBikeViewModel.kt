package com.isosic.fleetfixer.screens.addbike

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isosic.fleetfixer.data.BikeRepository
import com.isosic.fleetfixer.models.Bike
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
