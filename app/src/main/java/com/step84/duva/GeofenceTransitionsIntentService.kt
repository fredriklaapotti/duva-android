package com.step84.duva

import android.app.IntentService
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceTransitionsIntentService : IntentService("GeofenceTransitionsIntentService") {
    private val TAG = "GeofenceTransitionsIntentService"

    /*
    override fun onCreate() {
        super.onCreate()
    }
    */

    override fun onHandleIntent(intent: Intent?) {
        Log.i(TAG, "duva: geofence in onHandleIntent()")
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if(geofencingEvent.hasError()) {
            Log.d(TAG, "duva: geofence has error")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences)
            Log.i(TAG, "duva: geofence $geofenceTransitionDetails")
        } else {
            Log.d(TAG, "duva: geofence unknown trigger")
        }
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