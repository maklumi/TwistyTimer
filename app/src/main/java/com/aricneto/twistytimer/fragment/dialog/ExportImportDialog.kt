package com.aricneto.twistytimer.fragment.dialog

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogExportImportBinding
import com.aricneto.twistytimer.fragment.dialog.PuzzleChooserDialog.PuzzleCallback
import com.aricneto.twistytimer.utils.AnimUtils.toggleContentVisibility
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.graphics.drawable.toDrawable

/**
 * 
 * 
 * A dialog fragment that initiates the import and export of solve times. This dialog presents the
 * user with options to export solve times or to import solve times, and for each of those options
 * to use the simple text ("external") format or the back-up format. If the user chooses to import
 * times, a file must be chosen. If the user chooses to use the "external" format, this dialog hands
 * over to [PuzzleChooserDialog] to allow the user to select the puzzle type and category.
 * The puzzle chooser fragments report their choices back to this fragment via the common parent
 * activity. Once this dialog has assembled all of the necessary details for the operation, it calls
 * back to the activity to initiate the import or export operation.
 * 
 * 
 * 
 * *This dialog fragment **must** be used in the context of an activity that implements the
 * [ExportImportCallbacks] and extends the `AppCompatActivity` class, or exceptions will
 * occur.*
 * 
 */
class ExportImportDialog : DialogFragment(), PuzzleCallback {
    private var mContext: Context? = null
    private var binding: DialogExportImportBinding? = null

    /**
     * A call-back interface that supports interaction between the import/export dialogs, file chooser
     * dialogs, and the main activity that will perform the actual import/export operations.
     */
    interface ExportImportCallbacks {
        /**
         * Instructs the listener to begin importing solve times from a file.
         * 
         * @param fileFormat
         * The solve file format. Must be either [.EXIM_FORMAT_EXTERNAL] or
         * [.EXIM_FORMAT_BACKUP].
         * @param puzzleType
         * The type of the puzzle whose times will be imported. This is required when
         * `fileFormat` is `EXIM_FORMAT_EXTERNAL`. It may be `null` if the
         * format is `EXIM_FORMAT_BACKUP`, as it will not be used.
         * @param puzzleCategory
         * The category (subtype) of the puzzle whose times will be imported. This is required
         * when `fileFormat` is `EXIM_FORMAT_EXTERNAL`. It may be `null` if
         * the format is `EXIM_FORMAT_BACKUP`, as it will not be used.
         * @param mode
         * The timer mode (TIMER or TRAINER).
         */
        fun onImportSolveTimes(fileFormat: Int, puzzleType: String?, puzzleCategory: String?, mode: Int)

        /**
         * Instructs the listener to begin exporting solve times to a file. The export file name and
         * directory will be chosen automatically.
         * 
         * @param fileFormat
         * The solve file format. Must be either [.EXIM_FORMAT_EXTERNAL] or
         * [.EXIM_FORMAT_BACKUP].
         * @param puzzleType
         * The type of the puzzle whose times will be exported. This is required when
         * `fileFormat` is `EXIM_FORMAT_EXTERNAL`. It may be `null` if the
         * format is `EXIM_FORMAT_BACKUP`, as it will not be used.
         * @param puzzleCategory
         * The category (subtype) of the puzzle whose times will be exported. This is required
         * when `fileFormat` is `EXIM_FORMAT_EXTERNAL`. It may be `null` if
         * the format is `EXIM_FORMAT_BACKUP`, as it will not be used.
         * @param mode
         * The timer mode (TIMER or TRAINER).
         */
        fun onExportSolveTimes(fileFormat: Int, puzzleType: String?, puzzleCategory: String?, mode: Int)
    }

