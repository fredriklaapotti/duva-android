package com.step84.duva

import android.Manifest
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.GeoPoint
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(),
    HomeFragment.OnFragmentInteractionListener,
    ZonesFragment.OnFragmentInteractionListener,
    SettingsFragment.OnFragmentInteractionListener {

    private val TAG = "MainActivity"
    private val requestCodeAccessFineLocation = 101
    private val requestCodeWriteExternalStorage = 102
    private val requestCodeRecordAudio = 103

    var currentUser: User? = null
    var currentSubscriptions: MutableList<Subscription>? = null
    var currentLocation: GeoPoint? = null
    var allZones: MutableList<Zone>? = null

    private lateinit var auth: FirebaseAuth

    private var googleMapInterface: GoogleMapInterface? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var geofencingClient: GeofencingClient
    private var geofenceList: MutableList<Geofence> = mutableListOf()
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
        Log.i(TAG, "duva: geofence starting intent service")
        PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                switchFragment(HomeFragment(), "HomeFragment")
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_zones -> {
                switchFragment(ZonesFragment(), "ZonesFragment")
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {
                switchFragment(SettingsFragment(), "SettingsFragment")
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private val br = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i(TAG, "duva: geofence received intent in onReceive() in MainActivity")
            when(intent?.action) {
                "com.step84.duva.GEOFENCE_ENTER" -> geofenceTransition("enter", intent.extras!!.getString("zoneid", "0"))
                "com.step84.duva.GEOFENCE_EXIT" -> geofenceTransition("exit", intent.extras!!.getString("zoneid", "0"))
            }
        }
    }

    private val filter = IntentFilter("com.step84.duva.GEOFENCE_ENTER").apply {
        addAction("com.step84.duva.GEOFENCE_EXIT")
        addAction("com.step84.duva.GEOFENCE_DWELL")
    }

    private fun switchFragment(f: Fragment, t: String) {
        supportFragmentManager.beginTransaction().replace(R.id.container, f, t).commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        switchFragment(HomeFragment(), "HomeFragment")
        setupPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        setupPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setTimestampsInSnapshotsEnabled(true)
            .build()
        firestore.firestoreSettings = settings

        auth = FirebaseAuth.getInstance()
        geofencingClient = LocationServices.getGeofencingClient(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(br, filter)

        setupUser()
        setupSubscriptions()
        setupZones()
        setMapListener(ZonesFragment())
        setupPermission(Manifest.permission.RECORD_AUDIO)

        Log.i(TAG, "duva: currentUser object in MainActivity = " + currentUser?.lastLocation) // Should return null
    }

    override fun onFragmentInteraction(uri: Uri) {
    }

    override fun onStart() {
        super.onStart()
        startLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
        if(auth.currentUser != null && currentUser != null && currentLocation != null) {
            Firestore.updateField("users", currentUser!!.id, "lastLocation", currentLocation, object: FirestoreCallback {
                override fun onSuccess() {}
                override fun onFailed() {}
            })
        }
    }

    override fun onPause() {
        super.onPause()
        if(auth.currentUser != null && currentUser != null && currentLocation != null) {
            Firestore.updateField("users", currentUser!!.id, "lastLocation", currentLocation, object: FirestoreCallback {
                override fun onSuccess() {}
                override fun onFailed() {}
            })
        }
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        unregisterReceiver(br)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "duva: in onActivityResult")

        if(requestCode == 200) {
            Log.i(TAG, "duva: requestCode == 200 in onActivityResult")
        }
    }

    private fun startLocationUpdates() {
        if(checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            locationRequest = LocationRequest().apply {
                interval = 10 * 1000
                fastestInterval = 5 * 1000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val client: SettingsClient = LocationServices.getSettingsClient(this)
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

            task.addOnSuccessListener { locationSettingsResponse ->
                //
            }

            task.addOnFailureListener { exception ->
                if(exception is ResolvableApiException) {
                    try {
                        //exception.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error
                    }
                }
            }

            registerLocationListener()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            Log.i(TAG, "duva: Location updates started")
        }
    }

    private fun registerLocationListener() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for(location in locationResult.locations) {
                    currentLocation = GeoPoint(location.latitude, location.longitude)
                    //googleMapInterface?.onLocationUpdate(GeoPoint(location.latitude, location.longitude))
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.i(TAG, "Location updates stopped")
    }

    private fun setMapListener(listener: GoogleMapInterface) {
        this.googleMapInterface = listener
    }

    private fun setupUser() {
        Firestore.userListener(auth.currentUser, object: FirestoreListener<User> {
            override fun onStart() {
                //Something
            }

            override fun onSuccess(obj: User) {
                currentUser = obj
                val fragment = supportFragmentManager.findFragmentByTag("HomeFragment")
                if(fragment != null && fragment is HomeFragment) {
                    fragment.updateUI(auth.currentUser, currentUser)
                }
            }

            override fun onFailed() {
                // Something
            }
        })
    }

    private fun setupSubscriptions() {
        Firestore.subscriptionsListener(auth.currentUser, object: FirestoreListener<MutableList<Subscription>> {
            override fun onStart() {
                // Something
            }

            override fun onSuccess(obj: MutableList<Subscription>) {
                currentSubscriptions = obj
                for(subscription in obj) {
                    Log.i(TAG, "duva: subsription found = " + subscription.zone + " for user = " + currentUser?.uid)
                }

            }

            override fun onFailed() {
                // Something
            }
        })
    }

    private fun setupZones() {
        Firestore.zonesListener(object: FirestoreListener<MutableList<Zone>> {
            override fun onStart() {
                // Something
            }

            override fun onSuccess(obj: MutableList<Zone>) {
                allZones = obj
                setupGeofences(obj)
            }

            override fun onFailed() {
                // Something
            }
        })
    }

    fun setupGeofences(zones: MutableList<Zone>) {
        for(zone in zones) {
            Log.i(TAG, "duva: geofence zone found = " + zone.name)
            geofenceList.add(Geofence.Builder()
                .setRequestId(zone.id)
                .setCircularRegion(zone.location.latitude, zone.location.longitude, zone.radius.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build())
        }

        Log.i(TAG, "duva: geofence list = " + geofenceList.toString())

        if(checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.i(TAG, "duva: geofence adding or removing geofences")
            geofencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent)?.run {
                addOnSuccessListener {
                    Log.i(TAG, "duva: geofence added")
                }
                addOnFailureListener {
                    Log.d(TAG, "duva: failed to add geofence")
                }
            }

            // TODO: fix so geofences aren't removed directly at app start
            /*
            geofencingClient.removeGeofences(geofencePendingIntent)?.run {
                addOnSuccessListener {
                    Log.i(TAG, "duva: geofence removed")
                }
                addOnFailureListener {
                    Log.d(TAG, "duva: failed to remove geofence")
                }
            }
            */

        }
    }

    private fun getGeofencingRequest(): GeofencingRequest {
        Log.i(TAG, "duva: geofence getGeofencingRequest()")
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    // TODO: this might trigger uncorrectly since it receives zone[0] id. Fix, sometime.
    private fun geofenceTransition(transition: String, zoneid: String) {
        Log.i(TAG, "duva: geofence received geofenceTransition($transition, $zoneid)")

        if(auth.currentUser == null) {
            return
        }

        var subscriptionid: String = getSubscriptionidFromZoneid(zoneid)
        if(subscriptionid != "null") {
            when(transition) {
                "enter" -> {
                    Log.i(TAG, "duva: geofence geofenceTransition() enter in MainActivity: $zoneid")
                    Firestore.updateField("subscriptions", subscriptionid, "active", true, object: FirestoreCallback {
                        override fun onSuccess() {}
                        override fun onFailed() {}
                    })

                    if(auth.currentUser != null && currentUser != null) {
                        Firestore.updateField("users", currentUser!!.id, "lastZone", zoneid, object: FirestoreCallback {
                            override fun onSuccess() {}
                            override fun onFailed() {}
                        })
                    }
                }
                "exit" -> {
                    Log.i(TAG, "duva: geofence geofenceTransition() exit in MainActivity: $zoneid")
                    Firestore.updateField("subscriptions", subscriptionid, "active", false, object: FirestoreCallback {
                        override fun onSuccess() {}
                        override fun onFailed() {}
                    })
                }
            }
        }
    }

    private fun getSubscriptionidFromZoneid(zoneid: String): String {
        var subscriptionid: String = "null"
        if(auth.currentUser == null) {
            return "null"
        }

        for(subscription in currentSubscriptions!!) {
            if(zoneid == subscription.zone) {
                Log.i(TAG, "duva: found match in getSubscriptionFromZoneid: $zoneid == ${subscription.zone}")
                subscriptionid = subscription.id
            }
        }

        if(subscriptionid == "null") {
            Log.d(TAG, "duva: no match found in getSubscriptionFromZoneid: $zoneid")
        }

        return subscriptionid
    }

    private fun setupPermission(permissionString: String) {
        val permission = ContextCompat.checkSelfPermission(this, permissionString)

        if(permission != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, permissionString)) {
                val builder = AlertDialog.Builder(this)
                when(permissionString) {
                    Manifest.permission.ACCESS_FINE_LOCATION -> builder.setMessage(R.string.permission_ACCESS_FINE_LOCATION).setTitle(R.string.permission_title)
                    Manifest.permission.WRITE_EXTERNAL_STORAGE -> builder.setMessage(R.string.permission_WRITE_EXTERNAL_STORAGE).setTitle(R.string.permission_title)
                    Manifest.permission.RECORD_AUDIO -> builder.setMessage(R.string.permission_RECORD_AUDIO).setTitle(R.string.permission_title)
                }

                builder.setPositiveButton(R.string.permission_button_ok) { dialog, id ->
                    Log.i(TAG, "Permission ok button clicked")
                    makeRequest(permissionString)
                }

                val dialog = builder.create()
                dialog.show()
            } else {
                makeRequest(permissionString)
            }
        }
    }

    private fun makeRequest(permissionString: String) {
        var requestCode = 100

        when(permissionString) {
            Manifest.permission.ACCESS_FINE_LOCATION -> requestCode = requestCodeAccessFineLocation
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> requestCode = requestCodeWriteExternalStorage
            Manifest.permission.RECORD_AUDIO -> requestCode = requestCodeRecordAudio
        }

        ActivityCompat.requestPermissions(this, arrayOf(permissionString), requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            requestCodeAccessFineLocation -> {
                if(grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission: " + permissions[0] + "has been granted")
                } else {
                    Log.d(TAG, "Permission: " + permissions[0] + "has been denied")
                }
            }
            requestCodeWriteExternalStorage -> {
                if(grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // Keep for future use
                } else {
                    // Keep for future use
                }
            }
            requestCodeRecordAudio -> {
                if(grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // Keep for future use
                } else {
                    // Keep for future use
                }
            }
        }
    }

    private fun checkPermission(permissionString: String): Boolean = ContextCompat.checkSelfPermission(this, permissionString) == PackageManager.PERMISSION_GRANTED
}