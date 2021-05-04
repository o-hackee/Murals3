package com.example.murals3

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Handler
import android.os.Parcelable
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.LatLng
import kotlinx.parcelize.Parcelize
import timber.log.Timber

class GeoDataModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    companion object {
        const val EXTRA_GEOFENCE_INDEX = "GEOFENCE_INDEX"
        const val POIS_KEY = "pois"
    }

    enum class PoiStatus {
        NotActivated,
        Activated,
        Visited
    }

    @Parcelize
    data class PoiState(
            var activationTimestamp: Long,
            var status: PoiStatus
    ) : Parcelable

    private val _poiLiveData = savedStateHandle.getLiveData(POIS_KEY, MuralPois.data.map {
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
    fun setLastUpdated(index: Int, newStatus: PoiStatus, checkComplete: Boolean = false) {
        val value = _poiLiveData.value
        if (value != null) {
            lastUpdated = index
            Timber.d("lastUpdated: $lastUpdated")
            val updated = value.toMutableList()
            updated[index].status = newStatus
            _poiLiveData.value = updated

            if (checkComplete && !geofenceIsActive()) {
                // send a notification to allow the user to decide whether to restart
                Handler().postDelayed({
                    val notificationManager = ContextCompat.getSystemService(getApplication(), NotificationManager::class.java)
                    notificationManager?.sendCompleteNotification(getApplication())
                }, 5000)
            }
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

    fun geofenceIsActive() =
        pois.any { it.status == PoiStatus.Activated }

    fun checkExpired(): Boolean {
        var setIdx = -1
        pois.forEachIndexed { idx, it ->
            if (it.status == PoiStatus.Activated) {
                if (System.currentTimeMillis() - it.activationTimestamp > MuralPois.GEOFENCE_EXPIRATION_IN_MILLISECONDS) {
                    Timber.d("$idx ${MuralPois.data[idx].title} expired!")
                    it.status = PoiStatus.NotActivated
                    setIdx = idx
                }
            }
        }
        if (setIdx == -1) {
            return false
        }
        setLastUpdated(setIdx, PoiStatus.NotActivated)
        return true
    }

    fun restart() {
        val v = _poiLiveData.value
        if (v != null) {
            val u = v.toMutableList()
            u.forEach { it.status = PoiStatus.NotActivated }
            _poiLiveData.value = u
        }
    }

    // TODO delete this, debug only
    private fun add100(geofencingClient: GeofencingClient, geofencePendingIntent: PendingIntent) {
        return
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
        return
        geofencingClient.removeGeofences(geofencePendingIntent)
    }

    fun addAllGeofences(geofencingClient: GeofencingClient, geofencePendingIntent: PendingIntent) {

        val notificationManager = ContextCompat.getSystemService(getApplication(), NotificationManager::class.java)
        notificationManager?.cancelAll()

        add100(geofencingClient, geofencePendingIntent)

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
                    state.activationTimestamp = System.currentTimeMillis()
                    if (idx == pois.lastIndex) {
                        setLastUpdated(idx, PoiStatus.Activated)
                    }
                }
                addOnFailureListener { exception ->
                    Timber.e("failed to add ${poiData.title}: ${exception.message}")
                    setLastUpdated(idx, PoiStatus.NotActivated, true)
                }
            }
        }
    }
}