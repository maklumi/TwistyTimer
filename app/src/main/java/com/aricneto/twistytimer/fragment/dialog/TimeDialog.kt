package com.aricneto.twistytimer.fragment.dialog

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.toDrawable
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogTimeDetailsBinding
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.listener.DialogListener
import com.aricneto.twistytimer.utils.AnimUtils.toggleContentVisibility
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString
import com.aricneto.twistytimer.utils.ScrambleGenerator
import com.aricneto.twistytimer.utils.TTIntent
import com.aricneto.twistytimer.utils.TTIntent.broadcast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shows the timeList dialog
 */
class TimeDialog : DialogFragment() {
    private var binding: DialogTimeDetailsBinding? = null

    private var mId: Long = 0
    private var solve: Solve? = null
    private var dialogListener: DialogListener? = null

    private val clickListener = View.OnClickListener { view ->
        val solveRepository = TwistyTimer.getSolveRepository()

        when (view.id) {
            R.id.overflowButton -> {
                val popupMenu = PopupMenu(requireActivity(), binding!!.overflowButton)
                if (solve?.history ?: false) popupMenu.menuInflater
                    .inflate(R.menu.menu_list_detail_history, popupMenu.menu)
                else popupMenu.menuInflater
                    .inflate(R.menu.menu_list_detail, popupMenu.menu)

                try {
                    val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                    fieldPopup.isAccessible = true
                    val menuPopupHelper = fieldPopup.get(popupMenu)
                    val setForceIcons = menuPopupHelper.javaClass
                        .getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceIcons.invoke(menuPopupHelper, true)
                } catch (e: Exception) {
                    Log.e("TimeDialog", "Error forcing icons in PopupMenu: $e")
                }

                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.share -> {
                            val shareIntent = Intent()
                            shareIntent.action = Intent.ACTION_SEND
                            shareIntent.putExtra(
                                Intent.EXTRA_TEXT,
                                convertTimeToString(
                                    solve!!.time.toLong(),
                                    PuzzleUtils.FORMAT_DEFAULT
                                ) + "s.\n" + solve!!.comment + "\n" + solve!!.scramble
                            )
                            shareIntent.type = "text/plain"
                            requireContext().startActivity(shareIntent)
                        }

                        R.id.remove -> {
                            lifecycleScope.launch {
                                solveRepository.deleteSolve(mId)
                                updateList()
                            }
                        }

                        R.id.history_to -> {
                            solve!!.history = true
                            Toast.makeText(
                                context,
                                getString(R.string.sent_to_history),
                                Toast.LENGTH_SHORT
                            ).show()
                            lifecycleScope.launch {
                                solveRepository.updateSolve(
                                    id = solve!!.id,
                                    time = solve!!.time.toLong(),
                                    date = solve!!.date,
                                    scramble = solve!!.scramble,
                                    penalty = solve!!.penalty.toLong(),
                                    comment = solve!!.comment,
                                    history = solve!!.history,
                                    mode = solve!!.mode
                                )
                                updateList()
                            }
                        }

                        R.id.history_from -> {
                            solve!!.history = false
                            Toast.makeText(
                                context,
                                getString(R.string.sent_to_session),
                                Toast.LENGTH_SHORT
                            ).show()
                            lifecycleScope.launch {
                                solveRepository.updateSolve(
                                    id = solve!!.id,
                                    time = solve!!.time.toLong(),
                                    date = solve!!.date,
                                    scramble = solve!!.scramble,
                                    penalty = solve!!.penalty.toLong(),
                                    comment = solve!!.comment,
                                    history = solve!!.history,
                                    mode = solve!!.mode
                                )
                                updateList()
                            }
                        }
                    }
                    true
                }
                popupMenu.show()
            }

