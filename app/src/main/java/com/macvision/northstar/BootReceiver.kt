package com.macvision.northstar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arm the guardian after the phone reboots. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) SosService.start(context)
    }
}
