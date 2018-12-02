package com.step84.duva

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
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
    private val mMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_zones, container, false)
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
        var fillColor: Int
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(home, 15f))
        var allZones = (activity as MainActivity).allZones
        var currentSubscriptions = (activity as MainActivity).currentSubscriptions

        if(allZones != null && currentSubscriptions != null) {
            for (zone in allZones) {
                for (subscription in currentSubscriptions) {
                    if (subscription.zone.equals(zone.id))
                        zone.subscribed = true
                }
            }
        }

        if(allZones != null) {
            for(zone in allZones) {

                if(zone.subscribed) {
                    fillColor = 0x220000FF
                } else {
                    fillColor = 0x22FF0000
                }
                val circle = mMap.addCircle(
                    CircleOptions()
                        .center(LatLng(zone.location.latitude, zone.location.longitude))
                        .radius(zone.radius)
                        .strokeColor(Color.BLUE)
                        .strokeWidth(5f)
                        .clickable(true)
                        .fillColor(0x220000FF)
                )
            }
        }
    }

    override fun onLocationUpdate(location: GeoPoint) {
        Log.i(TAG, "duva: location changed via interface: lat = " + location.latitude + ", lng = " + location.longitude)
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
}
