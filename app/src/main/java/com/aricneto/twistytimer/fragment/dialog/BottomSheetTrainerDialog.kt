package com.aricneto.twistytimer.fragment.dialog

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogBottomsheetRecyclerBinding
import com.aricneto.twistytimer.activity.MainActivity
import com.aricneto.twistytimer.adapter.TrainerListAdapter
import com.aricneto.twistytimer.puzzle.TrainerScrambler.TrainerSubset
import com.aricneto.twistytimer.utils.TTIntent.ACTION_ALGS_MODIFIED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_CATEGORY
import com.aricneto.twistytimer.utils.TTIntent.ACTION_GENERATE_SCRAMBLE
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_ALG_DATA_CHANGES
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS
import com.aricneto.twistytimer.utils.TTIntent.TTFragmentBroadcastReceiver
import com.aricneto.twistytimer.utils.TTIntent.broadcast
import com.aricneto.twistytimer.utils.TTIntent.registerReceiver
import com.aricneto.twistytimer.utils.TTIntent.unregisterReceiver
import com.aricneto.twistytimer.utils.ThemeUtils
import com.aricneto.twistytimer.viewmodel.AlgViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch


class BottomSheetTrainerDialog : BottomSheetDialogFragment() {
    private var binding: DialogBottomsheetRecyclerBinding? = null

    var currentSubset: TrainerSubset? = null
    var currentCategory: String = ""
    var trainerListAdapter: TrainerListAdapter? = null
    private val viewModel: AlgViewModel by viewModels()

    // Receives broadcasts about changes to the algorithm data.
    private val mAlgDataChangedReceiver
            : TTFragmentBroadcastReceiver =
        object : TTFragmentBroadcastReceiver(this, CATEGORY_ALG_DATA_CHANGES) {
            override fun onReceiveWhileAdded(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_ALGS_MODIFIED -> reloadList()
                }
            }
        }

    // Receives broadcasts about changes to the time user interface.
    private val mUIInteractionReceiver
            : TTFragmentBroadcastReceiver =
        object : TTFragmentBroadcastReceiver(this, CATEGORY_UI_INTERACTIONS) {
            override fun onReceiveWhileAdded(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_CHANGED_CATEGORY -> reloadList()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            currentSubset = requireArguments().getSerializable(KEY_SUBSET) as TrainerSubset?
            currentCategory = requireArguments().getString(KEY_CATEGORY)!!
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogBottomsheetRecyclerBinding.inflate(inflater, container, false)

        binding!!.title.setText(R.string.trainer_spinner_title)
        val icon = ThemeUtils.tintDrawable(
            requireContext(), R.drawable.ic_outline_control_camera_24px,
        com.google.android.material.R.attr.colorOnSurface
//            ContextCompat.getColor(requireContext(), R.color.md_blue_A700)
        )
        binding!!.title.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)

        binding!!.button.visibility = View.VISIBLE
        binding!!.button.setText(R.string.trainer_select_all)
        binding!!.button.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
        binding!!.button.setOnClickListener {
            trainerListAdapter!!.selectAll()
            binding!!.list.setAdapter(trainerListAdapter)
            dismiss()
        }

        setupRecyclerView()
        
        viewModel.setSubset(currentSubset?.name)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.algorithms.collect { algs ->
                    trainerListAdapter?.submitList(algs)
                }
            }
        }

        registerReceiver(mAlgDataChangedReceiver)
        registerReceiver(mUIInteractionReceiver)

        return binding!!.getRoot()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        unregisterReceiver(mAlgDataChangedReceiver)
        unregisterReceiver(mUIInteractionReceiver)
    }

    fun reloadList() {
        viewModel.setSubset(currentSubset?.name)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_GENERATE_SCRAMBLE)
    }

    private fun setupRecyclerView() {
        val parentActivity: Activity? = activity

        trainerListAdapter =
            TrainerListAdapter(requireActivity(), getParentFragmentManager(), currentSubset!!, currentCategory)

        // Set different managers to support different orientations
        val gridLayoutManagerHorizontal =
            StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL)
        val gridLayoutManagerVertical =
            StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)

        // Adapt to orientation
        if (parentActivity!!.resources
                .configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        ) binding!!.list.setLayoutManager(gridLayoutManagerVertical)
        else binding!!.list.setLayoutManager(gridLayoutManagerHorizontal)

        binding!!.list.setAdapter(trainerListAdapter)
    }

    companion object {
        private const val KEY_SUBSET = "subset"
        private const val KEY_CATEGORY = "category"

        fun newInstance(subset: TrainerSubset?, category: String?): BottomSheetTrainerDialog {
            val fragment = BottomSheetTrainerDialog()
            val args = Bundle()
            args.putSerializable(KEY_SUBSET, subset)
            args.putString(KEY_CATEGORY, category)
            fragment.setArguments(args)
            return fragment
        }
    }
}
