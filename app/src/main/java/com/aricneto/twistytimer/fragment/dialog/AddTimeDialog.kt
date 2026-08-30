package com.aricneto.twistytimer.fragment.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogAddTimeBinding
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.listener.DialogListener
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.PuzzleUtils.parseAddedTime
import com.aricneto.twistytimer.utils.TTIntent.ACTION_GENERATE_SCRAMBLE
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_ADDED_MANUALLY
import com.aricneto.twistytimer.utils.TTIntent.BroadcastBuilder
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS
import com.aricneto.twistytimer.utils.TTIntent.broadcast
import com.aricneto.twistytimer.watcher.SolveTimeNumberTextWatcher
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.joda.time.DateTime

/**
 * Shows the algList dialog
 */
class AddTimeDialog : DialogFragment() {
    private var binding: DialogAddTimeBinding? = null

    private var dialogListener: DialogListener? = null

    private var currentPuzzle: String? = null
    private var currentScramble: String? = null
    private var currentPuzzleSubtype: String? = null

    private var mCurrentPenalty = PuzzleUtils.NO_PENALTY
    private var mCurrentComment = ""

    private var mContext: Context? = null

    @SuppressLint("RestrictedApi")
    private val clickListener: View.OnClickListener = View.OnClickListener { view ->
        when (view.getId()) {
            R.id.button_save -> if (binding!!.editTextTime.getText().toString().isNotEmpty()) {
                val time = parseAddedTime(binding!!.editTextTime.getText().toString()).toInt()
                val solve = Solve(
                    if (mCurrentPenalty == PuzzleUtils.PENALTY_PLUSTWO) time + 2000 else time,
                    currentPuzzle!!,
                    currentPuzzleSubtype!!,
                    DateTime().millis,
                    if (binding!!.checkScramble.isChecked) currentScramble ?: "" else "",
                    mCurrentPenalty,
                    mCurrentComment,
                    false
                )

                TwistyTimer.getDBHandler().addSolve(solve)
                // The receiver might be able to use the new solve and avoid
                // accessing the database.
                BroadcastBuilder(CATEGORY_UI_INTERACTIONS, ACTION_TIME_ADDED_MANUALLY)
                    .solve(solve)
                    .broadcast()

                // Generate new scramble
                broadcast(CATEGORY_UI_INTERACTIONS, ACTION_GENERATE_SCRAMBLE)

                dismiss()
            } else {
                dismiss()
            }

            R.id.button_more -> {
                val popupMenu = PopupMenu(mContext!!, binding!!.buttonMore)
                popupMenu.menuInflater.inflate(R.menu.menu_add_time_options, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                    when (item.itemId) {
                        R.id.penalty -> MaterialAlertDialogBuilder(mContext!!)
                            .setTitle(R.string.select_penalty)
                            .setSingleChoiceItems(
                                R.array.array_penalties,
                                mCurrentPenalty
                            ) { dialog: DialogInterface?, which: Int ->
                                when (which) {
                                    0 -> mCurrentPenalty = PuzzleUtils.NO_PENALTY
                                    1 -> mCurrentPenalty = PuzzleUtils.PENALTY_PLUSTWO
                                    2 -> mCurrentPenalty = PuzzleUtils.PENALTY_DNF
                                }
                                dialog!!.dismiss()
                            }
                            .setNegativeButton(R.string.action_cancel, null)
                            .show()

                        R.id.comment -> {
                            val commentView = layoutInflater
                                .inflate(R.layout.dialog_input, requireView() as ViewGroup, false)
                            val commentEditText =
                                commentView.findViewById<TextInputEditText>(R.id.edit_text)
                            commentEditText.setText(mCurrentComment)

                            MaterialAlertDialogBuilder(mContext!!)
                                .setTitle(R.string.edit_comment)
                                .setView(commentView)
                                .setPositiveButton(
                                    R.string.action_done
                                ) { _: DialogInterface?, _: Int ->
                                    mCurrentComment = commentEditText.getText().toString()
                                }
                                .setNegativeButton(R.string.action_cancel, null)
                                .show()
                        }
                    }
                    true
                }

                val popupHelper = MenuPopupHelper(
                    mContext!!,
                    popupMenu.menu as MenuBuilder,
                    binding!!.buttonMore
                )
                popupHelper.setForceShowIcon(true)
                popupHelper.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        currentPuzzle = requireArguments().getString("puzzle")
        currentPuzzleSubtype = requireArguments().getString("category")
        currentScramble = requireArguments().getString("scramble")
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAddTimeBinding.inflate(inflater, container, false)

        dialog!!.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        binding!!.buttonSave.setOnClickListener(clickListener)
        binding!!.buttonMore.setOnClickListener(clickListener)
        binding!!.editTextTime.addTextChangedListener(SolveTimeNumberTextWatcher())

        // Focus on editText and request keyboard
        binding!!.editTextTime.requestFocus()

        try {
            binding!!.editTextTime.postDelayed({
                (mContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(binding!!.editTextTime, InputMethodManager.SHOW_IMPLICIT)
            }, 400)
        } catch (e: Exception) {
            Log.e("AddTimeDialog", "Error showing keyboard: $e")
        }

        return binding!!.getRoot()
    }

    fun setDialogListener(listener: DialogListener?) {
        dialogListener = listener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        // Hide keyboard
        try {
            (mContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(binding!!.editTextTime.windowToken, 0)
        } catch (e: Exception) {
            Log.e("AddTimeDialog", "Error hiding keyboard: $e")
        }

        if (dialogListener != null) dialogListener!!.onDismissDialog()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(
            currentPuzzle: String?,
            currentPuzzleSubtype: String?,
            currentScramble: String?
        ): AddTimeDialog {
            val timeDialog = AddTimeDialog()
            val args = Bundle()
            args.putString("puzzle", currentPuzzle)
            args.putString("category", currentPuzzleSubtype)
            args.putString("scramble", currentScramble)
            timeDialog.setArguments(args)
            return timeDialog
        }
    }
}
