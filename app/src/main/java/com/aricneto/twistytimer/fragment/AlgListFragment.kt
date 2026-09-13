package com.aricneto.twistytimer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.aricneto.twistytimer.fragment.dialog.AlgDialog
import com.aricneto.twistytimer.ui.screens.AlgListScreen
import com.aricneto.twistytimer.ui.theme.TwistyTheme
import com.aricneto.twistytimer.viewmodel.AlgViewModel


class AlgListFragment : BaseFragment() {
    private var currentSubset: String? = null
    private val viewModel: AlgViewModel by viewModels()


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
        viewModel.setSubset(currentSubset)
        return ComposeView(requireContext()).apply {
            setContent {
                TwistyTheme {
                    val algorithms by viewModel.algorithms.collectAsState()
                    AlgListScreen(
                        algorithms = algorithms,
                        subsetName = currentSubset,
                        onItemClick = { algorithm ->
                            val algDialog = AlgDialog.newInstance(algorithm.id)
                            algDialog.show(parentFragmentManager, "alg_dialog")
                        },
                        onSettingsClick = {
                            mainActivity?.openDrawer()
                        }
                    )
                }
            }
        }
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
