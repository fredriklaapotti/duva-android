package com.step84.duva

import android.util.Log
import com.google.firebase.firestore.GeoPoint

object Globals {
    private val TAG = "Globals"
    var currentUser: User? = null
    var currentSubscriptions: MutableList<Subscription>? = null
    var currentLocation: GeoPoint? = null
    var activeZone: String = "unknown"
    var allZones: MutableList<Zone>? = null

    fun Globals() {
        // Something
    }

    // TODO: this is pretty!.. ugly
    fun getCurrentZoneName(zoneid: String?): String {
        var zoneName = "unknown"
        if(currentSubscriptions != null && allZones != null && currentUser != null) {
            Log.i(TAG, "duva: getCurrentZoneName() ----------------------------")
            for (subscription in currentSubscriptions!!) {
                Log.i(TAG, "duva: getCurrentZoneName() looping subscriptions = ${subscription.zone}")
                if (subscription.active) {
                    Log.i(TAG, "duva: getCurrentZoneName() found active subscription = ${subscription.zone}")
                    for (zone in allZones!!) {
                        Log.i(TAG, "duva: getCurrentZoneName() looping zones = ${zone.id}")
                        if (subscription.zone == zone.id) {
                            Log.i(
                                TAG,
                                "duva: getCurrentZoneName() found match, subscription.zone == ${subscription.zone}, zone.id == ${zone.id}"
                            )
                            zoneName = zone.name
                        }
                    }
                }
            }
        } else if(zoneid != null && zoneid != "exit") {
            Log.i(TAG, "duva: getCurrentZoneName() not logged in, going by zone id = $zoneid")
            zoneName = getZoneNameFromZoneId(zoneid)
        } else if(zoneid == "exit") {
            Log.i(TAG, "duva: getCurrentZoneName() not logged in, exit geofence, resetting zone")
            zoneName = "unknown"
        } else {
            Log.i(TAG, "duva: getCurrentZoneName() not logged in, no zoneid, no geofence exit detected, going by last zoneName = ${this.activeZone}")
            zoneName = this.activeZone
        }

        this.activeZone = zoneName
        return zoneName
    }

    fun getZoneNameFromZoneId(zoneid: String): String {
        var zoneName = "unknown"
        if(allZones != null) {
            for(zone in allZones!!) {
                if(zone.id == zoneid)
                    zoneName = zone.name
            }
        }
        return zoneName
    }
}