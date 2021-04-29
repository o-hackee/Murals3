package com.example.murals3

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle

class GeoDataModel(state: SavedStateHandle) : ViewModel() {
    companion object {
        const val EXTRA_GEOFENCE_INDEX = "GEOFENCE_INDEX"
        const val VISITED_KEY = "VISITED"
    }

    private val _geofenceIndex = state.getLiveData(VISITED_KEY, mutableListOf<Int>())
}