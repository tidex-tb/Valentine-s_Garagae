package com.example.valentinesgarage.ui.taskboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.valentinesgarage.domain.model.Vehicle
import com.example.valentinesgarage.domain.repository.GarageRepository
import com.example.valentinesgarage.ui.common.asGarageStream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ActiveVehiclesViewModel @Inject constructor(repository: GarageRepository) : ViewModel() {
    private val stream = repository.observeActiveVehicles().asGarageStream(viewModelScope)
    val vehicles: StateFlow<List<Vehicle>> = stream.items
    val errorMessage: StateFlow<String?> = stream.errorMessage
}
