package com.example.murals3

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.murals3.MapsActivity.Companion.ACTION_GEOFENCE_EVENT
import com.example.murals3.MapsActivity.Companion.ACTION_GEOFENCE_PASSED_EVENT
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import timber.log.Timber


fun errorMessage(context: Context, errorCode: Int): String {
    val resources = context.resources
    return when (errorCode) {
        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> resources.getString(
                R.string.geofence_not_available
        )
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> resources.getString(
                R.string.geofence_too_many_geofences
        )
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> resources.getString(
                R.string.geofence_too_many_pending_intents
        )
        else -> resources.getString(R.string.unknown_geofence_error)
    }
}

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_GEOFENCE_EVENT)
            return

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Timber.e(errorMessage(context, geofencingEvent.errorCode))
            return
        }
        if (geofencingEvent.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER)
            return

        Timber.v(context.getString(R.string.geofence_entered))
        if (geofencingEvent.triggeringGeofences.isEmpty()) {
            Timber.e("No Geofence Trigger Found! Abort mission!")
            return
        }

        val geofenceRequestId = geofencingEvent.triggeringGeofences.first().requestId
        val dataIdx = geofenceRequestId.toIntOrNull()
        if (dataIdx == null) {
            Timber.e("Unknown Geofence: Data not found")
            return
        }

        val newIntent = Intent(context, MapsActivity::class.java)
        newIntent.action = ACTION_GEOFENCE_PASSED_EVENT
        newIntent.putExtra(GeoDataModel.EXTRA_GEOFENCE_INDEX, dataIdx)
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // adding this flag starts the new Activity in a new Task
        context.startActivity(newIntent)

        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        notificationManager?.sendGeofenceEnteredNotification(context, dataIdx)
    }
}

