package org.gault.messages.dialogs

import androidx.appcompat.app.AlertDialog
import org.gault.commons.activities.BaseSimpleActivity
import org.gault.commons.extensions.getAlertDialogBuilder
import org.gault.commons.extensions.setupDialogStuff
import org.gault.commons.extensions.showKeyboard
import org.gault.commons.extensions.value
import org.gault.messages.databinding.DialogAddBlockedKeywordBinding
import org.gault.messages.extensions.config

class AddBlockedKeywordDialog(val activity: BaseSimpleActivity, private val originalKeyword: String? = null, val callback: () -> Unit) {
    init {
        val binding = DialogAddBlockedKeywordBinding.inflate(activity.layoutInflater).apply {
            if (originalKeyword != null) {
                addBlockedKeywordEdittext.setText(originalKeyword)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.gault.commons.R.string.ok, null)
            .setNegativeButton(org.gault.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.showKeyboard(binding.addBlockedKeywordEdittext)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newBlockedKeyword = binding.addBlockedKeywordEdittext.value
                        if (originalKeyword != null && newBlockedKeyword != originalKeyword) {
                            activity.config.removeBlockedKeyword(originalKeyword)
                        }

                        if (newBlockedKeyword.isNotEmpty()) {
                            activity.config.addBlockedKeyword(newBlockedKeyword)
                        }

                        callback()
                        alertDialog.dismiss()
                    }
                }
            }
    }
}
