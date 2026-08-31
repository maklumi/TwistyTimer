package com.aricneto.twistytimer.fragment.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.aricneto.twistify.databinding.DialogBottomsheetDetailBinding
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetDetailDialog : BottomSheetDialogFragment() {
    private var binding: DialogBottomsheetDetailBinding? = null

    private var hasHints = false

    private var detailText: String? = null
    private var hintText: String? = null
    private var detailTextSize = 0f

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // This makes the bottomSheet dialog start in the expanded state
        dialog.setOnShowListener { dia: DialogInterface? ->
            val bottomDialog = dia as BottomSheetDialog
            val bottomSheet = bottomDialog.findViewById<FrameLayout>(R.id.design_bottom_sheet)
            BottomSheetBehavior.from<FrameLayout>(bottomSheet!!)
                .setState(BottomSheetBehavior.STATE_EXPANDED)
            BottomSheetBehavior.from<FrameLayout>(bottomSheet).skipCollapsed = true
            BottomSheetBehavior.from<FrameLayout>(bottomSheet).isHideable = true
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogBottomsheetDetailBinding.inflate(inflater, container, false)

        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding!!.detailText.text = detailText
        binding!!.detailText.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            binding!!.detailText.textSize * detailTextSize
        )

        if (!hasHints) {
            setHintVisibility(999)
        } else {
            setHintVisibility(View.GONE)
        }
    }

    fun setDetailText(text: String?) {
        detailText = text
    }

    fun setHintText(text: String?) {
        hintText = text
        if (binding != null) {
            binding!!.hintText.text = hintText
        }
    }

    fun setDetailTextSize(size: Float) {
        this.detailTextSize = size
    }

    fun hasHints(hasHints: Boolean) {
        this.hasHints = hasHints
    }

    fun setHintVisibility(visibility: Int) {
        if (binding != null) {
            when (visibility) {
                View.VISIBLE -> {
                    binding!!.hintText.visibility = View.VISIBLE
                    binding!!.hintProgress.visibility = View.GONE
                    binding!!.hintTitle.visibility = View.VISIBLE
                    binding!!.hintDivider.visibility = View.VISIBLE
                }
                View.GONE -> {
                    binding!!.hintText.visibility = View.GONE
                    binding!!.hintProgress.visibility = View.VISIBLE
                    binding!!.hintTitle.visibility = View.VISIBLE
                    binding!!.hintDivider.visibility = View.VISIBLE
                }
                else -> {
                    binding!!.hintProgress.visibility = View.GONE
                    binding!!.hintText.visibility = View.GONE
                    binding!!.hintTitle.visibility = View.GONE
                    binding!!.hintDivider.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(): BottomSheetDetailDialog {
            return BottomSheetDetailDialog()
        }
    }
}
