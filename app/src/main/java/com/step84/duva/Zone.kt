package com.step84.duva

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Zone(
    var id: String = "0",
    var added: Timestamp = Timestamp.now(),
    var name: String = "0",
    var location: GeoPoint = GeoPoint(0.0, 0.0),
    var subscribed: Boolean = false,
    var radius: Double = 0.0
)