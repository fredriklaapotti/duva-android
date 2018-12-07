package com.step84.duva

import android.app.IntentService
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceTransitionsIntentService : IntentService("GeofenceTransitionsIntentService") {
    private val TAG = "GeofenceTransitionsIntentService"

    override fun onHandleIntent(intent: Intent?) {
        Log.i(TAG, "duva: geofence in onHandleIntent()")
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if(geofencingEvent.hasError()) {
            Log.d(TAG, "duva: geofence has error")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        val geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences)

        val broadcastString = when(geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "com.step84.duva.GEOFENCE_ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "com.step84.duva.GEOFENCE_EXIT"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "com.step84.duva.GEOFENCE_DWELL"
            else -> "com.step84.duva.GEOFENCE_UNKNOWN"
        }

        val broadcastIntent = Intent(broadcastString).apply {
            putExtra("zoneid", triggeringGeofences[0].requestId.toString())
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
        Log.i(TAG, "duva: geofence $geofenceTransitionDetails")
    }

    private fun getGeofenceTransitionDetails(geofenceTransition: Int, triggeringGeofences: MutableList<Geofence>): String {
        val triggeringGeofencesIdsList: MutableList<String> = mutableListOf()
        val geofenceTransitionString = getTransitionString(geofenceTransition)
        Log.i(TAG, "duva: geofence geofenceTransitionString = $geofenceTransitionString")
        for(geofence in triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.requestId)
        }
        val triggeringGeofencesIdsString: String = TextUtils.join(", ", triggeringGeofencesIdsList)
        return "$geofenceTransitionString:$triggeringGeofencesIdsString"
    }

    private fun getTransitionString(transitionType: Int): String {
        when(transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> return "enter"
            Geofence.GEOFENCE_TRANSITION_EXIT -> return "exit"
            Geofence.GEOFENCE_TRANSITION_DWELL -> return "dwell"
        }

        return "unknown"
    }
}