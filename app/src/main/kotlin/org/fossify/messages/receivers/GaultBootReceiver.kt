package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.messages.services.GaultDataRelay
import org.fossify.messages.services.GaultNetworkService

class GaultBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        // Keep the node + relay alive after reboot.
        context.startForegroundService(Intent(context, GaultNetworkService::class.java))
        context.startService(Intent(context, GaultDataRelay::class.java))
    }
}

