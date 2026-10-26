package com.safeguard.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.safeguard.app.util.Prefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (Prefs.isPaired && Prefs.serviceEnabled) {
                SafeguardService.start(context)
            }
        }
    }
}
