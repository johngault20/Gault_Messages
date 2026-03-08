package org.gault.messages.dialogs

import org.gault.commons.activities.BaseSimpleActivity
import org.gault.commons.extensions.getAlertDialogBuilder
import org.gault.commons.extensions.setupDialogStuff
import org.gault.messages.databinding.DialogSelectTextBinding

// helper dialog for selecting just a part of a message, not copying the whole into clipboard
class SelectTextDialog(val activity: BaseSimpleActivity, val text: String) {
    init {
        val binding = DialogSelectTextBinding.inflate(activity.layoutInflater).apply {
            dialogSelectTextValue.text = text
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.gault.commons.R.string.ok) { _, _ -> { } }
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }
}
