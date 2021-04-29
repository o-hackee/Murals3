package com.example.murals3

import android.app.PendingIntent
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import timber.log.Timber

class GeoDataModel(state: SavedStateHandle) : ViewModel() {

    companion object {
        const val EXTRA_GEOFENCE_INDEX = "GEOFENCE_INDEX"
        const val VISITED_KEY = "visited"
        const val IS_EVERYTHIG_ADDED_KEY = "isEverythingAdded"
    }

    private val _visitedLiveData = state.getLiveData(VISITED_KEY, listOf<Int>())
    private val visited
        get() = _visitedLiveData.value ?: listOf()
    val visitedLiveData: LiveData<List<Int>>
        get() = _visitedLiveData
    private val _geofencesAdded = state.getLiveData(IS_EVERYTHIG_ADDED_KEY, false)
    private val geofenceAdded
        get() = _geofencesAdded.value ?: false


    fun isVisited(index: Int): Boolean {
        return visited.contains(index)
    }
    fun markAsVisited(index: Int) {
        _visitedLiveData.value = _visitedLiveData.value?.plus(index)
    }

    fun addAllGeofences(geofencingClient: GeofencingClient, geofencePendingIntent: PendingIntent) {
        if (geofenceAdded) return
        MuralPois.data.forEach {
            val geofence = Geofence.Builder()
                    .setRequestId(it.title)
                    .setCircularRegion(it.latLng.latitude, it.latLng.longitude, MuralPois.GEOFENCE_RADIUS_IN_METERS)
                    .setExpirationDuration(MuralPois.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
            val geofencingRequest = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build()
            // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
            // TODO no result handling for now
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Timber.i( "successfully added ${geofence.requestId}")
                }
                addOnFailureListener { exception ->
                    Timber.e(exception.message.toString())
                }
            }
        }
        _geofencesAdded.value = true
    }
}