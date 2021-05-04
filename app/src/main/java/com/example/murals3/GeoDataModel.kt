package com.example.murals3

import android.app.PendingIntent
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber

class GeoDataModel(state: SavedStateHandle) : ViewModel() {

    companion object {
        const val EXTRA_GEOFENCE_INDEX = "GEOFENCE_INDEX"
        const val POIS_KEY = "pois"
    }

    enum class PoiStatus {
        NotActivated,
        Activated,
        Visited
    }

    data class PoiState(
            val activationTimestamp: Long,
            var status: PoiStatus
    )

    private val _poiLiveData = state.getLiveData(POIS_KEY, MuralPois.data.map {
        PoiState(-1, PoiStatus.NotActivated)
    })
    private val pois
        get() = _poiLiveData.value ?: listOf()
    val poiLiveData: LiveData<List<PoiState>>
        get() = _poiLiveData
    private var lastUpdated = -1


    fun getStatus(index: Int): PoiStatus {
        if (pois.size <= index) {
            Timber.e("asked $index, only ${pois.size} pois available")
            return PoiStatus.NotActivated
        }
        return pois[index].status
    }
    fun setLastUpdated(index: Int, newStatus: PoiStatus) {
        val value = _poiLiveData.value
        if (value != null) {
            lastUpdated = index
            Timber.d("lastUpdated: $lastUpdated")
            val updated = value.toMutableList()
            updated[index].status = newStatus
            _poiLiveData.value = updated
        }
    }
    fun resetLastUpdated(): Triple<Boolean, PoiStatus, Int> {
        if (lastUpdated < 0) return Triple(false, PoiStatus.NotActivated, -1)
        if (lastUpdated >= MuralPois.data.size) {
            Timber.e("Unacceptable last updated index detected: $lastUpdated")
            lastUpdated = -1
            return Triple(false, PoiStatus.NotActivated, -1)
        }
        val index = lastUpdated
        lastUpdated = -1
        return Triple(true, pois[index].status, index)
    }

    // TODO delete this, debug only
    private fun add100(geofencingClient: GeofencingClient, geofencePendingIntent: PendingIntent) {
        val point = LatLng(48.1977, 16.3671)
        (1..100).forEach {
            val geofence = Geofence.Builder()
                    .setRequestId(it.toString())
                    .setCircularRegion(point.latitude, point.longitude, MuralPois.GEOFENCE_RADIUS_IN_METERS)
                    .setExpirationDuration(MuralPois.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
            val geofencingRequest = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build()
            // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).addOnCompleteListener {
                Timber.i("pseudo $it added")
            }
        }
    }
    private fun remove100(geofencingClient: GeofencingClient, geofencePendingIntent: PendingIntent) {
        geofencingClient.removeGeofences(geofencePendingIntent)
    }

    fun addAllGeofences(geofencingClient: GeofencingClient, geofencePendingIntent: PendingIntent) {

        add100(geofencingClient, geofencePendingIntent)

        val geofenceIsActive = pois.any { it.status == PoiStatus.Activated }
        Timber.d("geofenceIsActive: $geofenceIsActive")
        if (geofenceIsActive) return
        pois.forEachIndexed { idx, state ->
            if (idx == 1) {
                remove100(geofencingClient, geofencePendingIntent)
            }
            val poiData = MuralPois.data[idx]
            val geofence = Geofence.Builder()
                    .setRequestId(idx.toString())
                    .setCircularRegion(poiData.latLng.latitude, poiData.latLng.longitude, MuralPois.GEOFENCE_RADIUS_IN_METERS)
                    .setExpirationDuration(MuralPois.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
            val geofencingRequest = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build()
            state.status = PoiStatus.Activated
            // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Timber.i( "successfully added ${poiData.title}")
                }
                addOnFailureListener { exception ->
                    Timber.e("failed to add ${poiData.title}: ${exception.message}")
                    setLastUpdated(idx, PoiStatus.NotActivated)
                }
            }
        }
    }
}