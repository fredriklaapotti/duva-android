package com.step84.duva

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceTransitionsJobIntentService : JobIntentService() {
    private val TAG = "GeofenceTransitionsJobIntentService"
    private val JOB_ID = 442
    private val CHANNELID = "0"
    private var ctx: Context? = null

    fun enqueueWork(context: Context, intent: Intent) {
        Log.i(TAG, "duva: geofence location in enqueueWork()")
        ctx = context
        enqueueWork(context, GeofenceTransitionsJobIntentService::class.java, JOB_ID, intent)
    }

    override fun onHandleWork(intent: Intent) {
        Log.i(TAG, "duva: geofence location in onHandleWork()")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if(geofencingEvent.hasError()) {
            Log.d(TAG, "duva: geofence has error")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        Log.i(TAG, "duva: geofenceTransition = ${geofenceTransition.toString()}")
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
        //createNotification("JobIntentService", "Received broadcast: $broadcastString")

        if(applicationContext != null) {
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(broadcastIntent)
        }
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

    fun createNotification(title: String, content: String) {
        val builder = NotificationCompat.Builder(this, CHANNELID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build())
        }
    }
}