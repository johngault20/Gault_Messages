package org.gault.messages.dialogs

import androidx.appcompat.app.AlertDialog
import org.gault.commons.extensions.getAlertDialogBuilder
import org.gault.commons.extensions.getProperPrimaryColor
import org.gault.commons.extensions.setupDialogStuff
import org.gault.commons.extensions.toast
import org.gault.commons.helpers.MEDIUM_ALPHA
import org.gault.commons.helpers.ensureBackgroundThread
import org.gault.messages.R
import org.gault.messages.activities.SimpleActivity
import org.gault.messages.databinding.DialogImportMessagesBinding
import org.gault.messages.extensions.config
import org.gault.messages.helpers.MessagesImporter
import org.gault.messages.models.ImportResult
import org.gault.messages.models.MessagesBackup

class ImportMessagesDialog(
    private val activity: SimpleActivity,
    private val messages: List<MessagesBackup>,
) {

    private val config = activity.config

    init {
        var ignoreClicks = false
        val binding = DialogImportMessagesBinding.inflate(activity.layoutInflater).apply {
            importSmsCheckbox.isChecked = config.importSms
            importMmsCheckbox.isChecked = config.importMms
        }

        binding.importProgress.setIndicatorColor(activity.getProperPrimaryColor())

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.gault.commons.R.string.ok, null)
            .setNegativeButton(org.gault.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(
                    view = binding.root,
                    dialog = this,
                    titleId = R.string.import_messages
                ) { alertDialog ->
                    val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    positiveButton.setOnClickListener {
                        if (ignoreClicks) {
                            return@setOnClickListener
                        }

                        if (!binding.importSmsCheckbox.isChecked && !binding.importMmsCheckbox.isChecked) {
                            activity.toast(R.string.no_option_selected)
                            return@setOnClickListener
                        }

                        ignoreClicks = true
                        activity.toast(org.gault.commons.R.string.importing)
                        config.importSms = binding.importSmsCheckbox.isChecked
                        config.importMms = binding.importMmsCheckbox.isChecked

                        alertDialog.setCanceledOnTouchOutside(false)
                        binding.importProgress.show()
                        arrayOf(
                            binding.importMmsCheckbox,
                            binding.importSmsCheckbox,
                            positiveButton,
                            negativeButton
                        ).forEach {
                            it.isEnabled = false
                            it.alpha = MEDIUM_ALPHA
                        }

                        ensureBackgroundThread {
                            MessagesImporter(activity).restoreMessages(messages) {
                                handleParseResult(it)
                                alertDialog.dismiss()
                            }
                        }
                    }
                }
            }
    }

    private fun handleParseResult(result: ImportResult) {
        activity.toast(
            when (result) {
                ImportResult.IMPORT_OK -> org.gault.commons.R.string.importing_successful
                ImportResult.IMPORT_PARTIAL -> org.gault.commons.R.string.importing_some_entries_failed
                ImportResult.IMPORT_FAIL -> org.gault.commons.R.string.importing_failed
                else -> org.gault.commons.R.string.no_items_found
            }
        )
    }
}
