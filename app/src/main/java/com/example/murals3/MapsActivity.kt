package com.example.murals3

import android.Manifest
import android.annotation.TargetApi
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateViewModelFactory
import com.example.murals3.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

// TODO icon
// какой-то geofence-статус?
// TODO потестить перезапуски, background и.т.д.

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, RestartRequestDialog.RestartRequestDialogListener {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var viewModel: GeoDataModel
    private lateinit var map: GoogleMap

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 52
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 43
        internal const val ACTION_GEOFENCE_EVENT = "MapsActivity.action.ACTION_GEOFENCE_EVENT"
        internal const val ACTION_GEOFENCE_PASSED_EVENT = "MapsActivity.action.ACTION_GEOFENCE_PASSED"
        internal const val ACTION_GEOFENCE_NOTIFY_EVENT = "MapsActivity.action.ACTION_GEOFENCE_NOTIFY"
        internal const val ACTION_COMPLETE_EVENT = "MapsActivity.action.ACTION_COMPLETE_EVENT"
    }

    // TODO добавить проверку expired

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        viewModel = ViewModelProvider(this, SavedStateViewModelFactory(this.application,
                this)).get(GeoDataModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        geofencingClient = LocationServices.getGeofencingClient(this)

        // Create channel for notifications
        createChannel(this)

        viewModel.poiLiveData.observe(this) {
            Timber.d("observe() $it")
            val (got, status, index) = viewModel.resetLastUpdated()
            Timber.d("$got $status $index")
            if (!got) {
                return@observe
            }
            if (status == GeoDataModel.PoiStatus.Visited) {
                geofencingClient.removeGeofences(listOf(MuralPois.data[index].title))
                Timber.i("remove geofence ${MuralPois.data[index].title}")
            }
            // дорого, но что поделать
            map.clear()
            addMarkers()
        }

        binding.resetButton.setOnClickListener {
            RestartRequestDialog().show(supportFragmentManager, "UserActionRestartTag")
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart()")
        if (this::map.isInitialized) {
            enableMyLocation()
        }
    }

    override fun onDestroy() {
        geofencingClient.removeGeofences(geofencePendingIntent)
        Timber.i("remove all geofences")
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("onActivityResult() resultCode: $resultCode")
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // RESULT_OK -1 (OK), RESULT_CANCELED 0 (NO THANKS)
            // sometimes after clicking OK, this second check is executed too early,
            // and the check result is failure even if actually the location is eventually turned on
            // hence the delay
            Thread.sleep(500)
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.d("onNewIntent() action: ${intent?.action}")
        if (intent == null) return
        when (intent.action) {
            ACTION_GEOFENCE_PASSED_EVENT, ACTION_GEOFENCE_NOTIFY_EVENT -> {
                val extras = intent.extras
                if (extras != null && extras.containsKey(GeoDataModel.EXTRA_GEOFENCE_INDEX)) {
                    val index = extras.getInt(GeoDataModel.EXTRA_GEOFENCE_INDEX)
                    Timber.d("index: $index")
                    if (intent.action == ACTION_GEOFENCE_PASSED_EVENT) {
                        viewModel.setLastUpdated(index, GeoDataModel.PoiStatus.Visited, true)
                    } else {
                        map.moveCamera(CameraUpdateFactory.newLatLng(MuralPois.data[index].latLng))
                    }
                }
            }
            ACTION_COMPLETE_EVENT -> {
                RestartRequestDialog().show(supportFragmentManager, "RestartRequestDialogTag")
            }
        }
    }

    override fun confirmButtonClicked() {
        val notificationManager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)
        notificationManager?.cancelAll()
        viewModel.restart()
        checkDeviceLocationSettingsAndStartGeofence()
    }

    override fun cancelButtonClicked() {
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        Timber.d("onMapReady()")
        map = googleMap

        map.uiSettings.isZoomControlsEnabled = true
        enableMyLocation()

        addMarkers()
        val zoomLevel = 17f
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(MuralPois.data.first().latLng, zoomLevel))
        map.setOnInfoWindowClickListener{
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.tag?.toString())))
        }
    }

    private fun addMarkers() {
        Timber.i("adding markers...")
        MuralPois.data.forEachIndexed { index, value ->
            val markerOptions = MarkerOptions().position(value.latLng).title(value.title)
            val status = viewModel.getStatus(index)
            if (status == GeoDataModel.PoiStatus.Visited) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            } else if (status == GeoDataModel.PoiStatus.NotActivated) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            }
            val marker = map.addMarker(markerOptions)
            marker?.tag = value.link
        }
    }

    @TargetApi(29)
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQUEST_LOCATION_PERMISSION)
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        Timber.d("checking location turned on/off")
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_LOW_POWER
        val locationSettingsRequest = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build()
        val settingsClient = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(locationSettingsRequest)
        locationSettingsResponseTask.addOnCompleteListener { locationSettingsResponse ->
            Timber.d("OnCompleteListener() locationSettingsResponse.isSuccessful: ${locationSettingsResponse.isSuccessful}")
            if (locationSettingsResponse.isSuccessful) {
                map.isMyLocationEnabled = true
                val expiredDetected = viewModel.checkExpired()

                val geofenceIsActive = viewModel.geofenceIsActive()
                Timber.d("geofenceIsActive: $geofenceIsActive")
                if (!geofenceIsActive) {
                    if (expiredDetected) {
                        RestartRequestDialog(true).show(supportFragmentManager, "ExpiredDialogTag")
                    } else {
                        viewModel.addAllGeofences(geofencingClient, geofencePendingIntent)
                    }
                }
            }
        }
        locationSettingsResponseTask.addOnFailureListener { exception ->
            Timber.d("OnFailureListener() exception is ResolvableApiException: ${exception is ResolvableApiException} resolve: $resolve")
            if (exception is ResolvableApiException && resolve) {
                try {
                    Timber.d("calling exception.startResolutionForResult()")
                    exception.startResolutionForResult(this, REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Timber.e("Error getting location settings resolution: ${sendEx.message}")
                }
            } else {
                Timber.d("showing snackbar")
                Snackbar.make(binding.activityMapsMain,
                        R.string.location_required_error, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok) {
                            Timber.d("calling checkDeviceLocationSettingsAndStartGeofence()")
                            checkDeviceLocationSettingsAndStartGeofence()
                        }
                        .show()
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        if (requestCode != REQUEST_LOCATION_PERMISSION)
            return
        Timber.d("grantResults: ${grantResults.joinToString()}")
        if (grantResults.any { it == PackageManager.PERMISSION_DENIED}) {
            Snackbar.make(
                    binding.activityMapsMain,
                    R.string.permission_denied_explanation,
                    Snackbar.LENGTH_INDEFINITE
            )
                    .setAction(R.string.settings) {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()
        } else {
            enableMyLocation()
        }
    }
}