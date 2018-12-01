package com.step84.duva

data class Subscription(
    var active: Boolean = false,
    var user: String = "0",
    var zone: String = "0",
    var setting_alarm_notice: Boolean = false,
    var setting_alarm_override_sound: Boolean = false
)