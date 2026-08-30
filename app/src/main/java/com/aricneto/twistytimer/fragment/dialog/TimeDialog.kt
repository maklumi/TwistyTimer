package com.aricneto.twistytimer.fragment.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.DialogFragment
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
import org.joda.time.DateTime
import androidx.core.graphics.drawable.toDrawable
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY

/**
 * Shows the timeList dialog
 */
class TimeDialog : DialogFragment() {
    private var binding: DialogTimeDetailsBinding? = null

    private var mId: Long = 0
    private var solve: Solve? = null
    private var dialogListener: DialogListener? = null

    @SuppressLint("RestrictedApi")
    private val clickListener: View.OnClickListener = object : View.OnClickListener {
        override fun onClick(view: View) {
            val dbHandler = TwistyTimer.getDBHandler()

            when (view.id) {
                R.id.overflowButton -> {
                    val popupMenu = PopupMenu(requireActivity(), binding!!.overflowButton)
                    if (solve?.history ?: false) popupMenu.menuInflater
                        .inflate(R.menu.menu_list_detail_history, popupMenu.menu)
                    else popupMenu.menuInflater
                        .inflate(R.menu.menu_list_detail, popupMenu.menu)

                    val popupHelper = MenuPopupHelper(
                        mContext!!,
                        popupMenu.menu as MenuBuilder,
                        binding!!.overflowButton
                    )
                    popupHelper.setForceShowIcon(true)

                    popupMenu.setOnMenuItemClickListener { item ->
                        when (item.getItemId()) {
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
                                dbHandler.deleteSolveByID(mId)
                                updateList()
                            }

                            R.id.history_to -> {
                                solve!!.history = true
                                Toast.makeText(
                                    context,
                                    getString(R.string.sent_to_history),
                                    Toast.LENGTH_SHORT
                                ).show()
                                dbHandler.updateSolve(solve!!)
                                updateList()
                                dismiss()
                            }

                            R.id.history_from -> {
                                solve!!.history = false
                                Toast.makeText(
                                    context,
                                    getString(R.string.sent_to_session),
                                    Toast.LENGTH_SHORT
                                ).show()
                                dbHandler.updateSolve(solve!!)
                                updateList()
                                dismiss()
                            }
                        }
                        true
                    }
                    popupHelper.show()
                }

                R.id.editButton -> MaterialAlertDialogBuilder(mContext!!)
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
                        dbHandler.updateSolve(solve!!)
                        updateList()
                        dialog!!.dismiss()
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()

                R.id.commentButton -> {
                    val commentView =
                        LayoutInflater.from(mContext).inflate(R.layout.dialog_input, null)
                    val commentEditText =
                        commentView.findViewById<TextInputEditText>(R.id.edit_text)
                    commentEditText.setText(solve!!.comment)

                    MaterialAlertDialogBuilder(mContext!!)
                        .setTitle(R.string.edit_comment)
                        .setView(commentView)
                        .setPositiveButton(
                            R.string.action_done
                        ) { _: DialogInterface?, _: Int ->
                            solve!!.comment = commentEditText.getText().toString()
                            dbHandler.updateSolve(solve!!)
                            Toast.makeText(
                                getContext(),
                                getString(R.string.added_comment),
                                Toast.LENGTH_SHORT
                            ).show()
                            updateList()
                        }
                        .setNegativeButton(R.string.action_cancel, null)
                        .show()
                }

                R.id.scrambleText -> toggleContentVisibility(binding!!.scrambleImage)
            }
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

        //Log.d("TIME DIALOG", "mId: " + mId + "\nexists: " + handler.idExists(mId));
        dialog!!.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        //getDialog().getWindow().setWindowAnimations(R.style.DialogAnimationScale);
        val matchedSolve = TwistyTimer.getDBHandler().getSolve(mId)

        if (matchedSolve != null) {
            solve = matchedSolve

            binding!!.timeText.text = Html.fromHtml(
                convertTimeToString(
                    solve!!.time.toLong(),
                    PuzzleUtils.FORMAT_SMALL_MILLI
                ), FROM_HTML_MODE_LEGACY
            )
            binding!!.dateText.text = DateTime(solve!!.date).toString("d MMM y'\n'H':'mm")

            binding!!.scrambleText.text = solve!!.scramble

            when (solve!!.penalty) {
                PuzzleUtils.PENALTY_DNF -> binding!!.puzzlePenaltyText.text = "DNF"
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
        }

        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
       this@TimeDialog.GenerateScrambleImage().execute()
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

    private inner class GenerateScrambleImage : AsyncTask<Void?, Void?, Drawable?>() {
        override fun doInBackground(vararg voids: Void?): Drawable? {
            val generator = ScrambleGenerator(solve!!.puzzle)
            return generator.generateImageFromScramble(
                PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext()),
                solve!!.scramble
            )
        }

        override fun onPostExecute(drawable: Drawable?) {
            super.onPostExecute(drawable)
            if (binding != null && binding!!.scrambleImage != null) binding!!.scrambleImage.setImageDrawable(
                drawable
            )
        }
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
