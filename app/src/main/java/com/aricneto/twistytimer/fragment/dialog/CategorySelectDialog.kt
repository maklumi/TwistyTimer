package com.aricneto.twistytimer.fragment.dialog

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogCategorySelectBinding
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.adapter.BottomSheetSpinnerAdapter
import com.aricneto.twistytimer.fragment.TimerFragment
import com.aricneto.twistytimer.listener.DialogListenerMessage
import com.aricneto.twistytimer.puzzle.TrainerScrambler
import com.aricneto.twistytimer.puzzle.TrainerScrambler.TrainerSubset
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CategorySelectDialog : DialogFragment() {
    private var binding: DialogCategorySelectBinding? = null

    // Puzzle and subtypes that are currently selected
    private var currentPuzzle: String? = null
    private var currentSubtype: String? = null
    private var currentTimerMode: String? = null
    private var currentSubset: TrainerSubset? = null
    private var subtypeList: MutableList<String>? = null

    // Subtype that's currently being edited
    private var currentEditSubtype: String? = ""

    private var dialogListenerMessage: DialogListenerMessage? = null

    private var mAdapter: BottomSheetSpinnerAdapter? = null
    private var mContext: Context? = null

    private var currentModeInt: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogCategorySelectBinding.inflate(inflater, container, false)
        mContext = context

        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog!!.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        // retrieve arguments
        currentPuzzle = requireArguments().getString("puzzle")
        currentSubtype = requireArguments().getString("subtype")
        currentTimerMode = requireArguments().getString("mode")
        currentSubset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getSerializable("subset", TrainerSubset::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getSerializable("subset") as TrainerSubset?
        }
        currentModeInt = TimerFragment.modeToInt(currentTimerMode)

        val solveRepository = TwistyTimer.getSolveRepository()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor = sharedPreferences.edit()

        lifecycleScope.launch {
            // get a list of all subtypes
            subtypeList = solveRepository.getAllSubtypesFromType(currentPuzzle!!, currentModeInt)
                .toMutableList()

            if (subtypeList!!.isEmpty()) {
                // if subtype list is empty, create a new entry
                solveRepository.insertSolve(
                    type = currentPuzzle!!,
                    subtype = "Normal",
                    time = 1L,
                    date = 0L,
                    scramble = "",
                    penalty = PuzzleUtils.PENALTY_HIDETIME.toLong(),
                    comment = "",
                    history = true,
                    mode = currentModeInt
                )
                subtypeList =
                    solveRepository.getAllSubtypesFromType(currentPuzzle!!, currentModeInt)
                        .toMutableList()
            }
            if (subtypeList!!.size == 1) {
                currentSubtype = subtypeList!![0]
            }

            updateList()
        }

        // Create subtype
        val createSubtypeView = layoutInflater.inflate(R.layout.dialog_input, null)
        val createSubtypeEditText =
            createSubtypeView.findViewById<TextInputEditText>(R.id.edit_text)
        createSubtypeEditText.setHint(R.string.enter_type_name)

        val createSubtypeDialog = MaterialAlertDialogBuilder(mContext!!)
            .setTitle(R.string.enter_type_name)
            .setView(createSubtypeView)
            .setPositiveButton(
                R.string.action_done
            ) { _: DialogInterface?, _: Int ->
                val input = createSubtypeEditText.text.toString()
                if (input.length in 2..32) {
                    lifecycleScope.launch {
                        // add a single hidden solve with that category name to save it
                        solveRepository.insertSolve(
                            type = currentPuzzle!!,
                            subtype = input,
                            time = 1L,
                            date = 0L,
                            scramble = "",
                            penalty = PuzzleUtils.PENALTY_HIDETIME.toLong(),
                            comment = "",
                            history = true,
                            mode = currentModeInt
                        )
                        currentSubtype = input
                        updateList()

                        editor.putString(KEY_SAVED_SUBTYPE + currentPuzzle, currentSubtype)
                        editor.apply()
                        if (dialogListenerMessage != null) dialogListenerMessage!!.onUpdateDialog(
                            currentSubtype
                        )
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        // select a subtype
        binding!!.list.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                // A subtype was selected
                currentSubtype = subtypeList!![position]

                editor.putString(KEY_SAVED_SUBTYPE + currentPuzzle, currentSubtype)
                editor.apply()
                dialogListenerMessage!!.onUpdateDialog(currentSubtype)
                dismiss()
            }

        binding!!.list.onItemLongClickListener =
            OnItemLongClickListener { _, view, position, _ ->
                // Create a popup menu anchored to the long‑clicked view
                val popup = PopupMenu(requireContext(), view)

                // Inflate your menu resource
                popup.menuInflater.inflate(R.menu.menu_category_options, popup.menu)

                // Handle menu item clicks
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.rename -> {
                            currentEditSubtype = subtypeList!![position]
                            //Rename subtype
                            val renameView =
                                layoutInflater.inflate(R.layout.dialog_input, null)
                            val renameEditText =
                                renameView.findViewById<TextInputEditText>(R.id.edit_text)
                            renameEditText.setText(currentEditSubtype)

                            MaterialAlertDialogBuilder(mContext!!)
                                .setTitle(R.string.enter_new_name_dialog)
                                .setView(renameView)
                                .setPositiveButton(
                                    R.string.action_done
                                ) { _: DialogInterface?, _: Int ->
                                    val input = renameEditText.text.toString()
                                    if (input.length in 2..32) {
                                        lifecycleScope.launch {
                                            solveRepository.renameSubtype(
                                                input,
                                                currentPuzzle!!,
                                                currentEditSubtype!!,
                                                currentModeInt
                                            )
                                            currentSubtype = input
                                            updateList()

                                            editor.putString(
                                                KEY_SAVED_SUBTYPE + currentPuzzle,
                                                currentSubtype
                                            )
                                            if (currentTimerMode == TimerFragment.TIMER_MODE_TRAINER) TrainerScrambler.renameCategory(
                                                currentSubset!!,
                                                currentEditSubtype!!,
                                                currentSubtype!!
                                            )
                                            editor.apply()
                                            dialogListenerMessage!!.onUpdateDialog(currentSubtype)
                                        }
                                    }
                                }
                                .setNegativeButton(R.string.action_cancel, null)
                                .show()
                            true
                        }

                        R.id.remove -> {
                            currentEditSubtype = subtypeList!![position]
                            // Remove Subtype dialog
                            MaterialAlertDialogBuilder(mContext!!)
                                .setTitle(R.string.remove_subtype_confirmation)
                                .setMessage(
                                    getString(R.string.remove_subtype_confirmation_content) + " \"" + currentEditSubtype + "\"?\n" + getString(
                                        R.string.remove_subtype_confirmation_content_continuation
                                    )
                                )
                                .setPositiveButton(
                                    R.string.action_remove
                                ) { _: DialogInterface?, _: Int ->
                                    lifecycleScope.launch {
                                        solveRepository.deleteSubtype(
                                            currentPuzzle!!,
                                            currentEditSubtype!!,
                                            currentModeInt
                                        )
                                        // After removing, change current subtype to first of list, if none exist, create "Normal" subtype
                                        val currentSubtypes =
                                            solveRepository.getAllSubtypesFromType(
                                                currentPuzzle!!,
                                                currentModeInt
                                            )
                                        currentSubtype = if (currentSubtypes.isNotEmpty()) {
                                            currentSubtypes[0]
                                        } else {
                                            "Normal"
                                        }
                                        updateList()

                                        editor.putString(
                                            KEY_SAVED_SUBTYPE + currentPuzzle,
                                            currentSubtype
                                        )
                                        editor.apply()
                                        dialogListenerMessage!!.onUpdateDialog(currentSubtype)
                                    }
                                }
                                .setNegativeButton(R.string.action_cancel, null)
                                .show()
                            true
                        }

                        else -> false
                    }
                }

                // Optional: force icons to show (PopupMenu doesn’t expose this directly)
                try {
                    val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                    fieldPopup.isAccessible = true
                    val menuPopupHelper = fieldPopup.get(popup)
                    val setForceIcons = menuPopupHelper.javaClass
                        .getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceIcons.invoke(menuPopupHelper, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                popup.show()
                true
            }

        binding!!.addCategory.setOnClickListener { _: View? -> createSubtypeDialog.show() }
    }

    private fun updateList() {
        lifecycleScope.launch {
            subtypeList = TwistyTimer.getSolveRepository()
                .getAllSubtypesFromType(currentPuzzle!!, currentModeInt).toMutableList()
            val icons = intArrayOf()
            mAdapter =
                BottomSheetSpinnerAdapter(
                    requireContext(),
                    subtypeList!!.toTypedArray<String?>(),
                    icons
                )
            binding!!.list.adapter = mAdapter
        }
    }

    fun setDialogListener(dialogListener: DialogListenerMessage?) {
        this.dialogListenerMessage = dialogListener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val KEY_SAVED_SUBTYPE = "savedSubtype"

        fun newInstance(
            currentPuzzle: String?,
            currentPuzzleSubtype: String?,
            currentTimerMode: String?,
            currentSubset: TrainerSubset?
        ): CategorySelectDialog {
            val dialog = CategorySelectDialog()
            val args = Bundle()
            args.putString("puzzle", currentPuzzle)
            args.putString("subtype", currentPuzzleSubtype)
            args.putString("mode", currentTimerMode)
            args.putSerializable("subset", currentSubset)
            dialog.setArguments(args)
            return dialog
        }
    }
}
