package com.step84.duva

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

interface GoogleMapInterface {
    fun onLocationUpdate(location: GeoPoint)
}

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ZonesFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ZonesFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class ZonesFragment : Fragment(), OnMapReadyCallback, GoogleMapInterface {
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    private val TAG = "ZonesFragment"

    private var mapView: View? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var txt_clickedZone: TextView

    private lateinit var switch_settingNotice: Switch
    private lateinit var switch_settingOverrideSound: Switch

    private lateinit var switch_larmPreset: CheckBox
    private lateinit var switch_larmSoundRecording: CheckBox
    private lateinit var switch_larmVideo: CheckBox
    private lateinit var btn_updateSettings: Button
    private lateinit var btn_subscribeZone: Button
    private lateinit var btn_unsubscribeZone: Button
    //private val mMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_zones, container, false)

        txt_clickedZone = view.findViewById(R.id.txt_clickedZone)
        btn_updateSettings = view.findViewById(R.id.btn_updateSettings)
        btn_subscribeZone = view.findViewById(R.id.btn_subscribeZone)
        btn_unsubscribeZone = view.findViewById(R.id.btn_unsubscribeZone)

        switch_settingNotice = view.findViewById(R.id.switch_settingNotice)
        switch_settingOverrideSound = view.findViewById(R.id.switch_settingOverrideSound)

        switch_larmPreset = view.findViewById(R.id.switch_larmPreset)
        switch_larmSoundRecording = view.findViewById(R.id.switch_larmSoundRecording)
        switch_larmVideo = view.findViewById(R.id.switch_larmVideo)

        toggleUI(false)

        btn_updateSettings.setOnClickListener {
            if(switch_settingNotice.tag != null && switch_settingOverrideSound.tag != null && btn_updateSettings.tag != null) {
                val subscription: Subscription = switch_settingNotice.tag as Subscription
                var fieldvalues: MutableMap<String, Boolean> = mutableMapOf()
                fieldvalues["setting_alarm_notice"] = switch_settingNotice.isChecked
                fieldvalues["setting_alarm_override_sound"]  = switch_settingOverrideSound.isChecked
                Firestore.batchUpdate("subscriptions", subscription.id, fieldvalues, object: FirestoreCallback {
                    override fun onSuccess() {
                        Toast.makeText(context!!, "${getText(R.string.toast_updateZoneSuccess)}", Toast.LENGTH_LONG).show()
                    }
                    override fun onFailed() {
                        Toast.makeText(context!!, "${getText(R.string.toast_updateZoneFailed)}", Toast.LENGTH_LONG).show()
                    }
                })
            }
        }

        btn_subscribeZone.setOnClickListener {
            val newSubscription: Subscription = Subscription("0", true, auth.uid.toString(), Globals.clickedZone, false, false, false, false, false)
            Log.i(TAG, "duva: trying to add subscription")

            // TODO: update to use active flag instead
            Firestore.addSubscription(newSubscription, object: FirestoreCallback {
                override fun onSuccess() {
                    Log.i(TAG, "duva: successfully added document and subscribed to zone")
                    btn_subscribeZone.visibility = View.INVISIBLE
                    btn_unsubscribeZone.visibility = View.VISIBLE
                    btn_updateSettings.visibility = View.VISIBLE
                }

                override fun onFailed() {
                    Log.d(TAG, "duva: failed to add document and subscribe to zone")
                }
            })
        }

        btn_unsubscribeZone.setOnClickListener {
            val currentSubscription: Subscription = btn_updateSettings.tag as Subscription
            Firestore.deleteDocument("subscriptions", currentSubscription.id, object: FirestoreCallback {
                override fun onSuccess() {
                    Log.i(TAG, "duva: successfully removed document and unsubscribed")
                    btn_subscribeZone.visibility = View.VISIBLE
                    btn_unsubscribeZone.visibility = View.INVISIBLE
                    btn_updateSettings.visibility = View.INVISIBLE
                }

                override fun onFailed() {
                    Log.d(TAG, "duva: failed to remove document and unsubscribe")
                }
            })

            // TODO: unsubscribing should only toggle active flag, find nicer way instead of deleting
            // TODO: That means above method is wrong, convert subscription method first
            /*
            val fieldvalues: MutableMap<String, Boolean> = mutableMapOf()
            fieldvalues["active"] = false
            Firestore.batchUpdate("subscriptions", currentSubscription.id, fieldvalues, object: FirestoreCallback {
                override fun onSuccess() {
                    Log.i(TAG, "duva: successfully unsubscribed (toggled active to false) from zone")
                }

                override fun onFailed() {
                    Log.d(TAG, "duva: failed to unsubscribe (toggle active to false) from zone")
                }
            })
            */
        }

        return view
    }

    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity != null) {
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

            mapFragment.getMapAsync(this)
            mapView = mapFragment.view
        }
    }

    override fun onMapReady(mMap: GoogleMap) {
        val home = LatLng(57.670897, 15.860455)
        var currentLocation = (activity as MainActivity).currentLocation
        val markers: MutableList<Marker> = mutableListOf()
        if(checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isZoomControlsEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentLocation!!.latitude, currentLocation.longitude), 15f))
            mMap.setOnCircleClickListener { circle ->
                onCircleClick(circle, mMap)
            }
            mMap.setOnMapClickListener { latlng ->
                onMapClick(latlng, mMap)
            }
        }
        val allZones = (activity as MainActivity).allZones

        if(allZones != null) {
            for(zone in allZones) {
                val circle = mMap.addCircle(
                    CircleOptions()
                        .center(LatLng(zone.location.latitude, zone.location.longitude))
                        .radius(zone.radius)
                        .strokeColor(Color.BLUE)
                        .strokeWidth(5f)
                        .clickable(true)
                        .fillColor(0x220000FF)
                        .clickable(true)
                )
                circle.tag = zone
            }
        }
    }

    override fun onLocationUpdate(location: GeoPoint) {
        Log.i(TAG, "duva: location changed via interface: lat = " + location.latitude + ", lng = " + location.longitude)
    }

    private fun onCircleClick(circle: Circle, mMap: GoogleMap) {
        val zone: Zone = circle.tag as Zone

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(zone.location.latitude, zone.location.longitude), 15f))
        Globals.clickedZone = zone.id
        txt_clickedZone.text = Globals.getZoneNameFromZoneId(zone.id)

        if(auth.currentUser == null) {
            Toast.makeText(context!!, "${getText(R.string.toast_notLoggedIn)}", Toast.LENGTH_SHORT).show()
            //txt_clickedZone.text = Globals.getZoneNameFromZoneId(zone.id)

        } else {
            if(Globals.currentSubscriptions!!.isEmpty()) {
                Log.i(TAG, "duva: no subscriptions at all")
                btn_subscribeZone.visibility = View.VISIBLE
                btn_unsubscribeZone.visibility = View.INVISIBLE
                switch_settingNotice.tag = null
                switch_settingOverrideSound.tag = null
                btn_updateSettings.tag = null
                btn_updateSettings.visibility = View.INVISIBLE

                switch_settingNotice.isChecked = false
                switch_settingOverrideSound.isChecked = false

                switch_larmPreset.isChecked = false
                switch_larmSoundRecording.isChecked = false
                switch_larmVideo.isChecked = false
            }
            loop@ for(subscription in Globals.currentSubscriptions!!) {
                if(subscription.zone == zone.id) {
                    toggleUI(true)
                    btn_subscribeZone.visibility = View.INVISIBLE
                    btn_unsubscribeZone.visibility = View.VISIBLE

                    switch_settingNotice.tag = subscription
                    switch_settingOverrideSound.tag = subscription
                    btn_updateSettings.tag = subscription

                    //txt_clickedZone.text = Globals.getZoneNameFromZoneId(zone.id)

                    switch_settingNotice.isChecked = subscription.setting_alarm_notice
                    switch_settingOverrideSound.isChecked = subscription.setting_alarm_override_sound

                    switch_larmPreset.isChecked = subscription.permission_larm_preset
                    switch_larmSoundRecording.isChecked = subscription.permission_larm_soundrecording
                    switch_larmVideo.isChecked = subscription.permission_larm_video
                    break@loop
                } else { // No subscription found, will loop subscriptions but end up here if none found
                    btn_subscribeZone.visibility = View.VISIBLE
                    btn_unsubscribeZone.visibility = View.INVISIBLE
                    switch_settingNotice.tag = null
                    switch_settingOverrideSound.tag = null
                    btn_updateSettings.tag = null
                    btn_updateSettings.visibility = View.INVISIBLE

                    switch_settingNotice.isChecked = false
                    switch_settingOverrideSound.isChecked = false

                    switch_larmPreset.isChecked = false
                    switch_larmSoundRecording.isChecked = false
                    switch_larmVideo.isChecked = false
                }
            }
        }
    }

    private fun onMapClick(location: LatLng, mMap: GoogleMap) {
        toggleUI(false)
    }

    private fun toggleUI(enabled: Boolean) {
        when(enabled) {
            true -> {
                btn_updateSettings.visibility = View.VISIBLE
                btn_subscribeZone.visibility = View.INVISIBLE
                btn_unsubscribeZone.visibility = View.INVISIBLE
                switch_settingNotice.isClickable = true
                switch_settingOverrideSound.isClickable = true
            }
            false -> {
                switch_settingNotice.isClickable = false
                switch_settingOverrideSound.isClickable = false
                switch_larmPreset.isClickable = false
                switch_larmSoundRecording.isClickable = false
                switch_larmVideo.isClickable = false

                btn_updateSettings.visibility = View.INVISIBLE
                btn_subscribeZone.visibility = View.INVISIBLE
                btn_unsubscribeZone.visibility = View.INVISIBLE

                txt_clickedZone.text = getText(R.string.txt_clickedZone)

                switch_settingNotice.isChecked = false
                switch_settingOverrideSound.isChecked = false

                switch_larmPreset.isChecked = false
                switch_larmSoundRecording.isChecked = false
                switch_larmVideo.isChecked = false
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ZonesFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ZonesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun checkPermission(permissionString: String): Boolean = ContextCompat.checkSelfPermission(context!!, permissionString) == PackageManager.PERMISSION_GRANTED
}