            R.id.editButton ->
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.select_penalty)
                    .setSingleChoiceItems(
                        R.array.array_penalties,
                        solve!!.penalty
                    ) { dialog: DialogInterface?, which: Int ->
                        when (which) {
                            0 -> solve =
                                PuzzleUtils.applyPenalty(solve!!, PuzzleUtils.NO_PENALTY)

                            1 -> solve =
                                PuzzleUtils.applyPenalty(solve!!, PuzzleUtils.PENALTY_PLUSTWO)

                            2 -> solve =
                                PuzzleUtils.applyPenalty(solve!!, PuzzleUtils.PENALTY_DNF)
                        }
                        lifecycleScope.launch {
                            solveRepository.updateSolve(
                                id = solve!!.id,
                                time = solve!!.time.toLong(),
                                date = solve!!.date,
                                scramble = solve!!.scramble,
                                penalty = solve!!.penalty.toLong(),
                                comment = solve!!.comment,
                                history = solve!!.history,
                                mode = solve!!.mode
                            )
                            updateList()
                            dialog!!.dismiss()
                        }
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()

            R.id.commentButton -> {
                val commentView =
                    layoutInflater.inflate(
                        R.layout.dialog_input,
                        null
                    )
                val commentEditText = commentView.findViewById<TextInputEditText>(R.id.edit_text)
                commentEditText.setText(solve!!.comment)

                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.edit_comment)
                    .setView(commentView)
                    .setPositiveButton(
                        R.string.action_done
                    ) { _: DialogInterface?, _: Int ->
                        solve!!.comment = commentEditText.text.toString()
                        lifecycleScope.launch {
                            solveRepository.updateSolve(
                                id = solve!!.id,
                                time = solve!!.time.toLong(),
                                date = solve!!.date,
                                scramble = solve!!.scramble,
                                penalty = solve!!.penalty.toLong(),
                                comment = solve!!.comment,
                                history = solve!!.history,
                                mode = solve!!.mode
                            )
                            Toast.makeText(
                                context,
                                getString(R.string.added_comment),
                                Toast.LENGTH_SHORT
                            ).show()
                            updateList()
                        }
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }

            R.id.scrambleText -> toggleContentVisibility(binding!!.scrambleImage)
        }
    }
    private var mContext: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogTimeDetailsBinding.inflate(inflater, container, false)
        //this.setEnterTransition(R.anim.activity_slide_in);
        mContext = context

        mId = requireArguments().getLong("id")

        dialog!!.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        lifecycleScope.launch {
            val matchedSolve = TwistyTimer.getSolveRepository().getSolve(mId)

            if (matchedSolve != null) {
                solve = matchedSolve

                binding!!.timeText.text = Html.fromHtml(
                    convertTimeToString(
                        solve!!.time.toLong(),
                        PuzzleUtils.FORMAT_SMALL_MILLI
                    ), FROM_HTML_MODE_LEGACY
                )
                val dateTime = Instant.fromEpochMilliseconds(solve!!.date).toLocalDateTime(TimeZone.currentSystemDefault())
                val formatter = DateTimeFormatter.ofPattern("d MMM y'\n'H':'mm", Locale.getDefault())
                binding!!.dateText.text = dateTime.toJavaLocalDateTime().format(formatter)

                binding!!.scrambleText.text = solve!!.scramble

                when (solve!!.penalty) {
                    PuzzleUtils.PENALTY_DNF -> binding!!.puzzlePenaltyText.text =
                        getString(R.string.do_not_finished)

                    PuzzleUtils.PENALTY_PLUSTWO -> binding!!.puzzlePenaltyText.text =
                        "+2"

                    else -> binding!!.puzzlePenaltyText.visibility = View.GONE
                }

                if (solve!!.comment != "") {
                    binding!!.commentText.text = solve!!.comment
                    binding!!.commentText.visibility = View.VISIBLE
                }

                if (solve!!.scramble == "") binding!!.scrambleText.visibility = View.GONE

                binding!!.scrambleText.setOnClickListener(clickListener)
                binding!!.overflowButton.setOnClickListener(clickListener)
                binding!!.editButton.setOnClickListener(clickListener)
                binding!!.commentButton.setOnClickListener(clickListener)

                // Generate scramble image
                val generator = ScrambleGenerator(solve!!.puzzle)
                val drawable = withContext(Dispatchers.IO) {
                    generator.generateImageFromScramble(
                        PreferenceManager.getDefaultSharedPreferences(requireContext()),
                        solve!!.scramble
                    )
                }
                binding!!.scrambleImage.setImageDrawable(drawable)
            }
        }

        return binding!!.getRoot()
    }

    fun setDialogListener(listener: DialogListener?) {
        dialogListener = listener
    }

    private fun updateList() {
        if (dialogListener != null) {
            dialogListener!!.onUpdateDialog()
        } else {
            broadcast(TTIntent.CATEGORY_TIME_DATA_CHANGES, TTIntent.ACTION_TIMES_MODIFIED)
        }
        dismiss()
    }

    override fun onDestroyView() {
        binding = null
        if (dialogListener != null) dialogListener!!.onDismissDialog()
        super.onDestroyView()
    }

    companion object {
        fun newInstance(id: Long): TimeDialog {
            val timeDialog = TimeDialog()
            val args = Bundle()
            args.putLong("id", id)
            timeDialog.setArguments(args)
            return timeDialog
        }
    }
}
