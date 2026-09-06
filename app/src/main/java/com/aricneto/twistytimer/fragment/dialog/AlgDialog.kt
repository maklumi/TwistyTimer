package com.aricneto.twistytimer.fragment.dialog

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogAlgDetailsBinding
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.items.Algorithm
import com.aricneto.twistytimer.listener.DialogListener
import com.aricneto.twistytimer.utils.AlgUtils
import com.aricneto.twistytimer.utils.TTIntent
import com.aricneto.twistytimer.utils.TTIntent.broadcast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Shows the algList dialog
 */
class AlgDialog : DialogFragment() {
    private var binding: DialogAlgDetailsBinding? = null
    private var mContext: Context? = null

    private var mId: Long = 0
    private var algorithm: Algorithm? = null
    private var dialogListener: DialogListener? = null

    private val clickListener: View.OnClickListener = View.OnClickListener { view ->
        val algRepository = TwistyTimer.getAlgRepository()

        when (view.id) {
            R.id.editButton -> {
                val editView =
                    layoutInflater.inflate(
                        R.layout.dialog_input,
                        requireView().parent as ViewGroup,
                        false
                    )
                val editEditText = editView.findViewById<TextInputEditText>(R.id.edit_text)
                editEditText.setText(algorithm!!.algs)

                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.edit_algorithm)
                    .setView(editView)
                    .setPositiveButton(
                        R.string.action_done
                    ) { _: DialogInterface?, _: Int ->
                        val input = editEditText.getText().toString()
                        algorithm!!.algs = input
                        lifecycleScope.launch {
                            algRepository.updateAlgorithmAlg(mId, input)
                            binding!!.algText.text = input
                            updateList()
                        }
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }

            R.id.progressButton -> {
                val seekBar = layoutInflater.inflate(
                    R.layout.dialog_progress,
                    requireView().parent as ViewGroup,
                    false
                ) as SeekBar
                seekBar.progress = algorithm!!.progress
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.dialog_set_progress)
                    .setView(seekBar)
                    .setPositiveButton(
                        R.string.action_update
                    ) { _: DialogInterface?, _: Int ->
                        val seekProgress = seekBar.progress
                        algorithm!!.progress = seekProgress
                        lifecycleScope.launch {
                            algRepository.updateAlgorithmProgress(mId, seekProgress.toLong())
                            binding!!.progressBar.progress = seekProgress
                            updateList()
                        }
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }

            R.id.revertButton -> MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.dialog_revert_title_confirmation)
                .setMessage(R.string.dialog_revert_content_confirmation)
                .setPositiveButton(
                    R.string.action_reset
                ) { _: DialogInterface?, _: Int ->
                    algorithm!!.algs =
                        AlgUtils.getDefaultAlgs(algorithm!!.subset, algorithm!!.name)
                    lifecycleScope.launch {
                        algRepository.updateAlgorithmAlg(mId, algorithm!!.algs)
                        binding!!.algText.text = algorithm!!.algs
                    }
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAlgDetailsBinding.inflate(inflater, container, false)

        mContext = context
        mId = requireArguments().getLong("id")

        dialog!!.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        lifecycleScope.launch {
            val matchedAlgorithm = TwistyTimer.getAlgRepository().getAlgorithmById(mId)

            if (matchedAlgorithm != null) {
                algorithm = matchedAlgorithm
                binding!!.algText.text = algorithm!!.algs
                binding!!.nameText.text = algorithm!!.name

                binding!!.cube.cubeState =
                    AlgUtils.getCaseState(requireContext(), algorithm!!.subset, algorithm!!.name)

                binding!!.progressBar.progress = algorithm!!.progress

                binding!!.revertButton.setOnClickListener(clickListener)
                binding!!.progressButton.setOnClickListener(clickListener)
                binding!!.editButton.setOnClickListener(clickListener)

                // If the subset is PLL, it'll need to show the pll arrows.
                if (algorithm!!.subset == "PLL") {
                    binding!!.pllArrows.setImageDrawable(
                        AlgUtils.getPllArrow(
                            requireContext(),
                            algorithm!!.name
                        )
                    )
                    binding!!.pllArrows.visibility = View.VISIBLE
                }
            }
        }

        return binding!!.getRoot()
    }

    fun setDialogListener(listener: DialogListener?) {
        dialogListener = listener
    }

    private fun updateList() {
        broadcast(TTIntent.CATEGORY_ALG_DATA_CHANGES, TTIntent.ACTION_ALGS_MODIFIED)
        //dismiss();
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        if (dialogListener != null) dialogListener!!.onDismissDialog()
    }

    companion object {
        fun newInstance(id: Long): AlgDialog {
            val timeDialog = AlgDialog()
            val args = Bundle()
            args.putLong("id", id)
            timeDialog.setArguments(args)
            return timeDialog
        }
    }
}
