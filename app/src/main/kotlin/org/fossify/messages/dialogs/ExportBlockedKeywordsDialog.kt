package org.gault.messages.dialogs

import androidx.appcompat.app.AlertDialog
import org.gault.commons.activities.BaseSimpleActivity
import org.gault.commons.dialogs.FilePickerDialog
import org.gault.commons.extensions.beGone
import org.gault.commons.extensions.getAlertDialogBuilder
import org.gault.commons.extensions.getCurrentFormattedDateTime
import org.gault.commons.extensions.getParentPath
import org.gault.commons.extensions.humanizePath
import org.gault.commons.extensions.internalStoragePath
import org.gault.commons.extensions.isAValidFilename
import org.gault.commons.extensions.setupDialogStuff
import org.gault.commons.extensions.showKeyboard
import org.gault.commons.extensions.toast
import org.gault.commons.extensions.value
import org.gault.commons.helpers.ensureBackgroundThread
import org.gault.messages.R
import org.gault.messages.databinding.DialogExportBlockedKeywordsBinding
import org.gault.messages.extensions.config
import org.gault.messages.helpers.BLOCKED_KEYWORDS_EXPORT_EXTENSION
import java.io.File

class ExportBlockedKeywordsDialog(
    val activity: BaseSimpleActivity,
    val path: String,
    val hidePath: Boolean,
    callback: (file: File) -> Unit,
) {
    private var realPath = path.ifEmpty { activity.internalStoragePath }
    private val config = activity.config

    init {
        val view =
            DialogExportBlockedKeywordsBinding.inflate(activity.layoutInflater, null, false).apply {
                exportBlockedKeywordsFolder.text = activity.humanizePath(realPath)
                exportBlockedKeywordsFilename.setText("${activity.getString(R.string.blocked_keywords)}_${activity.getCurrentFormattedDateTime()}")

                if (hidePath) {
                    exportBlockedKeywordsFolderLabel.beGone()
                    exportBlockedKeywordsFolder.beGone()
                } else {
                    exportBlockedKeywordsFolder.setOnClickListener {
                        FilePickerDialog(activity, realPath, false, showFAB = true) {
                            exportBlockedKeywordsFolder.text = activity.humanizePath(it)
                            realPath = it
                        }
                    }
                }
            }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.gault.commons.R.string.ok, null)
            .setNegativeButton(org.gault.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(
                    view = view.root,
                    dialog = this,
                    titleId = R.string.export_blocked_keywords
                ) { alertDialog ->
                    alertDialog.showKeyboard(view.exportBlockedKeywordsFilename)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = view.exportBlockedKeywordsFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(org.gault.commons.R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file =
                                    File(realPath, "$filename$BLOCKED_KEYWORDS_EXPORT_EXTENSION")
                                if (!hidePath && file.exists()) {
                                    activity.toast(org.gault.commons.R.string.name_taken)
                                    return@setOnClickListener
                                }

                                ensureBackgroundThread {
                                    config.lastBlockedKeywordExportPath =
                                        file.absolutePath.getParentPath()
                                    callback(file)
                                    alertDialog.dismiss()
                                }
                            }

                            else -> activity.toast(org.gault.commons.R.string.invalid_name)
                        }
                    }
                }
            }
    }
}
