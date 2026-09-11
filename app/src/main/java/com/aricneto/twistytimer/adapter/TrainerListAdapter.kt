package com.aricneto.twistytimer.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentManager
import com.aricneto.twistify.R
import com.aricneto.twistytimer.fragment.dialog.AlgDialog
import com.aricneto.twistytimer.items.Algorithm
import com.aricneto.twistytimer.puzzle.TrainerScrambler.TrainerSubset
import com.aricneto.twistytimer.puzzle.TrainerScrambler.fetchSelectedItems
import com.aricneto.twistytimer.puzzle.TrainerScrambler.saveSelectedItems
import com.aricneto.twistytimer.utils.ThemeUtils.createSquareDrawable
import com.aricneto.twistytimer.utils.ThemeUtils.fetchAttrColor
import java.util.Locale

/**
 * Extends [AlgListAdapter] specifically for the Trainer mode. It adds multi-selection logic,
 * allowing users to pick exactly which cases they want to practice. It automatically saves
 * these selections to preferences.
 */
class TrainerListAdapter(
    context: Context,
    private val fragmentManager: FragmentManager,
    subset: TrainerSubset,
    category: String
) : AlgListAdapter(context, fragmentManager) {

    private val selectedItems: MutableList<String> = ArrayList()
    var currentSubset: TrainerSubset
    var currentPuzzleCategory: String

    private var cardBackground: Drawable? = null
    private var selectedCardBackground: Drawable? = null

    init {
        cardBackground = createSquareDrawable(
            context,
            fetchAttrColor(context, R.attr.colorSurfaceContainer),
            0, 14, 0f
        )
        selectedCardBackground = createSquareDrawable(
            context,
            fetchAttrColor(context, R.attr.colorSurfaceVariant),
            Color.BLACK, 14, 2f
        )

        val fetched = fetchSelectedItems(subset, category)
        fetched?.forEach { it.let { name -> selectedItems.add(name) } }

        this.currentSubset = subset
        this.currentPuzzleCategory = category
    }

    private fun isSelected(name: String): Boolean {
        return selectedItems.contains(name)
    }

    fun selectAll() {
        val size = selectedItems.size
        selectedItems.clear()
        when (currentSubset) {
            TrainerSubset.OLL -> if (size != 57) {
                for (i in 1..57) {
                    selectedItems.add("OLL " + String.format(Locale.US, "%02d", i))
                }
            }

            TrainerSubset.PLL -> if (size != 21) {
                val pllCases = arrayOf(
                    "H",
                    "Ua",
                    "Ub",
                    "Z",
                    "Aa",
                    "Ab",
                    "E",
                    "F",
                    "Ga",
                    "Gb",
                    "Gc",
                    "Gd",
                    "Ja",
                    "Jb",
                    "Na",
                    "Nb",
                    "Ra",
                    "Rb",
                    "T",
                    "V",
                    "Y"
                )
                selectedItems.addAll(pllCases)
            }

            TrainerSubset.CMLL -> if (size != 42) {
                val cases = arrayOf(
                    "U fs", "U bs", "U fr", "U rows", "U xc", "U br",
                    "T lb", "T rb", "T rows", "T fr", "T br", "T cols",
                    "L mir", "L inv", "L pure", "L fc", "L diag", "L bc",
                    "S lb", "S xc", "S fs", "S cols", "S rb", "S bs",
                    "As rb", "As cols", "As bs", "As xc", "As fs", "As lb",
                    "H cols", "H rows", "H col", "H row",
                    "Pi rb", "Pi bs", "Pi xc", "Pi fs", "Pi cols", "Pi lb",
                    "O adj", "O diag"
                )
                selectedItems.addAll(cases)
            }
        }
        saveSelectedItems(currentSubset, currentPuzzleCategory, selectedItems)
        notifyItemRangeChanged(0, itemCount)
    }

    private fun toggleSelection(name: String, card: CardView) {
        if (!isSelected(name)) {
            selectedItems.add(name)
            card.background = selectedCardBackground
        } else {
            selectedItems.remove(name)
            card.background = cardBackground
        }
        saveSelectedItems(currentSubset, currentPuzzleCategory, selectedItems)
    }

    override fun handleAlgorithm(holder: AlgHolder, algorithm: Algorithm) {
        super.handleAlgorithm(holder, algorithm)

        if (isSelected(algorithm.name)) {
            holder.binding.card.background = selectedCardBackground
        } else {
            holder.binding.card.background = cardBackground
        }

        holder.binding.itemLayout.setOnClickListener {
            toggleSelection(algorithm.name, holder.binding.card)
        }

        holder.binding.itemLayout.setOnLongClickListener {
            if (!isLocked) {
                this@TrainerListAdapter.isLocked = true
                val algDialog = AlgDialog.newInstance(algorithm.id)
                algDialog.show(fragmentManager, "alg_dialog")
                algDialog.setDialogListener(this@TrainerListAdapter)
            }
            true
        }
    }
}
