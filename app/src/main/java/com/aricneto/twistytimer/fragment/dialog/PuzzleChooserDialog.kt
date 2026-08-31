package com.aricneto.twistytimer.fragment.dialog

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogPuzzleChooserDialogBinding
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.PuzzleUtils.getPuzzleInPosition
import kotlinx.coroutines.launch

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

    private var mMode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogPuzzleChooserDialogBinding.inflate(inflater, container, false)

        @StringRes val buttonTextResID =
            if (arguments != null) requireArguments().getInt(ARG_BUTTON_TEXT_RES_ID, 0) else 0
        mMode = if (arguments != null) requireArguments().getInt(ARG_MODE, 0) else 0

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
        val solveRepository = TwistyTimer.getSolveRepository()
        lifecycleScope.launch {
            val subtypeList = solveRepository.getAllSubtypesFromType(puzzleType!!, mMode).toMutableList()

            if (subtypeList.isEmpty()) {
                subtypeList.add(CURRENT_CATEGORY)
                solveRepository.insertSolve(
                    type = puzzleType,
                    subtype = CURRENT_CATEGORY,
                    time = 1,
                    date = 0L,
                    scramble = "",
                    penalty = PuzzleUtils.PENALTY_HIDETIME.toLong(),
                    comment = "",
                    history = true,
                    mode = mMode
                )
            }
            categoryAdapter = ArrayAdapter<String>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, subtypeList
            )
            binding!!.categorySpinner.adapter = categoryAdapter
        }
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
    @Suppress("UNCHECKED_CAST")
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
        private const val ARG_MODE = "mode"

        /**
         * Creates a new instance of this fragment.
         * 
         * @param buttonTextResID
         * @param consumerTag
         * @param mode
         * 
         * @return
         * The new instance of this puzzle chooser.
         */
        fun newInstance(
            @StringRes buttonTextResID: Int, consumerTag: String?, mode: Int = 0
        ): PuzzleChooserDialog {
            val fragment = PuzzleChooserDialog()
            val args = Bundle()

            args.putInt(ARG_BUTTON_TEXT_RES_ID, buttonTextResID)
            args.putString(ARG_CONSUMER_TAG, consumerTag)
            args.putInt(ARG_MODE, mode)
            fragment.setArguments(args)

            return fragment
        }
    }
}
