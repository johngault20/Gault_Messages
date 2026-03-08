package org.gault.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.gault.commons.extensions.notificationManager
import org.gault.commons.helpers.ensureBackgroundThread
import org.gault.messages.extensions.deleteMessage
import org.gault.messages.extensions.updateLastConversationMessage
import org.gault.messages.helpers.IS_MMS
import org.gault.messages.helpers.MESSAGE_ID
import org.gault.messages.helpers.THREAD_ID
import org.gault.messages.helpers.refreshConversations
import org.gault.messages.helpers.refreshMessages

class DeleteSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(THREAD_ID, 0L)
        val messageId = intent.getLongExtra(MESSAGE_ID, 0L)
        val isMms = intent.getBooleanExtra(IS_MMS, false)
        context.notificationManager.cancel(threadId.hashCode())
        ensureBackgroundThread {
            context.deleteMessage(messageId, isMms)
            context.updateLastConversationMessage(threadId)
            refreshMessages()
            refreshConversations()
        }
    }
}
