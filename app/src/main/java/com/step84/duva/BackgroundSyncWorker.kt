package com.step84.duva

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class BackgroundSyncWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    private val TAG = "BackgroundSyncWorker"
    private val CHANNELID = "0"

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var geofencingClientWorker: GeofencingClient
    private var geofenceList: MutableList<Geofence> = mutableListOf()

    private val mContext: Context = appContext
    private val db = FirebaseFirestore.getInstance()

    private var syncAllZones: MutableList<Zone>? = null
    private var syncAllSubscriptions: MutableList<Subscription>? = null

    private val geofencePendingIntent: PendingIntent by lazy {
        Log.i(TAG, "duva: geofence intent after getBroadcast()")
        val intent = Intent(applicationContext, GeofenceBroadcastReceiver::class.java)
        //applicationContext.sendBroadcast(intent)
        PendingIntent.getBroadcast(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun doWork(): Result = try {
        Log.i(TAG, "duva: background in doWork()")
        geofencingClientWorker = LocationServices.getGeofencingClient(applicationContext)
        // Result.failure, Result.retry


        createNotification("testing from background", "Where: ")
        startLocationUpdates()



        Result.success()
    } catch(e: Throwable) {
        Log.e(TAG, "duva: error = " + e.message, e)
        Result.failure()
    }

    fun calculateDistance(p1: GeoPoint, p2: GeoPoint): Double {
        val c: Double =
            Math.sin(Math.toRadians(p1.latitude)) * Math.sin(Math.toRadians(p2.latitude)) +
                    Math.cos(Math.toRadians(p1.latitude)) * Math.cos(Math.toRadians(p2.latitude)) *
                    Math.cos(Math.toRadians(p2.longitude)) - Math.toRadians(p1.longitude)
        if(c > 0) { Math.min(1.toDouble(), c) }
        else { Math.max(-1.toDouble(), c) }
        return 3959 * 1.609 * 1000 * Math.acos(c)
    }

    fun haversine(p1: GeoPoint, p2: GeoPoint): Double {
        val dLat = Math.toRadians(p1.latitude - p2.latitude)
        val dLon = Math.toRadians(p1.longitude - p2.longitude)
        val originLat = Math.toRadians(p2.latitude)
        val destinationLat = Math.toRadians(p1.latitude)

        val a = Math.pow(Math.sin(dLat / 2), 2.toDouble()) + Math.pow(Math.sin(dLon / 2), 2.toDouble()) * Math.cos(originLat) * Math.cos(destinationLat)
        val c = 2 * Math.asin(Math.sqrt(a))
        return 6372.8 * c * 1000
    }

    fun isInGeofence(zone: Zone, userPosition: GeoPoint) : Boolean {
        //Log.i(TAG, "duva: sync calculating distance between: " + zone.location.toString() + " and " + userPosition.toString())
        //Log.i(TAG, "duva: sync distance = " + haversine(zone.location, userPosition))
        return haversine(zone.location, userPosition) < zone.radius
    }

    fun checkZones(currentLocation: GeoPoint?) {
        val mutableIterator = syncAllZones!!.iterator()

        currentLocation?.let {
            for(zone in mutableIterator) {
                Log.i(TAG, "duva: isInGeoFence (zone: ${zone.id})? = " + isInGeofence(zone, currentLocation).toString())
            }
        }

    }

    fun createNotification(title: String, content: String) {
        val builder = NotificationCompat.Builder(applicationContext, CHANNELID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(1, builder.build())
        }
    }

    private fun startLocationUpdates() {
        Log.i(TAG, "duva: in startLocationUpdates()")
        if(checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            /*
            locationRequest = LocationRequest().apply {
                interval = 60 * 1000
                fastestInterval = 20 * 1000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val client: SettingsClient = LocationServices.getSettingsClient(applicationContext)
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

            task.addOnSuccessListener { locationSettingsResponse ->
                Log.i(TAG, "duva: locationSettingsResponse task successful")
            }

            task.addOnFailureListener { exception ->
                if(exception is ResolvableApiException) {
                    try {
                        //exception.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.d(TAG, "duva: locationSettingsResponse failed")
                    }
                }
            }

            registerLocationListener()

             */
            //fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

            /**
             * The following code works, by starting location updates followed by database calls and manual geofence checking.
             * However, it might be enough just to probe lastLocation every 15 minutes to invoke geofence events in the JobIntentService.
             * Commenting this one out and trying with copy below.
             */
            /*
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    Log.i(TAG, "duva: sync = " + location?.latitude + ", " + location?.longitude)
                    val gp = GeoPoint(location!!.latitude, location!!.longitude)

                    db.collection("zones").get()
                        .addOnSuccessListener { documents ->
                            syncAllZones = documents.toObjects(Zone::class.java)
                            Log.i(TAG, "duva: sync syncAllZones = " + syncAllZones?.toString())
                            checkZones(gp)
                        }
                        .addOnFailureListener { exception ->
                            Log.d(TAG, "duva: sync failed to read from database: ", exception)
                        }

                    // TODO: make sure we receive inputData with uid
                    Log.i(TAG, "duva: sync user from inputData() = " + inputData.getString("uid"))
                    db.collection("subscriptions").whereEqualTo("user", inputData.getString("uid")).get()
                        .addOnSuccessListener { documents ->
                            syncAllSubscriptions = documents.toObjects(Subscription::class.java)
                            Log.i(TAG, "duva: sync syncAllSubscriptions= " + syncAllSubscriptions?.toString())
                        }
                        .addOnFailureListener { exception ->
                            Log.d(TAG, "duva: sync failed to read from database: ", exception)
                        }

                }
             */

            /**
             * This didn't work.
             */
            /*
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    Log.i(TAG, "duva: sync = " + location?.latitude + ", " + location?.longitude)
                }
                .addOnFailureListener {
                    Log.d(TAG, "duva: sync lastLocation in worker failed")
                }
             */


            /**
             * Third time the charm. Now trying if a geofence check might trigger an intent to the JobIntentService.
             * Still, it gets more tempting to just rewrite everything for the background sync worker.
             */
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    Log.i(TAG, "duva: sync = " + location?.latitude + ", " + location?.longitude)
                    val gp = GeoPoint(location!!.latitude, location!!.longitude)

                    db.collection("zones").get()
                        .addOnSuccessListener { documents ->
                            syncAllZones = documents.toObjects(Zone::class.java)
                            setupGeofences(documents.toObjects(Zone::class.java))
                            Log.i(TAG, "duva: sync syncAllZones = " + syncAllZones?.toString())
                            //checkZones(gp)
                        }
                        .addOnFailureListener { exception ->
                            Log.d(TAG, "duva: sync failed to read from database: ", exception)
                        }

                    // TODO: make sure we receive inputData with uid
                    Log.i(TAG, "duva: sync user from inputData() = " + inputData.getString("uid"))
                    db.collection("subscriptions").whereEqualTo("user", inputData.getString("uid")).get()
                        .addOnSuccessListener { documents ->
                            syncAllSubscriptions = documents.toObjects(Subscription::class.java)
                            Log.i(TAG, "duva: sync syncAllSubscriptions= " + syncAllSubscriptions?.toString())
                        }
                        .addOnFailureListener { exception ->
                            Log.d(TAG, "duva: sync failed to read from database: ", exception)
                        }

                }
        }
    }

    private fun registerLocationListener() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                Log.i(TAG, "duva: geofence location in onLocationResult()")
                for(location in locationResult.locations) {
                    //Log.i(TAG, "duva: location geofence looping in registerLocationListener()")
                    Globals.currentLocation = GeoPoint(location.latitude, location.longitude)
                    /*
                    if(Globals.currentUser != null && Globals.currentUser!!.id != "0") {
                        Firestore.updateField("users", Globals.currentUser!!.id, "lastLocation", Globals.currentLocation, object: FirestoreCallback {
                            override fun onSuccess() {}
                            override fun onFailed() {}
                        })
                    }
                    */

                    Log.i(TAG, "duva: location geofence currentLocation = " + Globals.currentLocation.toString())
                    //googleMapInterface?.onLocationUpdate(GeoPoint(location.latitude, location.longitude))
                }
            }
        }
    }

    fun setupGeofences(zones: MutableList<Zone>) {
        Log.i(TAG, "duva: SYNC in setupGeofences()")
        for(zone in zones) {
            //Log.i(TAG, "duva: SYNC geofence zone found = " + zone.name)
            geofenceList.add(Geofence.Builder()
                .setRequestId(zone.id)
                .setCircularRegion(zone.location.latitude, zone.location.longitude, zone.radius.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setLoiteringDelay(20 * 1000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build())
        }

        Log.i(TAG, "duva: SYNC geofence list = " + geofenceList.toString())

        if(checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.i(TAG, "duva: SYNC geofence adding or removing geofences")
            if(this::geofencingClientWorker.isInitialized) {
                Log.i(TAG, "duva: SYNC geofence before geofencingClient.addGeofences()")
                geofencingClientWorker.addGeofences(getGeofencingRequest(), geofencePendingIntent)?.run {
                    addOnSuccessListener {
                        Log.i(TAG, "duva: SYNC geofence added, geofencePendingIntent = " + geofencePendingIntent.toString())
                        Globals.geofencesAdded = true
                    }
                    addOnFailureListener {
                        Log.d(TAG, "duva: SYNC failed to add geofence" + exception.toString())
                        Globals.geofencesAdded = false
                    }
                }
            }
            Log.i(TAG, "duva: SYNC geofence exiting from checkPermission block")
        }
    }

    private fun getGeofencingRequest(): GeofencingRequest {
        Log.i(TAG, "duva: SYNC geofence getGeofencingRequest()")
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            addGeofences(geofenceList)
        }.build()
    }

    private fun checkPermission(permissionString: String): Boolean = ContextCompat.checkSelfPermission(applicationContext, permissionString) == PackageManager.PERMISSION_GRANTED
}