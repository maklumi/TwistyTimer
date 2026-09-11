package com.aricneto.twistytimer.puzzle

import android.content.Context
import android.util.Log
import com.aricneto.twistify.R
import com.aricneto.twistytimer.utils.Prefs.getPrefs
import com.aricneto.twistytimer.utils.PuzzleUtils
import org.worldcubeassociation.tnoodle.puzzle.CubePuzzle
import org.worldcubeassociation.tnoodle.puzzle.CubePuzzle.CubeState
import org.worldcubeassociation.tnoodle.puzzle.ThreeByThreeCubePuzzle
import org.worldcubeassociation.tnoodle.scrambles.InvalidScrambleException
import java.util.Locale
import java.util.Random
import androidx.core.content.edit

/**
 * Provides scramble algorithms to be used in the trainer
 */
object TrainerScrambler {
    // The algorithms were taken from
    // github.com/Roman-/oll_trainer TODO: credit them in the app
    private val random = Random()

    private val Y_ROTATIONS = arrayOf<String?>("", "y", "y2", "y'")

    private val puzzle: CubePuzzle = ThreeByThreeCubePuzzle()
    private val solved: CubeState = puzzle.solvedState

    // The key preceding every trainer entry.
    // The version MUST NOT be decreased, only increased, as previous users may have
    // configurations saved in a different TRAINER version that is incompatible with the current
    // implementation, causing crashes.
    const val KEY_TRAINER: String = "TRAINER_V2"

    /**
     * Amount of different variations for each registered subset
     */
    private fun getSubsetVariations(subset: TrainerSubset): Int {
        return when (subset) {
            TrainerSubset.OLL -> 57
            TrainerSubset.PLL -> 0
            TrainerSubset.CMLL -> 42
        }
    }

    /**
     * Saves selected cases to preferences, to be fetched later
     * Preference will be saved in the format:
     * key: TRAINER[SUBSET][category]
     * set: ITEMS
     */
    fun saveSelectedItems(
        subset: TrainerSubset,
        category: String,
        selectedItems: MutableSet<String>?
    ) {
        getPrefs().edit {
            putStringSet(KEY_TRAINER + subset.name + category, selectedItems)
        }
    }

    /**
     * Saves selected cases to preferences, to be fetched later. Accepts a List<String> input.
     * Preference will be saved in the format:
     * key: TRAINER[SUBSET][category]
     * set: ITEMS
    </String> */
    fun saveSelectedItems(
        subset: TrainerSubset,
        category: String,
        selectedItems: MutableList<String>
    ) {
        saveSelectedItems(subset, category, HashSet(selectedItems))
    }

    /**
     * Utility function to rename a Trainer category, maintaining trainer subsets
     * @param subset
     * @param oldCategoryName
     * @param newCategoryName
     */
    @JvmStatic
    fun renameCategory(subset: TrainerSubset, oldCategoryName: String, newCategoryName: String) {
        val items = fetchSelectedItems(subset, oldCategoryName)
        getPrefs().edit { remove(KEY_TRAINER + subset.name + oldCategoryName) }
        saveSelectedItems(subset, newCategoryName, items)
    }

    /**
     * Fetches previously selected cases depending on current subset and category
     * @param subset
     * @param category
     * @return
     */
    fun fetchSelectedItems(subset: TrainerSubset, category: String?): MutableSet<String>? {
        return getPrefs()
            .getStringSet(
                KEY_TRAINER + subset.name + category,
                HashSet<String>()
            )
    }

    /**
     * Generates a random trainer case from the selected cases
     */
    fun generateTrainerCase(context: Context, subset: TrainerSubset, category: String?): String {
        val selectedItems: MutableSet<String>? = fetchSelectedItems(subset, category)
        var scramble = ""

        if (selectedItems?.isNotEmpty() == true) {
            try {
                // Fetch a random setup algorithm from the file trainer_scrambles.xml
                val caseAlg = fetchCaseAlgorithm(
                    context, subset.name, selectedItems.elementAt(
                        random.nextInt(selectedItems.size)
                    )
                )

                if (subset == TrainerSubset.CMLL) {
                    // For CMLL, use the algorithm directly as the scramble as requested
                    scramble = caseAlg.replace("\"", "") // Remove quotes if present
                } else {
                    // For others, use the solver to generate a random scramble for that state
                    val state = solved.applyAlgorithm(caseAlg) as CubeState?
                    scramble = (puzzle as ThreeByThreeCubePuzzle).solveIn(state, 20, null, null)
                }
            } catch (e: InvalidScrambleException) {
                e.printStackTrace()
            }
        } else {
            scramble = context.getString(R.string.trainer_help_message)
        }

        return if (subset == TrainerSubset.CMLL) {
            scramble // Don't apply extra y-rotation for CMLL as they are fixed scrambles
        } else {
            PuzzleUtils.applyRotationForAlgorithm(scramble, Y_ROTATIONS[random.nextInt(4)]!!)
        }
    }

    private fun fetchCaseAlgorithm(context: Context, subset: String?, name: String): String {
        val resources = context.resources

        // Finds an algorithm resource with a matching name on the file trainer_scrambles.xml
        try {
            // Find the resource
            val resId = resources.getIdentifier(
                "TRAINER_" + subset + "_" + name.replace(" ", "_").uppercase(Locale.getDefault()),
                "array",
                context.packageName
            )

            // Split the resource entries
            val res = resources.getStringArray(resId)

            // Return one of the entries
            return res[random.nextInt(res.size)]
        } catch (e: Exception) {
            Log.e("TRAINER_SCRAMBLE", "Error retrieving scramble: $e")
        }

        return "U"
    }

    enum class TrainerSubset {
        OLL, PLL, CMLL
    }
}


