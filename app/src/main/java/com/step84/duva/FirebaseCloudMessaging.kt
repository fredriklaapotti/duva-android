package com.step84.duva

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseCloudMessaging : FirebaseMessagingService() {
    private val TAG = "FirebaseCloudMessaging"

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(TAG, "duva: FCM received message")

        message.data?.isNotEmpty().let {
            Log.i(TAG, "duva: FCM message data payload: ${message.data}")
        }

        message.notification?.let { notification ->
            Log.i(TAG, "duva: FCM message notification payload: ${notification.body}")
        }
    }
}