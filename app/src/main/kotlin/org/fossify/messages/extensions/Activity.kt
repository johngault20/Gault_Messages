package org.gault.messages.extensions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import org.gault.commons.activities.BaseSimpleActivity
import org.gault.commons.extensions.getMimeType
import org.gault.commons.extensions.hideKeyboard
import org.gault.commons.extensions.isPackageInstalled
import org.gault.commons.extensions.launchActivityIntent
import org.gault.commons.extensions.launchViewContactIntent
import org.gault.commons.extensions.showErrorToast
import org.gault.commons.extensions.toast
import org.gault.commons.helpers.CONTACT_ID
import org.gault.commons.helpers.IS_PRIVATE
import org.gault.commons.helpers.PERMISSION_CALL_PHONE
import org.gault.commons.helpers.SimpleContactsHelper
import org.gault.commons.helpers.ensureBackgroundThread
import org.gault.commons.models.SimpleContact
import org.gault.messages.activities.ConversationDetailsActivity
import org.gault.messages.helpers.THREAD_ID
import java.util.Locale

fun BaseSimpleActivity.dialNumber(phoneNumber: String, callback: (() -> Unit)? = null) {
    hideKeyboard()
    handlePermission(PERMISSION_CALL_PHONE) {
        val action = if (it) Intent.ACTION_CALL else Intent.ACTION_DIAL
        Intent(action).apply {
            data = Uri.fromParts("tel", phoneNumber, null)

            try {
                startActivity(this)
                callback?.invoke()
            } catch (_: ActivityNotFoundException) {
                toast(org.gault.commons.R.string.no_app_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }
}

fun Activity.launchViewIntent(uri: Uri, mimetype: String, filename: String) {
    Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(uri, mimetype.lowercase(Locale.getDefault()))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            hideKeyboard()
            startActivity(this)
        } catch (_: ActivityNotFoundException) {
            val newMimetype = filename.getMimeType()
            if (newMimetype.isNotEmpty() && mimetype != newMimetype) {
                launchViewIntent(uri, newMimetype, filename)
            } else {
                toast(org.gault.commons.R.string.no_app_found)
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}

fun Activity.startContactDetailsIntent(contact: SimpleContact) {
    val simpleContacts = "org.gault.contacts"
    val simpleContactsDebug = "org.gault.contacts.debug"
    if (
        contact.rawId > 1000000 &&
        contact.contactId > 1000000 &&
        contact.rawId == contact.contactId &&
        (isPackageInstalled(simpleContacts) || isPackageInstalled(simpleContactsDebug))
    ) {
        Intent().apply {
            action = Intent.ACTION_VIEW
            putExtra(CONTACT_ID, contact.rawId)
            putExtra(IS_PRIVATE, true)
            setPackage(
                if (isPackageInstalled(simpleContacts)) {
                    simpleContacts
                } else {
                    simpleContactsDebug
                }
            )

            setDataAndType(
                ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                "vnd.android.cursor.dir/person"
            )

            launchActivityIntent(this)
        }
    } else {
        ensureBackgroundThread {
            val lookupKey = SimpleContactsHelper(this)
                .getContactLookupKey(
                    contactId = (contact).rawId.toString()
                )

            val publicUri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey
            )

            runOnUiThread {
                launchViewContactIntent(publicUri)
            }
        }
    }
}

fun Activity.launchConversationDetails(threadId: Long) {
    Intent(this, ConversationDetailsActivity::class.java).apply {
        putExtra(THREAD_ID, threadId)
        startActivity(this)
    }
}
