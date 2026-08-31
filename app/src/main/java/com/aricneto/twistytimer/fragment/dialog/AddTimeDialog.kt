package com.aricneto.twistytimer.fragment.dialog

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
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogAddTimeBinding
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.items.Solve
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
import kotlinx.coroutines.launch
import org.joda.time.DateTime

/**
 * Shows the algList dialog
 */
class AddTimeDialog : DialogFragment() {
    private var binding: DialogAddTimeBinding? = null

    private var currentPuzzle: String? = null
    private var currentScramble: String? = null
    private var currentPuzzleSubtype: String? = null
    private var currentModeInt: Int = 0

    private var mCurrentPenalty = PuzzleUtils.NO_PENALTY
    private var mCurrentComment = ""

    private var mContext: Context? = null

    private val clickListener: View.OnClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.button_save -> if (binding!!.editTextTime.text.toString().isNotEmpty()) {
                val time = parseAddedTime(binding!!.editTextTime.text.toString()).toInt()
                val solve = Solve(
                    if (mCurrentPenalty == PuzzleUtils.PENALTY_PLUSTWO) time + 2000 else time,
                    currentPuzzle!!,
                    currentPuzzleSubtype!!,
                    DateTime().millis,
                    if (binding!!.checkScramble.isChecked) currentScramble ?: "" else "",
                    mCurrentPenalty,
                    mCurrentComment,
                    false,
                    currentModeInt
                )

                lifecycleScope.launch {
                    TwistyTimer.getSolveRepository().insertSolve(
                        type = solve.puzzle,
                        subtype = solve.subtype,
                        time = solve.time.toLong(),
                        date = solve.date,
                        scramble = solve.scramble,
                        penalty = solve.penalty.toLong(),
                        comment = solve.comment,
                        history = solve.history,
                        mode = solve.mode
                    )

                    // The receiver might be able to use the new solve and avoid
                    // accessing the database.
                    BroadcastBuilder(CATEGORY_UI_INTERACTIONS, ACTION_TIME_ADDED_MANUALLY)
                        .solve(solve)
                        .broadcast()

                    // Generate new scramble
                    broadcast(CATEGORY_UI_INTERACTIONS, ACTION_GENERATE_SCRAMBLE)

                    dismiss()
                }
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

                try {
                    val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                    fieldPopup.isAccessible = true
                    val menuPopupHelper = fieldPopup.get(popupMenu)
                    val setForceIcons = menuPopupHelper.javaClass
                        .getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceIcons.invoke(menuPopupHelper, true)
                } catch (e: Exception) {
                    Log.e("AddTimeDialog", "Error forcing icons in PopupMenu: $e")
                }

                popupMenu.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        currentPuzzle = requireArguments().getString("puzzle")
        currentPuzzleSubtype = requireArguments().getString("category")
        currentScramble = requireArguments().getString("scramble")
        currentModeInt = requireArguments().getInt("mode", 0)
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
                    .showSoftInput(binding!!.editTextTime, 0)
            }, 400)
        } catch (e: Exception) {
            Log.e("AddTimeDialog", "Error showing keyboard: $e")
        }

        return binding!!.getRoot()
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(
            currentPuzzle: String?,
            currentPuzzleSubtype: String?,
            currentScramble: String?,
            mode: Int = 0
        ): AddTimeDialog {
            val timeDialog = AddTimeDialog()
            val args = Bundle()
            args.putString("puzzle", currentPuzzle)
            args.putString("category", currentPuzzleSubtype)
            args.putString("scramble", currentScramble)
            args.putInt("mode", mode)
            timeDialog.setArguments(args)
            return timeDialog
        }
    }
}
