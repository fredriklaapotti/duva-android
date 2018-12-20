package com.step84.duva

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class User(
    var id: String = "0",
    var uid: String = "0",
    var active: Boolean = false,
    var added: Timestamp = Timestamp.now(),
    var lastUpdate: Timestamp = Timestamp.now(),
    var lastLocation: GeoPoint = GeoPoint(0.0, 0.0),
    var lastZone: String = "0"
)