package com.outgoer.ui.map

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.codec.binary.Base64
import com.google.gson.Gson
import com.outgoer.BuildConfig
import com.outgoer.api.venue.model.GeoFenceResponse
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import timber.log.Timber

private const val TAG = "GeofenceBroadcastReceiv"

class GeofenceBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Get the geofencing event from the intent
        val event = intent?.let { GeofencingEvent.fromIntent(it) }

        // Check if the event has any errors
        if (event != null) {
            if (event.hasError()) {
                Log.e("Geofence error", "Error code: ${event.errorCode}")
                return
            }
        }

        // Get the geofence transition type
        val transitionType = event?.geofenceTransition

        // Check if the event was triggered by entering or exiting a geofence
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
            transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the IDs of the triggered geofences
            val triggeredGeofences = event.triggeringGeofences
            val triggeredGeofenceIds = mutableListOf<String>()
            for (geofence in triggeredGeofences) {
                triggeredGeofenceIds.add(geofence.requestId)
            }

            // Get the transition details (e.g. enter/exit time)
            val transitionTime = event.triggeringLocation.time

            // Do something with the triggered geofence IDs and transition details
            Timber.tag("GeofenceTransition").d("Geofences triggered: $triggeredGeofenceIds, Transition time: $transitionTime")

            // Creating and sending Notification

            triggeredGeofenceIds.firstOrNull {
                Timber.tag("GeofenceTransition").d("triggeredGeofenceIds.first(): ${triggeredGeofenceIds.first()}")
                val jsonString = String(Base64.decodeBase64(it))
                val venueMapInfo = Gson().fromJson(jsonString, GeoFenceResponse::class.java)

                if(isAppRunning(context, BuildConfig.APPLICATION_ID)) {
                    if(transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
                        Timber.tag("Geofence").d("GeoFenceResEntry venueMapInfo: $venueMapInfo")
                        RxBus.publish(RxEvent.GeoFenceResEntry(venueMapInfo))
                    } else {
                        Timber.tag("Geofence").d("GeoFenceResExit venueMapInfo: $venueMapInfo")
                        RxBus.publish(RxEvent.GeoFenceResExit(venueMapInfo))
                    }
                } else {
                    val notificationManager = ContextCompat.getSystemService(
                        context!!,
                        NotificationManager::class.java
                    ) as NotificationManager
                    notificationManager.sendGeofenceEnteredNotification(context, venueMapInfo.name ?: "", venueMapInfo.id ?: 0)
                }
                true
            }
        }
    }

    private fun isAppRunning(context: Context?, packageName: String): Boolean {
        val activityManager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.runningAppProcesses?.apply {
            for (processInfo in this) {
                if (processInfo.processName == packageName) {
                    return true
                }
            }
        }
        return false
    }

}
