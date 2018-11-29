package com.step84.duva

data class Subscription(
    var active: Boolean,
    var user: String,
    var zone: String,
    var setting_alarm_notice: Boolean,
    var setting_alarm_override_sound: Boolean
)