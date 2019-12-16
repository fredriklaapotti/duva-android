package com.step84.duva

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"
    // TODO: might be unsafe, check setAction()
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            Log.i(TAG, "duva: boot in on Receive, context not null, starting foreground service")
            ForegroundService.startService(context, "Service started from boot...")
        }
    }
}