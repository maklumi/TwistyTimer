package com.aricneto.twistytimer.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.ItemTimeListBinding
import com.aricneto.twistytimer.fragment.dialog.TimeDialog
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.listener.DialogListener
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString
import com.aricneto.twistytimer.utils.TTIntent.ACTION_SELECTION_MODE_OFF
import com.aricneto.twistytimer.utils.TTIntent.ACTION_SELECTION_MODE_ON
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MODIFIED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_SELECTED
import com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_UNSELECTED
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS
import com.aricneto.twistytimer.utils.TTIntent.broadcast
import com.aricneto.twistytimer.utils.ThemeUtils.createSquareDrawable
import com.aricneto.twistytimer.utils.ThemeUtils.fetchAttrColor
import org.joda.time.DateTime

/**
 * Manages the list of timed solves. It formats the time strings, handles
 * penalties (DNF/+2), displays comment icons, and implements a multi-select mode
 * for bulk deleting or archiving solves.
 */
class SolveListAdapter(
    private val mContext: Context,
    private val mFragmentManager: FragmentManager
) : ListAdapter<Solve, SolveListAdapter.SolveViewHolder>(SolveDiffCallback()), DialogListener {

    private val cardBackground: Drawable = createSquareDrawable(
        mContext,
        fetchAttrColor(mContext, R.attr.colorItemListBackground),
        0, 10, 0f
    )
    private val selectedCardBackground: Drawable = createSquareDrawable(
        mContext,
        fetchAttrColor(mContext, R.attr.colorItemListBackgroundSelected),
        Color.BLACK, 10, 2f
    )
    private val mDateFormatSpec: String = mContext.getString(R.string.shortDateFormat)

    private var isInSelectionMode = false
    private val selectedItems = mutableSetOf<Long>()
    private var isLocked = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SolveViewHolder {
        val binding =
            ItemTimeListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SolveViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SolveViewHolder, position: Int) {
        val solve = getItem(position)
        holder.bind(solve)
    }

    inner class SolveViewHolder(val binding: ItemTimeListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(solve: Solve) {
            binding.date.text = DateTime(solve.date).toString(mDateFormatSpec)

            if (selectedItems.contains(solve.id)) {
                binding.card.background = selectedCardBackground
            } else {
                binding.card.background = cardBackground
            }

            binding.itemLayout.setOnClickListener {
                if (isInSelectionMode) {
                    toggleSelection(solve.id, bindingAdapterPosition)
                } else if (!isLocked) {
                    isLocked = true
                    val timeDialog = TimeDialog.newInstance(solve.id)
                    timeDialog.show(mFragmentManager, "time_dialog")
                    timeDialog.setDialogListener(this@SolveListAdapter)
                }
            }

            binding.itemLayout.setOnLongClickListener {
                if (!isInSelectionMode) {
                    isInSelectionMode = true
                    broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SELECTION_MODE_ON)
                    toggleSelection(solve.id, bindingAdapterPosition)
                }
                true
            }

            binding.timeText.text = Html.fromHtml(
                convertTimeToString(solve.time.toLong(), PuzzleUtils.FORMAT_SMALL_MILLI),
                FROM_HTML_MODE_LEGACY
            )
            binding.penaltyText.setTextColor(ContextCompat.getColor(mContext, R.color.red_material))

            when (solve.penalty) {
                PuzzleUtils.PENALTY_DNF -> {
                    binding.timeText.text = mContext.getString(R.string.do_not_finished)
                    binding.penaltyText.visibility = View.GONE
                }

                PuzzleUtils.PENALTY_PLUSTWO -> {
                    binding.penaltyText.text = "+2"
                    binding.penaltyText.visibility = View.VISIBLE
                }

                else -> binding.penaltyText.visibility = View.GONE
            }

            binding.commentIcon.visibility =
                if (solve.comment.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun toggleSelection(id: Long, position: Int) {
        if (selectedItems.contains(id)) {
            selectedItems.remove(id)
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_TIME_UNSELECTED)
        } else {
            selectedItems.add(id)
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_TIME_SELECTED)
        }

        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position)
        }

        if (selectedItems.isEmpty()) {
            isInSelectionMode = false
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SELECTION_MODE_OFF)
        }
    }

    fun getSelectedIds(): List<Long> = selectedItems.toList()

    override fun onUpdateDialog() {
        broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED)
    }

    override fun onDismissDialog() {
        isLocked = false
    }

    class SolveDiffCallback : DiffUtil.ItemCallback<Solve>() {
        override fun areItemsTheSame(oldItem: Solve, newItem: Solve): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Solve, newItem: Solve): Boolean =
            oldItem == newItem
    }
}
