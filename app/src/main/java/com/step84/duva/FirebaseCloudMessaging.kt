package com.step84.duva

import android.media.MediaPlayer
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.io.IOException
import java.lang.IllegalStateException

class FirebaseCloudMessaging : FirebaseMessagingService() {
    private val TAG = "FirebaseCloudMessaging"

    private val mediaPlayer: MediaPlayer = MediaPlayer()

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(TAG, "duva: FCM received message")

        message.data?.isNotEmpty().let {
            Log.i(TAG, "duva: FCM message data payload: ${message.data}")

            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(message.data["file"])
                mediaPlayer.prepare()
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener {
                    it.stop()
                    it.reset()
                }
            } catch (ise: IllegalStateException) {
                Log.d(TAG, "duva: audio playback failed", ise)
                return
            } catch (ioe: IOException) {
                Log.d(TAG, "duva: audio playback failed", ioe)
                return
            }
        }

        message.notification?.let { notification ->
            Log.i(TAG, "duva: FCM message notification payload: ${notification.body}")
        }
    }
}