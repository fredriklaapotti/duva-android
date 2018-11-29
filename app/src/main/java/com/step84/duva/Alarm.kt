package com.step84.duva

import com.google.firebase.Timestamp

data class Alarm(
    var activated: Timestamp,
    var source: String,
    var zone: String,
    var file: String,
    var type: String
)