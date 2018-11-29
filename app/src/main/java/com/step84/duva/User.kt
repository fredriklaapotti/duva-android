package com.step84.duva

import com.google.android.gms.maps.model.LatLng

data class User(
    var id: String,
    var lastLocation: LatLng
)