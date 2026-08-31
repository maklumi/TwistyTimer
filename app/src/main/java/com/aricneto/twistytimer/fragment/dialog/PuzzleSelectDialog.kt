package com.aricneto.twistytimer.fragment.dialog

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogPuzzleSelectBinding
import com.aricneto.twistytimer.listener.DialogListenerMessage
import com.aricneto.twistytimer.utils.PuzzleUtils.getPuzzleInPosition
import androidx.core.graphics.drawable.toDrawable

class PuzzleSelectDialog : DialogFragment() {
    private var dialogListener: DialogListenerMessage? = null
    private var binding: DialogPuzzleSelectBinding? = null
    private var mContext: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogPuzzleSelectBinding.inflate(inflater, container, false)

        mContext = context

        dialog!!.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        binding!!.list.setHasFixedSize(true)

        val layoutManager = GridLayoutManager(mContext, 3, RecyclerView.VERTICAL, false)
        binding!!.list.layoutManager = layoutManager

        val puzzleAdapter = PuzzleSelectAdapter(
            dialogListener,
            arrayOf(
                Pair.create<String, Int>(getString(R.string.cube_222), R.drawable.ic_2x2),
                Pair.create<String, Int>(getString(R.string.cube_333), R.drawable.ic_3x3),
                Pair.create<String, Int>(getString(R.string.cube_444), R.drawable.ic_4x4),
                Pair.create<String, Int>(getString(R.string.cube_555), R.drawable.ic_5x5),
                Pair.create<String, Int>(getString(R.string.cube_666), R.drawable.ic_6x6),
                Pair.create<String, Int>(getString(R.string.cube_777), R.drawable.ic_7x7),
                Pair.create<String, Int>(getString(R.string.cube_skewb), R.drawable.ic_skewb),
                Pair.create<String, Int>(getString(R.string.cube_mega), R.drawable.ic_mega),
                Pair.create<String, Int>(getString(R.string.cube_pyra), R.drawable.ic_pyra),
                Pair.create<String, Int>(getString(R.string.cube_sq1), R.drawable.ic_sq1),
                Pair.create<String, Int>(getString(R.string.cube_clock), R.drawable.ic_clock)
            )
        )

        binding!!.list.adapter = puzzleAdapter

        return binding!!.root
    }

    fun setDialogListener(listener: DialogListenerMessage?) {
        this.dialogListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(): PuzzleSelectDialog {
            return PuzzleSelectDialog()
        }
    }
}

internal class PuzzleSelectAdapter(
    var dialogListener: DialogListenerMessage?,
    private val puzzles: Array<Pair<String, Int>>
) : RecyclerView.Adapter<PuzzleSelectAdapter.CardViewHolder>() {

    internal class CardViewHolder(var view: View) : RecyclerView.ViewHolder(view) {
        var title: TextView = view.findViewById(R.id.title)
        var icon: ImageView = view.findViewById(R.id.icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_puzzle_select, parent, false)
        val viewHolder = CardViewHolder(view)

        return viewHolder
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.title.text = puzzles[position].first
        holder.icon.setImageResource(puzzles[position].second!!)

        holder.view.setOnClickListener { _: View? ->
            if (dialogListener != null) dialogListener!!.onUpdateDialog(
                getPuzzleInPosition(position)
            )
        }
    }

    override fun getItemCount(): Int {
        return puzzles.size
    }
}
