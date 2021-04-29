/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        val dataIdx = MuralPois.data.indexOfFirst { it.title == geofenceRequestId }
        if (-1 == dataIdx) {
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

