package com.step84.duva

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp

data class Zone(
    var added: Timestamp,
    var name: String,
    var geopoint: LatLng,
    var radius: Double
)