    private var mIsExport = false
    private var mMode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        mMode = arguments?.getInt(ARG_MODE, 0) ?: 0
    }

    private val clickListener: View.OnClickListener = View.OnClickListener { view ->
        // NOTE: The call-backs from the "PuzzleChooserDialog"
        //
        when (view.id) {
            R.id.export_backup -> {
                // All puzzle types and categories are exported to a single back-up file.
                // There is no need to identify the export file or the puzzle type/category.
                // Just invoke the activity and dismiss this dialog.
                (getExImActivity() as ExportImportCallbacks).onExportSolveTimes(EXIM_FORMAT_BACKUP, null, null, 0)
                dismiss()
            }

            R.id.export_external -> {
                mIsExport = true
                // Select the single puzzle type and category that will be exported. When the
                // call-back from this puzzle chooser is received ("onPuzzleTypeSelected"),
                // this dialog will exit and hand control back to the activity to perform the
                // export. The call-back uses "getTag()" to tell the chooser to tell the
                // activity that the chosen puzzle type/category should be relayed back to this
                // dialog fragment.
                PuzzleChooserDialog.newInstance(
                    R.string.action_export, this@ExportImportDialog.getTag(), mMode
                )
                    .show(requireActivity().getSupportFragmentManager(), null)
            }

            R.id.import_backup -> {
                (getExImActivity() as ExportImportCallbacks).onImportSolveTimes(EXIM_FORMAT_BACKUP, null, null, 0)
                dismiss()
            }

            R.id.import_external -> {
                mIsExport = false
                // Need to get the puzzle type and category before importing the data. There will
                // be a call-back to "onPuzzleSelected" before returning to the activity.
                PuzzleChooserDialog.newInstance(
                    R.string.action_import, this@ExportImportDialog.getTag(), mMode
                )
                    .show(requireActivity().getSupportFragmentManager(), null)
            }

            R.id.export_button -> toggleContentVisibility(
                binding!!.exportBackup,
                binding!!.exportExternal
            )

            R.id.import_button -> toggleContentVisibility(
                binding!!.importBackup,
                binding!!.importExternal
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogExportImportBinding.inflate(inflater, container, false)
        mContext = context

        binding!!.exportBackup.setOnClickListener(clickListener)
        binding!!.exportExternal.setOnClickListener(clickListener)
        binding!!.importBackup.setOnClickListener(clickListener)
        binding!!.importExternal.setOnClickListener(clickListener)
        //helpButton.setOnClickListener(clickListener);
        binding!!.importButton.setOnClickListener(clickListener)
        binding!!.exportButton.setOnClickListener(clickListener)

        dialog!!.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        return binding!!.getRoot()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    /**
     * Gets the activity reference type cast to support the required interfaces and base classes
     * for export/import operations.
     * 
     * @return The attached activity, or `null` if no activity is attached.
     */
     private fun <A>
            getExImActivity(): A where A : AppCompatActivity, A : ExportImportCallbacks {
        return activity as A
    }

    override fun onPuzzleSelected(
        tag: String, puzzleType: String, puzzleCategory: String
    ) {
        // Importing or exporting to an "external" format file. The file will already have been
        // chosen if this is an import operation. Now that the puzzle type and category are known,
        // hand control back to the activity.
        if (mIsExport) {
            (getExImActivity() as ExportImportCallbacks).onExportSolveTimes(EXIM_FORMAT_EXTERNAL, puzzleType, puzzleCategory, mMode)
        } else {
            // Show a dialog that explains the required text format, then, when that is
            // closed, select the file to import. When the call-back from this file chooser
            // is received ("onFileSelection"), this dialog will check that the file name
            // is valid. If valid, the puzzle chooser will be shown.  When the call-back
            // from that puzzle chooser is received ("onPuzzleTypeSelected"), this dialog
            // will exit and hand control back to the activity to perform the import.
            val activityMain: ExportImportCallbacks = getExImActivity()
            MaterialAlertDialogBuilder(mContext!!)
                .setTitle(R.string.import_external_title)
                .setMessage(R.string.import_external_content_first)
                .setPositiveButton(
                    R.string.action_ok
                ) { _: DialogInterface?, _: Int ->
                    activityMain.onImportSolveTimes(
                        EXIM_FORMAT_EXTERNAL, puzzleType,
                        puzzleCategory, mMode
                    )
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
        dismiss()
    }

    companion object {
        /**
         * The file format for simple text import/export of solve times for a single puzzle type and
         * category that can be interchanged with external applications.
         */
        const val EXIM_FORMAT_EXTERNAL: Int = 1

        /**
         * The file format for full text import/export of all solve times used to back-up the database.
         */
        const val EXIM_FORMAT_BACKUP: Int = 2

        private const val ARG_MODE = "mode"

        fun newInstance(mode: Int = 0): ExportImportDialog {
            val fragment = ExportImportDialog()
            val args = Bundle()
            args.putInt(ARG_MODE, mode)
            fragment.setArguments(args)
            return fragment
        }
    }
}
