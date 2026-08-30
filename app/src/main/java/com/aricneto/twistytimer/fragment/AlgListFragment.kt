package com.aricneto.twistytimer.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.FragmentAlgListBinding
import com.aricneto.twistytimer.activity.MainActivity
import com.aricneto.twistytimer.adapter.AlgListAdapter
import com.aricneto.twistytimer.utils.TTIntent.ACTION_ALGS_MODIFIED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_THEME
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_ALG_DATA_CHANGES
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS
import com.aricneto.twistytimer.utils.TTIntent.TTFragmentBroadcastReceiver
import com.aricneto.twistytimer.utils.TTIntent.registerReceiver
import com.aricneto.twistytimer.utils.TTIntent.unregisterReceiver
import com.aricneto.twistytimer.utils.ThemeUtils
import com.aricneto.twistytimer.utils.ThemeUtils.preferredTheme
import com.aricneto.twistytimer.viewmodel.AlgViewModel
import kotlinx.coroutines.launch


class AlgListFragment : BaseFragment() {
    private var binding: FragmentAlgListBinding? = null

    private var currentSubset: String? = null
    private var algListAdapter: AlgListAdapter? = null
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
                    ACTION_CHANGED_THEME -> try {
                        // If the theme has been changed, then the activity will need to be recreated. The
                        // theme can only be applied properly during the inflation of the layouts, so it has
                        // to go back to "Activity.updateLocale()" to do that.
                        (activity as MainActivity).onRecreateRequired()
                    } catch (_: Exception) {
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            currentSubset = requireArguments().getString(KEY_SUBSET)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlgListBinding.inflate(inflater, container, false)

        binding!!.root.background = ThemeUtils.fetchBackgroundGradient(
            requireContext(),
            preferredTheme
        )

        binding!!.actionbar.puzzleName.setText(R.string.title_algorithms)
        binding!!.actionbar.puzzleCategory.text = currentSubset

        binding!!.actionbar.spinnerIcon.visibility = View.GONE
        binding!!.actionbar.navButtonHistory.visibility = View.GONE
        binding!!.actionbar.navButtonCategory.visibility = View.GONE
        binding!!.actionbar.navButtonSettings.setOnClickListener {
            mainActivity?.openDrawer()
        }

        setupRecyclerView()

        viewModel.setSubset(currentSubset)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.algorithms.collect { algs ->
                    algListAdapter?.submitList(algs)
                }
            }
        }

        // Register a receiver to update if something has changed
        registerReceiver(mAlgDataChangedReceiver)
        registerReceiver(mUIInteractionReceiver)

        return binding!!.getRoot()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // To fix memory leaks
        unregisterReceiver(mAlgDataChangedReceiver)
        unregisterReceiver(mUIInteractionReceiver)
    }

    fun reloadList() {
        viewModel.setSubset(currentSubset)
    }

    private fun setupRecyclerView() {
        val parentActivity: Activity? = activity

        algListAdapter = AlgListAdapter(requireActivity(), getParentFragmentManager())

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

        binding!!.list.setAdapter(algListAdapter)
    }

    companion object {
        private const val KEY_SUBSET = "subset"

        @JvmStatic
        fun newInstance(subset: String?): AlgListFragment {
            val fragment = AlgListFragment()
            val args = Bundle()
            args.putString(KEY_SUBSET, subset)
            fragment.setArguments(args)
            return fragment
        }
    }
}
