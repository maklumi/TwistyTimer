package com.aricneto.twistytimer.fragment.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.aricneto.twistify.databinding.DialogPuzzleSpinnerBinding
import com.aricneto.twistytimer.adapter.BottomSheetSpinnerAdapter
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Implements a layout for easy creation of spinner-like bottom sheet dialogs (like the ones seen in
 * the puzzle selection screen).
 */
class BottomSheetSpinnerDialog : BottomSheetDialogFragment() {
    private var binding: DialogPuzzleSpinnerBinding? = null

    private var mContext: Context? = null

    private var mAdapter: BottomSheetSpinnerAdapter? = null
    private var mClickListener: OnItemClickListener? = null

    private var titleText: String? = null
    private var titleIcon = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // This makes the bottomSheet dialog start in the expanded state
        dialog.setOnShowListener { dia: DialogInterface? ->
            val bottomDialog = dia as BottomSheetDialog
            val bottomSheet = bottomDialog.findViewById<FrameLayout>(R.id.design_bottom_sheet)
            BottomSheetBehavior.from<FrameLayout>(bottomSheet!!)
                .setState(BottomSheetBehavior.STATE_EXPANDED)
            BottomSheetBehavior.from<FrameLayout?>(bottomSheet).skipCollapsed = true
            BottomSheetBehavior.from<FrameLayout?>(bottomSheet).isHideable = true
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogPuzzleSpinnerBinding.inflate(inflater, container, false)

        mContext = context

        return binding!!.getRoot()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.list.adapter = mAdapter
        binding!!.list.onItemClickListener = mClickListener

        binding!!.title.text = titleText

        if (titleIcon != 0) {
            val icon: Drawable? =
                VectorDrawableCompat.create(mContext!!.resources, titleIcon, null)
            binding!!.title.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null)
        }

        binding!!.list.setOnTouchListener { v: View, event: MotionEvent? ->
            val action = event!!.action
            when (action) {
                MotionEvent.ACTION_DOWN -> v.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP -> v.parent.requestDisallowInterceptTouchEvent(false)
            }

            v.onTouchEvent(event)
            true
        }
    }

    fun setTitle(title: String?, @DrawableRes iconRes: Int) {
        titleText = title
        titleIcon = iconRes
    }

    fun setListAdapter(adapter: BottomSheetSpinnerAdapter?) {
        mAdapter = adapter
    }

    fun setListClickListener(clickListener: OnItemClickListener?) {
        mClickListener = clickListener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(): BottomSheetSpinnerDialog {
            return BottomSheetSpinnerDialog()
        }
    }
}
