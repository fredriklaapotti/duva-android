package com.step84.duva

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

    var currentUser: User? = null
    var currentZone: Zone? = null
    var allZones: MutableList<Zone>? = null
    var currentSubscriptions: MutableList<Subscription>? = null

    private lateinit var auth: FirebaseAuth

    private var googleMapInterface: GoogleMapInterface? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                switchFragment(HomeFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_zones -> {
                switchFragment(ZonesFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {
                switchFragment(SettingsFragment())
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private fun switchFragment(f: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.container, f).commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        switchFragment(HomeFragment())
        setupPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        setupPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setTimestampsInSnapshotsEnabled(true)
            .build()
        firestore.firestoreSettings = settings

        auth = FirebaseAuth.getInstance()

        auth.signInWithEmailAndPassword("fredrik.laapotti@gmail.com", "password")
            .addOnCompleteListener(this) { task ->
                if(task.isSuccessful) {
                    Log.i(TAG, "duva: user logged in")
                } else {
                    Log.d(TAG, "duva: failed to login user", task.exception)
                }
            }

        setupUser()
        setupSubscriptions()
        setupZones()

        setMapListener(ZonesFragment())

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
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
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
                    //var fragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                    googleMapInterface?.onLocationUpdate(GeoPoint(location.latitude, location.longitude))
                    //currentUser?.lastLocation = GeoPoint(location.latitude, location.longitude)
                    //Log.i(TAG, "duva: currentUser?.lastLocation = " + currentUser?.lastLocation.toString())
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.i(TAG, "Location updates stopped")
    }

    fun setMapListener(listener: GoogleMapInterface) {
        this.googleMapInterface = listener
    }

    private fun setupUser() {
        Firestore.userListener(auth.currentUser, object: FirestoreListener<User> {
            override fun onStart() {
                //Something
            }

            override fun onSuccess(obj: User) {
                currentUser = obj
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
                for(zone in obj) {
                    Log.i(TAG, "duva: zone found = " + zone.name)
                }
            }

            override fun onFailed() {
                // Something
            }
        })
    }

    private fun setupPermission(permissionString: String) {
        val permission = ContextCompat.checkSelfPermission(this, permissionString)

        if(permission != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, permissionString)) {
                val builder = AlertDialog.Builder(this)
                when(permissionString) {
                    Manifest.permission.ACCESS_FINE_LOCATION -> builder.setMessage(R.string.permission_ACCESS_FINE_LOCATION).setTitle(R.string.permission_title)
                    Manifest.permission.WRITE_EXTERNAL_STORAGE -> builder.setMessage(R.string.permission_WRITE_EXTERNAL_STORAGE).setTitle(R.string.permission_title)
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
        var requestCode: Int = 100

        when(permissionString) {
            Manifest.permission.ACCESS_FINE_LOCATION -> requestCode = requestCodeAccessFineLocation
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> requestCode = requestCodeWriteExternalStorage
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
        }
    }

    private fun checkPermission(permissionString: String): Boolean = ContextCompat.checkSelfPermission(this, permissionString) == PackageManager.PERMISSION_GRANTED
}