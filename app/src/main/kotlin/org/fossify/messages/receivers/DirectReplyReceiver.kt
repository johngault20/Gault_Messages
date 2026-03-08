package org.gault.messages.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.RemoteInput
import org.gault.commons.extensions.showErrorToast
import org.gault.commons.helpers.SimpleContactsHelper
import org.gault.commons.helpers.ensureBackgroundThread
import org.gault.messages.extensions.*
import org.gault.messages.helpers.REPLY
import org.gault.messages.helpers.THREAD_ID
import org.gault.messages.helpers.THREAD_NUMBER
import org.gault.messages.messaging.sendMessageCompat

class DirectReplyReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val address = intent.getStringExtra(THREAD_NUMBER)
        val threadId = intent.getLongExtra(THREAD_ID, 0L)
        var body = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(REPLY)?.toString() ?: return

        body = context.removeDiacriticsIfNeeded(body)

        if (address != null) {
            var subscriptionId: Int? = null
            val availableSIMs = context.subscriptionManagerCompat().activeSubscriptionInfoList
            if ((availableSIMs?.size ?: 0) > 1) {
                val currentSIMCardIndex = context.config.getUseSIMIdAtNumber(address)
                val wantedId = availableSIMs?.getOrNull(currentSIMCardIndex)
                if (wantedId != null) {
                    subscriptionId = wantedId.subscriptionId
                }
            }

            ensureBackgroundThread {
                var messageId = 0L
                try {
                    context.sendMessageCompat(body, listOf(address), subscriptionId, emptyList())
                    val message = context.getMessages(
                        threadId = threadId, includeScheduledMessages = false, limit = 1
                    ).lastOrNull()
                    if (message != null) {
                        context.messagesDB.insertOrUpdate(message)
                        messageId = message.id

                        context.updateLastConversationMessage(threadId)
                    }
                } catch (e: Exception) {
                    context.showErrorToast(e)
                }

                val photoUri = SimpleContactsHelper(context).getPhotoUriFromPhoneNumber(address)
                val bitmap = context.getNotificationBitmap(photoUri)
                Handler(Looper.getMainLooper()).post {
                    context.notificationHelper.showMessageNotification(
                        messageId = messageId,
                        address = address,
                        body = body,
                        threadId = threadId,
                        bitmap = bitmap,
                        sender = null,
                        alertOnlyOnce = true
                    )
                }

                context.markThreadMessagesRead(threadId)
                context.conversationsDB.markRead(threadId)
            }
        }
    }
}
