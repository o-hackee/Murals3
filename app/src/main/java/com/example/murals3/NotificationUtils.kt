package com.example.murals3

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.example.murals3.MapsActivity.Companion.ACTION_COMPLETE_EVENT
import com.example.murals3.MapsActivity.Companion.ACTION_GEOFENCE_NOTIFY_EVENT
import timber.log.Timber

private const val CHANNEL_ID = "GeofenceChannel"
private const val COMPLETE_NOTIFICATION_ID = 1

/*
 * We need to create a NotificationChannel associated with our CHANNEL_ID before sending a
 * notification.
 */
fun createChannel(context: Context) {
    val notificationChannel = NotificationChannel(CHANNEL_ID,
        context.getString(R.string.channel_name),
        NotificationManager.IMPORTANCE_HIGH
    )

    notificationChannel.enableLights(true)
    notificationChannel.lightColor = Color.GREEN
    notificationChannel.enableVibration(true)
    notificationChannel.description = context.getString(R.string.notification_channel_description)

    val notificationManager = context.getSystemService(NotificationManager::class.java)
    notificationManager.createNotificationChannel(notificationChannel)
}

private fun getUniqueNotificationId(): Int {
    var notificationId: Int
    do {
        notificationId = System.currentTimeMillis().toInt()
    } while (notificationId == COMPLETE_NOTIFICATION_ID)
    return notificationId
}

fun NotificationManager.sendGeofenceEnteredNotification(context: Context, foundIndex: Int) {
    val contentIntent = Intent(context, MapsActivity::class.java)
    contentIntent.action = ACTION_GEOFENCE_NOTIFY_EVENT
    contentIntent.putExtra(GeoDataModel.EXTRA_GEOFENCE_INDEX, foundIndex)
    val notificationId = getUniqueNotificationId()
    Timber.i("notificationId: $notificationId")
    val contentPendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    // We use the name resource ID from the LANDMARK_DATA along with content_text to create
    // a custom message when a Geofence triggers.
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_map)
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(context.getString(R.string.content_text,
            MuralPois.data[foundIndex].title))
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    notify(notificationId, builder.build())
}

fun NotificationManager.sendCompleteNotification(context: Context) {
    val contentIntent = Intent(context, MapsActivity::class.java)
    contentIntent.action = ACTION_COMPLETE_EVENT
    val contentPendingIntent = PendingIntent.getActivity(
            context,
            COMPLETE_NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
    )

    // We use the name resource ID from the LANDMARK_DATA along with content_text to create
    // a custom message when a Geofence triggers.
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_map)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Tour is complete!")
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

    notify(COMPLETE_NOTIFICATION_ID, builder.build())
}


