package org.gault.messages.dialogs

import org.gault.commons.activities.BaseSimpleActivity
import org.gault.commons.extensions.getAlertDialogBuilder
import org.gault.commons.extensions.setupDialogStuff
import org.gault.messages.databinding.DialogInvalidNumberBinding

class InvalidNumberDialog(val activity: BaseSimpleActivity, val text: String) {
    init {
        val binding = DialogInvalidNumberBinding.inflate(activity.layoutInflater).apply {
            dialogInvalidNumberDesc.text = text
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.gault.commons.R.string.ok) { _, _ -> }
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }
}
