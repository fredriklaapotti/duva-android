package com.step84.duva

data class Subscription(
    var id: String = "0",
    var active: Boolean = false,
    var user: String = "0",
    var zone: String = "0",
    var setting_alarm_notice: Boolean = false,
    var setting_alarm_override_sound: Boolean = false,
    var permission_larm_soundrecording: Boolean = false,
    var permission_larm_video: Boolean = false,
    var permission_larm_preset: Boolean = false
)