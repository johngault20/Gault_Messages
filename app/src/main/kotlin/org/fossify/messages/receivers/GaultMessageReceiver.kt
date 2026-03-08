package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.messages.extensions.messagesDB
import org.fossify.messages.helpers.*
import org.fossify.messages.models.Message
import org.greenrobot.eventbus.EventBus

class GaultMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "RECEIVE_P2P_MSG") {
            val sender = intent.getStringExtra("SENDER_ADDRESS") ?: return
            val body = intent.getStringExtra("MESSAGE_BODY") ?: return

            // The Database Injection logic
            ensureBackgroundThread {
                // 1. Find or create the conversation thread for this sender
                val threadId = context.getThreadId(setOf(sender))
                
                // 2. Create the Gault Message object
                val message = Message(
                    id = 0, 
                    threadId = threadId,
                    body = body,
                    type = TYPE_P2P, // Mark as Gault P2P
                    date = (System.currentTimeMillis() / 1000).toInt(),
                    read = false,
                    isMms = false
                )

                // 3. Push to SQLite Database
                context.messagesDB.insertOrUpdate(message)

                // 4. Update the conversation preview snippet in the main list
                context.updateConversationSnippet(threadId, body)

                // 5. Tell the UI to refresh immediately
                val bus = EventBus.getDefault()
                bus.post(Events.RefreshMessages())
                bus.post(Events.RefreshConversations())
            }
        }
    }
}