package com.example.murals3

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var viewModel: GeoDataModel
    private lateinit var map: GoogleMap

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 52
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 43
        private const val TAG = "MapsActivity"
        internal const val ACTION_GEOFENCE_EVENT =
                "HuntMainActivity.treasureHunt.action.ACTION_GEOFENCE_EVENT"
    }

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
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart()")
        if (this::map.isInitialized) {
            enableMyLocation()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult() resultCode: $resultCode")
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // RESULT_OK -1 (OK), RESULT_CANCELED 0 (NO THANKS)
            // sometimes after clicking OK, this second check is executed too early,
            // and the check result is failure even if actually the location is eventually turned on
            // hence the delay
            Thread.sleep(500)
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady()")
        map = googleMap

        map.uiSettings.isZoomControlsEnabled = true
        enableMyLocation()

        MuralPois.data.forEach {
            val marker = map.addMarker(MarkerOptions().position(it.latLng).title(it.title))
            marker.tag = it.link
        }
        val zoomLevel = 17f
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(MuralPois.data.first().latLng, zoomLevel))
        map.setOnInfoWindowClickListener{
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.tag.toString())))
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
        Log.d(TAG,"checking location turned on/off")
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_LOW_POWER
        val locationSettingsRequest = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build()
        val settingsClient = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(locationSettingsRequest)
        locationSettingsResponseTask.addOnCompleteListener { locationSettingsResponse ->
            Log.d(TAG, "OnCompleteListener() locationSettingsResponse.isSuccessful: ${locationSettingsResponse.isSuccessful}")
            if (locationSettingsResponse.isSuccessful) {
                map.isMyLocationEnabled = true
                startGeofenceForNotFound()
            }
        }
        locationSettingsResponseTask.addOnFailureListener { exception ->
            Log.d(TAG, "OnFailureListener() exception is ResolvableApiException: ${exception is ResolvableApiException} resolve: $resolve")
            if (exception is ResolvableApiException && resolve) {
                try {
                    Log.d(TAG, "calling exception.startResolutionForResult()")
                    exception.startResolutionForResult(this, REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, "Error getting location settings resolution: ${sendEx.message}")
                }
            } else {
                Log.d(TAG, "showing snackbar")
                Snackbar.make(binding.activityMapsMain,
                        R.string.location_required_error, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok) {
                            Log.d(TAG, "calling checkDeviceLocationSettingsAndStartGeofence()")
                            checkDeviceLocationSettingsAndStartGeofence()
                        }
                        .show()
            }
        }
    }

    private fun startGeofenceForNotFound() {
        Log.d(TAG, "start geofence!")
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        if (requestCode != REQUEST_LOCATION_PERMISSION)
            return
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