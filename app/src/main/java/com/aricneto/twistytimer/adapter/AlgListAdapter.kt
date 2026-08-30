package com.aricneto.twistytimer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.ItemAlgListBinding
import com.aricneto.twistytimer.fragment.dialog.AlgDialog
import com.aricneto.twistytimer.items.Algorithm
import com.aricneto.twistytimer.listener.DialogListener
import com.aricneto.twistytimer.utils.AlgUtils.getCaseState
import com.aricneto.twistytimer.utils.AlgUtils.getPllArrow

open class AlgListAdapter(
    private val mContext: Context,
    private val mFragmentManager: FragmentManager
) : ListAdapter<Algorithm, AlgListAdapter.AlgHolder>(AlgDiffCallback()), DialogListener {

    // Locks opening new windows until the last one is dismissed
    var isLocked: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlgHolder {
        val binding = ItemAlgListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        val binding = LayoutInflater.from(parent.context).inflate(R.layout.item_alg_list, parent, false)
        return AlgHolder(binding)
    }

    override fun onBindViewHolder(holder: AlgHolder, position: Int) {
        val algorithm = getItem(position)
        handleAlgorithm(holder, algorithm)
    }

    override fun onUpdateDialog() {
        // Do nothing.
    }

    override fun onDismissDialog() {
        this.isLocked = false
    }

    open fun handleAlgorithm(holder: AlgHolder, algorithm: Algorithm) {
        val pState = getCaseState(mContext, algorithm.subset, algorithm.name)

        holder.binding.itemLayout.setOnClickListener {
            if (!this@AlgListAdapter.isLocked) {
                this@AlgListAdapter.isLocked = true
                val algDialog = AlgDialog.newInstance(algorithm.id)
                algDialog.show(mFragmentManager, "alg_dialog")
                algDialog.setDialogListener(this@AlgListAdapter)
            }
        }

        holder.binding.name.text = algorithm.name
        holder.binding.progressBar.progress = algorithm.progress
        holder.binding.cube.cubeState = pState

        // If the subset is PLL, it'll need to show the pll arrows.
        if (algorithm.subset == "PLL") {
            holder.binding.pllArrows.setImageDrawable(getPllArrow(mContext, algorithm.name))
            holder.binding.pllArrows.visibility = View.VISIBLE
        } else {
            holder.binding.pllArrows.visibility = View.GONE
        }
    }

    class AlgHolder(val binding: ItemAlgListBinding) : RecyclerView.ViewHolder(binding.getRoot())

    class AlgDiffCallback : DiffUtil.ItemCallback<Algorithm>() {
        override fun areItemsTheSame(oldItem: Algorithm, newItem: Algorithm): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Algorithm, newItem: Algorithm): Boolean {
            return oldItem == newItem
        }
    }
}
