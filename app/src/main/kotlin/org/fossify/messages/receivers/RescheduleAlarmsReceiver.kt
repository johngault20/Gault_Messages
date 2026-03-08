package org.gault.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.gault.commons.helpers.ensureBackgroundThread
import org.gault.messages.extensions.rescheduleAllScheduledMessages

/**
 * Reschedules alarms after boot/package updates.
 */
class RescheduleAlarmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        ensureBackgroundThread {
            context.rescheduleAllScheduledMessages()
            pendingResult.finish()
        }
    }
}
