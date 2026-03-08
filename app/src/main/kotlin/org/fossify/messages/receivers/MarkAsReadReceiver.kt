package org.gault.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.gault.commons.extensions.notificationManager
import org.gault.commons.helpers.ensureBackgroundThread
import org.gault.messages.extensions.conversationsDB
import org.gault.messages.extensions.markThreadMessagesRead
import org.gault.messages.helpers.MARK_AS_READ
import org.gault.messages.helpers.THREAD_ID
import org.gault.messages.helpers.refreshConversations

class MarkAsReadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MARK_AS_READ -> {
                val threadId = intent.getLongExtra(THREAD_ID, 0L)
                context.notificationManager.cancel(threadId.hashCode())
                ensureBackgroundThread {
                    context.markThreadMessagesRead(threadId)
                    context.conversationsDB.markRead(threadId)
                    refreshConversations()
                }
            }
        }
    }
}
