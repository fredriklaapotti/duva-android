package com.step84.duva

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    private val TAG = "GeofenceBroadcastReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "duva: geofence broadcast received in class = " + intent.toString())

        /**
         * JobIntentService test
         */

        if(context != null && intent != null) {
            Log.i(TAG, "duva: geofence in BroadcastReceiver() context != null and intent != null")
            GeofenceTransitionsJobIntentService().enqueueWork(context, intent)
        } else {
            Log.d(TAG, "duva: geofence in BroadcastReceiver() context or intent == null")
        }

        /**
         * Experiment with JobIntentService instead
         * The following code works with app in foreground
         */
        /*
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

        Log.i(TAG, "duva: geofence broadcastString == $broadcastString")

        val broadcastIntent = Intent(broadcastString).apply {
            putExtra("zoneid", triggeringGeofences[0].requestId.toString())
        }

        Log.i(TAG, "duva: geofence broadcastIntent = $broadcastIntent")

        LocalBroadcastManager.getInstance(context!!).sendBroadcast(broadcastIntent)
        Log.i(TAG, "duva: geofence $geofenceTransitionDetails")
         */
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