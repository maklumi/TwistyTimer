package com.aricneto.twistytimer.fragment.dialog

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.DialogFragment
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogCategorySelectBinding
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.adapter.BottomSheetSpinnerAdapter
import com.aricneto.twistytimer.database.DatabaseHandler
import com.aricneto.twistytimer.fragment.TimerFragment
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.listener.DialogListenerMessage
import com.aricneto.twistytimer.puzzle.TrainerScrambler
import com.aricneto.twistytimer.puzzle.TrainerScrambler.TrainerSubset
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import androidx.core.graphics.drawable.toDrawable

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
    private val mItemClickListener: OnItemClickListener? = null
    private val mItemLongClickListener: OnItemLongClickListener? = null
    private val mOnClickListener: View.OnClickListener? = null
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
    ): View? {
        binding = DialogCategorySelectBinding.inflate(inflater, container, false)
        mContext = context

        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog!!.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        // retrieve arguments
        currentPuzzle = requireArguments().getString("puzzle")
        currentSubtype = requireArguments().getString("subtype")
        currentTimerMode = requireArguments().getString("mode")
        currentSubset = requireArguments().getSerializable("subset") as TrainerSubset?
        currentModeInt = DatabaseHandler.modeToInt(currentTimerMode)

        val dbHandler = TwistyTimer.getDBHandler()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()

        // get a list of all subtypes
        subtypeList = dbHandler.getAllSubtypesFromType(currentPuzzle!!, currentModeInt)

        if (subtypeList!!.isEmpty()) {
            // if subtype list is empty, create a new entry
            dbHandler.addSolve(
                Solve(
                    1,
                    currentPuzzle!!,
                    "Normal",
                    0L,
                    "",
                    PuzzleUtils.PENALTY_HIDETIME,
                    "",
                    true,
                    currentModeInt
                )
            )
        } else if (subtypeList!!.size == 1) {
            currentSubtype = subtypeList!![0]
        }

        updateList(dbHandler)

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
                val input = createSubtypeEditText.getText().toString()
                if (input.length in 2..32) {
                    // add a single hidden solve with that category name to save it
                    dbHandler.addSolve(
                        Solve(
                            1,
                            currentPuzzle!!,
                            input,
                            0L,
                            "",
                            PuzzleUtils.PENALTY_HIDETIME,
                            "",
                            true,
                            currentModeInt
                        )
                    )
                    currentSubtype = input
                    updateList(dbHandler)

                    editor.putString(KEY_SAVED_SUBTYPE + currentPuzzle, currentSubtype)
                    editor.apply()
                    if (dialogListenerMessage != null) dialogListenerMessage!!.onUpdateDialog(
                        currentSubtype
                    )
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        val context = getContext()

        // click listeners
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
                            currentEditSubtype = subtypeList!!.get(position)
                            //Rename subtype
                            val renameView =
                                LayoutInflater.from(mContext).inflate(R.layout.dialog_input, null)
                            val renameEditText =
                                renameView.findViewById<TextInputEditText>(R.id.edit_text)
                            renameEditText.setText(currentEditSubtype)

                            MaterialAlertDialogBuilder(mContext!!)
                                .setTitle(R.string.enter_new_name_dialog)
                                .setView(renameView)
                                .setPositiveButton(
                                    R.string.action_done,
                                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                                        val input = renameEditText.getText().toString()
                                        if (input.length >= 2 && input.length <= 32) {
                                            dbHandler.renameSubtype(
                                                currentPuzzle!!,
                                                currentEditSubtype!!,
                                                input,
                                                currentModeInt
                                            )
                                            currentSubtype = input
                                            updateList(dbHandler)

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
                                    })
                                .setNegativeButton(R.string.action_cancel, null)
                                .show()
                            true
                        }
                        R.id.remove -> {
                            currentEditSubtype = subtypeList!!.get(position)
                            // Remove Subtype dialog
                            MaterialAlertDialogBuilder(mContext!!)
                                .setTitle(R.string.remove_subtype_confirmation)
                                .setMessage(
                                    getString(R.string.remove_subtype_confirmation_content) + " \"" + currentEditSubtype + "\"?\n" + getString(
                                        R.string.remove_subtype_confirmation_content_continuation
                                    )
                                )
                                .setPositiveButton(
                                    R.string.action_remove,
                                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                                        dbHandler.deleteSubtype(currentPuzzle!!, currentEditSubtype!!, currentModeInt)
                                        // After removing, change current subtype to first of list, if none exist, create "Normal" subtype
                                        if (subtypeList!!.size > 1) {
                                            currentSubtype =
                                                dbHandler.getAllSubtypesFromType(currentPuzzle!!, currentModeInt)
                                                    .get(0)
                                        } else {
                                            currentSubtype = "Normal"
                                        }
                                        updateList(dbHandler)

                                        editor.putString(
                                            KEY_SAVED_SUBTYPE + currentPuzzle,
                                            currentSubtype
                                        )
                                        editor.apply()
                                        dialogListenerMessage!!.onUpdateDialog(currentSubtype)
                                    })
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

        /*
        binding!!.list.onItemLongClickListener =
            OnItemLongClickListener { _: AdapterView<*>?, view12: View?, position: Int, _: Long ->
                // Create a popup menu for each entry
                val menuBuilder = MenuBuilder(context)
                val inflater = MenuInflater(context)

                inflater.inflate(R.menu.menu_category_options, menuBuilder)
                val popupHelper = MenuPopupHelper(context!!, menuBuilder, view12!!)
                popupHelper.setForceShowIcon(true)

                menuBuilder.setCallback(object : MenuBuilder.Callback {
                    override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                        when (item.getItemId()) {
                            R.id.rename -> {
                                currentEditSubtype = subtypeList!!.get(position)
                                //Rename subtype
                                val renameView =
                                    LayoutInflater.from(mContext).inflate(R.layout.dialog_input, null)
                                val renameEditText =
                                    renameView.findViewById<TextInputEditText>(R.id.edit_text)
                                renameEditText.setText(currentEditSubtype)

                                MaterialAlertDialogBuilder(mContext!!)
                                    .setTitle(R.string.enter_new_name_dialog)
                                    .setView(renameView)
                                    .setPositiveButton(
                                        R.string.action_done,
                                        DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                                            val input = renameEditText.getText().toString()
                                            if (input.length >= 2 && input.length <= 32) {
                                                dbHandler.renameSubtype(
                                                    currentPuzzle!!,
                                                    currentEditSubtype!!,
                                                    input,
                                                    currentModeInt
                                                )
                                                currentSubtype = input
                                                updateList(dbHandler)

                                                editor.putString(
                                                    KEY_SAVEDSUBTYPE + currentPuzzle,
                                                    currentSubtype
                                                )
                                                if (currentTimerMode == TimerFragment.TIMER_MODE_TRAINER) TrainerScrambler.renameCategory(
                                                    currentSubset!!,
                                                    currentEditSubtype,
                                                    currentSubtype
                                                )
                                                editor.apply()
                                                dialogListenerMessage!!.onUpdateDialog(currentSubtype)
                                            }
                                        })
                                    .setNegativeButton(R.string.action_cancel, null)
                                    .show()
                            }

                            R.id.remove -> {
                                currentEditSubtype = subtypeList!!.get(position)
                                // Remove Subtype dialog
                                MaterialAlertDialogBuilder(mContext!!)
                                    .setTitle(R.string.remove_subtype_confirmation)
                                    .setMessage(
                                        getString(R.string.remove_subtype_confirmation_content) + " \"" + currentEditSubtype + "\"?\n" + getString(
                                            R.string.remove_subtype_confirmation_content_continuation
                                        )
                                    )
                                    .setPositiveButton(
                                        R.string.action_remove,
                                        DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                                            dbHandler.deleteSubtype(currentPuzzle!!, currentEditSubtype!!)
                                            // After removing, change current subtype to first of list, if none exist, create "Normal" subtype
                                            if (subtypeList!!.size > 1) {
                                                currentSubtype =
                                                    dbHandler.getAllSubtypesFromType(currentPuzzle!!)
                                                        .get(0)
                                            } else {
                                                currentSubtype = "Normal"
                                            }
                                            updateList(dbHandler)

                                            editor.putString(
                                                KEY_SAVEDSUBTYPE + currentPuzzle,
                                                currentSubtype
                                            )
                                            editor.apply()
                                            dialogListenerMessage!!.onUpdateDialog(currentSubtype)
                                        })
                                    .setNegativeButton(R.string.action_cancel, null)
                                    .show()
                            }
                        }
                        return false
                    }

                    override fun onMenuModeChange(menu: MenuBuilder) {
                    }
                })

                popupHelper.show()
                true
            }*/
        binding!!.addCategory.setOnClickListener { _: View? -> createSubtypeDialog.show() }
    }

    private fun updateList(dbHandler: DatabaseHandler) {
        subtypeList = dbHandler.getAllSubtypesFromType(currentPuzzle!!, currentModeInt)
        val icons = intArrayOf()
        mAdapter =
            BottomSheetSpinnerAdapter(requireContext(), subtypeList!!.toTypedArray<String?>(), icons)
        binding!!.list.adapter = mAdapter
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
