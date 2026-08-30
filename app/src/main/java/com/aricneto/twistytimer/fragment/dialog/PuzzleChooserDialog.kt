package com.aricneto.twistytimer.fragment.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogPuzzleChooserDialogBinding
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.PuzzleUtils.getPuzzleInPosition
import androidx.core.graphics.drawable.toDrawable

/**
 * 
 * 
 * A dialog fragment that chooses a the puzzle type and category. Once the desired options are
 * chosen this dialog relays the selection through the parent activity to the "consumer" fragment
 * that initiated this fragment. The "consumer" fragment is identified by its fragment tag, which
 * it passes to this chooser in [.newInstance] and which this chooser passes
 * back to the activity, so the activity can find that "consumer" fragment.
 * 
 * 
 * 
 * *This dialog fragment **must** be used in the context of an activity that implements the
 * [PuzzleCallback] interface, or exceptions will occur.*
 * 
 */
class PuzzleChooserDialog : DialogFragment() {
    /**
     * An interface that allows fragments to communicate changes to the selected puzzle type and/or
     * category.
     */
    interface PuzzleCallback {
        /**
         * Notifies the listener that a new puzzle type and/or category have been selected.
         * 
         * @param tag
         * The tag identifying the callback. When this interface is implemented by an activity
         * as a means of communication between two fragments, the tag should be the fragment
         * tag that identifies the fragment to which the activity should relay the message. The
         * receiving fragment should also (probably) implement this interface.
         * @param puzzleType
         * The name of the newly-selected puzzle type.
         * @param puzzleCategory
         * The name of the newly-selected puzzle category.
         */
        fun onPuzzleSelected(
            tag: String, puzzleType: String, puzzleCategory: String
        )
    }

    private var binding: DialogPuzzleChooserDialogBinding? = null

    /**
     * The selected puzzle type.
     */
    private var mSelectedPuzzleType: String? = null

    /**
     * The selected puzzle category.
     */
    private var mSelectedPuzzleCategory: String? = null

    private var categoryAdapter: ArrayAdapter<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = DialogPuzzleChooserDialogBinding.inflate(inflater, container, false)

        @StringRes val buttonTextResID =
            if (arguments != null) requireArguments().getInt(ARG_BUTTON_TEXT_RES_ID, 0) else 0

        if (buttonTextResID != 0) {
            // Override the default text.
            binding!!.selectButton.setText(buttonTextResID)
        }

        val puzzleAdapter: ArrayAdapter<*> = ArrayAdapter.createFromResource(
            requireContext(), R.array.puzzles, android.R.layout.simple_spinner_dropdown_item
        )

        binding!!.puzzleSpinner.adapter = puzzleAdapter

        // Be flexible with the initial value, as it depends on what is first in "R.array.puzzle".
        mSelectedPuzzleType = binding!!.puzzleSpinner.selectedItem as String
        mSelectedPuzzleCategory = CURRENT_CATEGORY

        updateCategoriesForType(mSelectedPuzzleType)

        binding!!.puzzleSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                mSelectedPuzzleType = getPuzzleInPosition(i)
                updateCategoriesForType(mSelectedPuzzleType)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {
            }
        }

        binding!!.categorySpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                mSelectedPuzzleCategory = categoryAdapter!!.getItem(i)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {
            }
        }

        binding!!.selectButton.setOnClickListener {
            // Relay this information back to the fragment/activity that opened this chooser.
            getRelayActivity<PuzzleCallback?>()!!.onPuzzleSelected(
                requireArguments().getString(ARG_CONSUMER_TAG, "not set!"),
                mSelectedPuzzleType!!, mSelectedPuzzleCategory!!
            )
            dismiss()
        }

        dialog!!.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        return binding!!.getRoot()
    }

    private fun updateCategoriesForType(puzzleType: String?) {
        val dbHandler = TwistyTimer.getDBHandler()
        val subtypeList = dbHandler.getAllSubtypesFromType(puzzleType!!)

        if (subtypeList.isEmpty()) {
            subtypeList.add(CURRENT_CATEGORY)
            dbHandler.addSolve(
                Solve(
                    1, puzzleType, CURRENT_CATEGORY,
                    0L, "", PuzzleUtils.PENALTY_HIDETIME, "", true
                )
            )
        }
        categoryAdapter = ArrayAdapter<String>(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, subtypeList
        )
        binding!!.categorySpinner.adapter = categoryAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    /**
     * Gets the activity reference type cast to support the required interface for relaying the
     * puzzle type/category selection to another fragment.
     * 
     * @return The attached activity, or `null` if no activity is attached.
     */
    private fun <A : PuzzleCallback?> getRelayActivity(): A? {
        return activity as A?
    }

    companion object {
        private const val CURRENT_CATEGORY = "Normal"

        /**
         * The name of the fragment argument holding a string resource ID for the text to be displayed
         * on the selection button that closes this chooser dialog.
         */
        private const val ARG_BUTTON_TEXT_RES_ID = "buttonTextResourceID"

        /**
         * The name of the fragment argument holding the fragment tag of the fragment that instantiated
         * this puzzle chooser.
         */
        private const val ARG_CONSUMER_TAG = "consumerTag"

        /**
         * Creates a new instance of this fragment.
         * 
         * @param buttonTextResID
         * The string resource ID of the string to be displayed on the select button that closes
         * this fragment and reports the selection. If zero, the default "OK" will be shown.
         * @param consumerTag
         * The fragment tag that identifies the fragment that instantiated this puzzle chooser.
         * This "consumer" fragment will be informed, via the parent activity, of the selected
         * puzzle type and category before this chooser is dismissed.
         * 
         * @return
         * The new instance of this puzzle chooser.
         */
        fun newInstance(
            @StringRes buttonTextResID: Int, consumerTag: String?
        ): PuzzleChooserDialog {
            val fragment = PuzzleChooserDialog()
            val args = Bundle()

            args.putInt(ARG_BUTTON_TEXT_RES_ID, buttonTextResID)
            args.putString(ARG_CONSUMER_TAG, consumerTag)
            fragment.setArguments(args)

            return fragment
        }
    }
}
