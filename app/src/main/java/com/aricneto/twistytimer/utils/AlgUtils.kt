package com.aricneto.twistytimer.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.aricneto.twistify.R
import com.aricneto.twistytimer.utils.Prefs.getString

/**
 * Used by the alg list
 */
object AlgUtils {
    private var colorLetterMap: HashMap<Char, Int> = HashMap()
    private var colorStates: Array<String> = arrayOf()
    private const val EMPTYSUBSET = ""

    private var CASES_PLL: MutableList<String> = mutableListOf()

    private var CASES_CMLL: MutableList<String> = mutableListOf()

    val colorLetterHashMap: HashMap<Char, Int>
        /**
         * This function returns a hashmap which contains the colors for each face of the cube
         */
        get() {
            if (colorLetterMap.isEmpty()) {
                colorLetterMap = HashMap(7)
                colorLetterMap['Y'] = ("#" + (getString(R.string.pk_cube_down_color, "FDD835")
                    ?: "FDD835")).toColorInt()
                colorLetterMap['R'] = ("#" + (getString(R.string.pk_cube_right_color, "EC0000")
                    ?: "EC0000")).toColorInt()
                colorLetterMap['G'] = ("#" + (getString(R.string.pk_cube_front_color, "02D040")
                    ?: "02D040")).toColorInt()
                colorLetterMap['B'] = ("#" + (getString(R.string.pk_cube_back_color, "304FFE")
                    ?: "304FFE")).toColorInt()
                colorLetterMap['O'] = ("#" + (getString(R.string.pk_cube_left_color, "FF8B24")
                    ?: "FF8B24")).toColorInt()
                colorLetterMap['W'] = ("#" + (getString(R.string.pk_cube_top_color, "FFFFFF")
                    ?: "FFFFFF")).toColorInt()
                colorLetterMap['N'] = "#A7A7A7".toColorInt()
                colorLetterMap['X'] = 0
            }

            return colorLetterMap
        }

    /**
     * Returns an array containing all color states for the given alg subset.
     * The subset name is stored in the database [com.aricneto.twistytimer.database.AlgRepository]
     * @return
     */
    fun getCaseColorStates(context: Context, subset: String): Array<String> {
        if (colorStates.isEmpty() || subset != EMPTYSUBSET) {
            try {
                val resId = when (subset) {
                    "OLL" -> R.array.alg_reference_OLL
                    "PLL" -> R.array.alg_reference_PLL
                    "CMLL" -> R.array.alg_reference_CMLL
                    else -> 0
                }

                if (resId != 0) {
                    colorStates = context.resources.getStringArray(resId)
                }
            } catch (e: Exception) {
                Log.e("ALGUTILS", "Error retrieving subset: $e")
            }
        }

        return colorStates
    }

    private val subsetCases: MutableList<String>
        /**
         * Returns an array list containing all cases of a subset
         * currently, we only have PLL subset with weird names,
         * this function can be expanded in the future
         * @return
         */
        get() {
            if (CASES_PLL.isEmpty()) {
                val casesPLL = arrayOf(
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
                CASES_PLL = ArrayList(listOf(*casesPLL))
            }
            return CASES_PLL
        }

    val subsetCasesCMLL: MutableList<String>
        get() {
            if (CASES_CMLL.isEmpty()) {
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
                CASES_CMLL = ArrayList(listOf(*cases))
            }
            return CASES_CMLL
        }

    /**
     * Converts a case name to its specific id reference in the reference_states.xml file
     * @return
     */
    fun caseNameToSubsetId(subset: String, name: String): Int {
        try {
            when (subset) {
                "PLL" -> return subsetCases.indexOf(name)
                "OLL" -> {
                    if (name.length >= 6) {
                        return name.substring(4).toInt() - 1
                    }
                }
                "CMLL" -> {
                    val baseName = name.replace("CMLL ", "")
                    return subsetCasesCMLL.indexOf(baseName)
                }
            }
        } catch (_: Exception) {
            return -1
        }
        return -1
    }

    /**
     * Translates a char to a color
     * i.e: Y -> yellow
     * The index is a number between 0-24 (number of cells in a 2d array)
     * 
     * @param state
     * @param index
     * @return
     */
    @ColorInt
    fun getColorFromStateIndex(state: String, index: Int): Int {
        val char = state.getOrNull(index) ?: return Color.WHITE
        return colorLetterHashMap[char] ?: Color.WHITE
    }

    @JvmStatic
    fun getCaseState(context: Context, subset: String, name: String): String {
        val states = getCaseColorStates(context, subset)
        val index = caseNameToSubsetId(subset, name)
        return if (index in states.indices) states[index] else ""
    }

    @JvmStatic
    fun getPllArrow(context: Context, name: String): Drawable? {
        when (name) {
            "H" -> return ContextCompat.getDrawable(context, R.drawable.pll_h_perm)
            "Ua" -> return ContextCompat.getDrawable(context, R.drawable.pll_ua_perm)
            "Ub" -> return ContextCompat.getDrawable(context, R.drawable.pll_ub_perm)
            "Z" -> return ContextCompat.getDrawable(context, R.drawable.pll_z_perm)
            "Aa" -> return ContextCompat.getDrawable(context, R.drawable.pll_aa_perm)
            "Ab" -> return ContextCompat.getDrawable(context, R.drawable.pll_ab_perm)
            "E" -> return ContextCompat.getDrawable(context, R.drawable.pll_e_perm)
            "F" -> return ContextCompat.getDrawable(context, R.drawable.pll_f_perm)
            "Ga" -> return ContextCompat.getDrawable(context, R.drawable.pll_ga_perm)
            "Gb" -> return ContextCompat.getDrawable(context, R.drawable.pll_gb_perm)
            "Gc" -> return ContextCompat.getDrawable(context, R.drawable.pll_gc_perm)
            "Gd" -> return ContextCompat.getDrawable(context, R.drawable.pll_gd_perm)
            "Ja" -> return ContextCompat.getDrawable(context, R.drawable.pll_ja_perm)
            "Jb" -> return ContextCompat.getDrawable(context, R.drawable.pll_jb_perm)
            "Na" -> return ContextCompat.getDrawable(context, R.drawable.pll_na_perm)
            "Nb" -> return ContextCompat.getDrawable(context, R.drawable.pll_nb_perm)
            "Ra" -> return ContextCompat.getDrawable(context, R.drawable.pll_ra_perm)
            "Rb" -> return ContextCompat.getDrawable(context, R.drawable.pll_rb_perm)
            "T" -> return ContextCompat.getDrawable(context, R.drawable.pll_t_perm)
            "V" -> return ContextCompat.getDrawable(context, R.drawable.pll_v_perm)
            "Y" -> return ContextCompat.getDrawable(context, R.drawable.pll_y_perm)
        }
        return null
    }

    @JvmStatic
    fun getDefaultAlgs(subset: String, name: String): String {
        when (subset) {
            "OLL" -> when (name) {
                "OLL 01" -> return "R U2 R2' F R F' U2 R' F R F' \n" +
                        "R U B' R B R2 U' R' F R F' \n" +
                        "y R U' R2 D' r U' r' D R2 U R' \n" +
                        "r U R' U R' r2 U' R' U R' r2 U2 r'"

                "OLL 02" -> return "F R U R' U' F' f R U R' U' f' \n" +
                        "F R U R' U' S R U R' U' f' \n" +
                        "y r U r' U2 R U2 R' U2 r U' r' \n" +
                        "F R U r' U' R U R' M' U' F'"

                "OLL 03" -> return "y' f R U R' U' f' U' F R U R' U' F' \n" +
                        "r' R2 U R' U r U2 r' U M' \n" +
                        "r' R U R' F2 R U L' U L M' \n" +
                        "y F U R U' R' F' U F R U R' U' F'"

                "OLL 04" -> return "y' f R U R' U' f' U F R U R' U' F' \n" +
                        "M U' r U2 r' U' R U' R2 r \n" +
                        "y F U R U' R' F' U' F R U R' U' F' \n" +
                        "y2 r R2' U' R U' r' U2 r U' M"

                "OLL 05" -> return "r' U2 R U R' U r \n" +
                        "y2 l' U2 L U L' U l \n" +
                        "y2 R' F2 r U r' F R \n" +
                        "L' U' L2 F' L' F2 U' F'"

                "OLL 06" -> return "r U2 R' U' R U' r' \n" +
                        "y2 l U2 L' U' L U' l' \n" +
                        "y2 R U R2 F R F2 U F \n" +
                        "y' x' D R2 U' R' U R' D' x"

                "OLL 07" -> return "r U R' U R U2 r' \n" +
                        "L' U2 L U2 L F' L' F \n" +
                        "F R' F' R U2 R U2 R' \n" +
                        "r U r' U R U' R' r U' r'"

                "OLL 08" -> return "y2 r' U' R U' R' U2 r \n" +
                        "l' U' L U' L' U2 l \n" +
                        "R U2 R' U2 R' F R F' \n" +
                        "F' L F L' U2 L' U2 L"

                "OLL 09" -> return "y R U R' U' R' F R2 U R' U' F' \n" +
                        "y2 R' U' R U' R' U R' F R F' U R \n" +
                        "r' R2 U2 R' U' R U' R' U' M' \n" +
                        "y' L' U' L U' L F' L' F L' U2 L"

                "OLL 10" -> return "R U R' U R' F R F' R U2 R' \n" +
                        "R U R' y R' F R U' R' F' R \n" +
                        "y2 L' U' L U L F' L2 U' L U F \n" +
                        "R U R' y' r' U r U' r' U' r"

                "OLL 11" -> return "r' R2 U R' U R U2 R' U M' \n" +
                        "M R U R' U R U2 R' U M' \n" +
                        "r U R' U R' F R F' R U2 r' \n" +
                        "y2 r U R' U R' F R F' R U2 r'"

                "OLL 12" -> return "F R U R' U' F' U F R U R' U' F' \n" +
                        "y' r R2' U' R U' R' U2 R U' R r' \n" +
                        "y M U2 R' U' R U' R' U2 R U M' \n" +
                        "y M L' U' L U' L' U2 L U' M'"

                "OLL 13" -> return "r U' r' U' r U r' F' U F \n" +
                        "F U R U' R2 F' R U R U' R' \n" +
                        "F U R U2 R' U' R U R' F' \n" +
                        "r U' r' U' r U r' y' R' U R"

                "OLL 14" -> return "R' F R U R' F' R F U' F' \n" +
                        "R' F R U R' F' R y' R U' R' \n" +
                        "F' U' r' F r2 U r' U' r' F r \n" +
                        "r' U r U r' U' r y R U' R'"

                "OLL 15" -> return "r' U' r R' U' R U r' U r \n" +
                        "y2 l' U' l L' U' L U l' U l \n" +
                        "r' U' M' U' R U r' U r \n" +
                        "y' R' U2 R U R' F U R U' R' F' R"

                "OLL 16" -> return "r U r' R U R' U' r U' r' \n" +
                        "r U M U R' U' r U' r' \n" +
                        "y2 R' F R U R' U' F' R U' R' U2 R \n" +
                        "y2 l U l' L U L' U' l U' l'"

                "OLL 17" -> return "R U R' U R' F R F' U2 R' F R F' \n" +
                        "f R U R' U' f' U' R U R' U' R' F R F' \n" +
                        "y2 F R' F' R2 r' U R U' R' U' M' \n" +
                        "y' F' r U r' U' S r' F r S'"

                "OLL 18" -> return "r U R' U R U2 r2 U' R U' R' U2 r \n" +
                        "y R U2 R2 F R F' U2 M' U R U' r' \n" +
                        "y2 F R U R' d R' U2 R' F R F' \n" +
                        "y2 F R U R' U y' R' U2 R' F R F'"

                "OLL 19" -> return "M U R U R' U' M' R' F R F' \n" +
                        "r' R U R U R' U' r R2' F R F' \n" +
                        "r' U2 R U R' U r2 U2 R' U' R U' r' \n" +
                        "R' U2 F R U R' U' F2 U2 F R"

                "OLL 20" -> return "M U R U R' U' M2 U R U' r' \n" +
                        "r U R' U' M2 U R U' R' U' M' \n" +
                        "M' U M' U M' U M' U' M' U M' U M' U M' \n" +
                        "M' U' R' U' R U M2' U' R' U r"

                "OLL 21" -> return "y R U2 R' U' R U R' U' R U' R' \n" +
                        "y F R U R' U' R U R' U' R U R' U' F' \n" +
                        "R U R' U R U' R' U R U2 R' \n" +
                        "R' U' R U' R' U R U' R' U2 R"

                "OLL 22" -> return "R U2 R2 U' R2 U' R2 U2 R \n" +
                        "f R U R' U' f' F R U R' U' F' \n" +
                        "R' U2 R2 U R2 U R2 U2 R' \n" +
                        "R U2' R2' U' R2 U' R2' U2 R"

                "OLL 23" -> return "R2 D R' U2 R D' R' U2 R' \n" +
                        "y2 R2 D' R U2 R' D R U2 R \n" +
                        "y R U R' U' R U' R' U2 R U' R' U2 R U R' \n" +
                        "R U R' U R U2 R2 U' R U' R' U2 R"

                "OLL 24" -> return "r U R' U' r' F R F' \n" +
                        "y2 l' U' L U R U' r' F \n" +
                        "y' x' R U R' D R U' R' D' x \n" +
                        "r U R' U' L' U R U' x'"

                "OLL 25" -> return "y F' r U R' U' r' F R \n" +
                        "R' F R B' R' F' R B \n" +
                        "F R' F' r U R U' r' \n" +
                        "y2 R U2 R' U' R U R' U' R U R' U' R U' R'"

                "OLL 26" -> return "y R U2 R' U' R U' R' \n" +
                        "R' U' R U' R' U2 R \n" +
                        "y2 L' U' L U' L' U2 L \n" +
                        "R' U L U' R U L'"

                "OLL 27" -> return "R U R' U R U2 R' \n" +
                        "y' R' U2 R U R' U R \n" +
                        "R U' L' U R' U' L \n" +
                        "y L' U2 L U L' U L"

                "OLL 28" -> return "r U R' U' M U R U' R' \n" +
                        "y2 M' U M U2 M' U M \n" +
                        "M U M' U2 M U M' \n" +
                        "y' M' U' M U2 M' U' M"

                "OLL 29" -> return "M U R U R' U' R' F R F' M' \n" +
                        "r2 D' r U r' D r2 U' r' U' r \n" +
                        "y R U R' U' R U' R' F' U' F R U R' \n" +
                        "y2 R' F R F' R U2 R' U' F' U' F"

                "OLL 30" -> return "M U' L' U' L U L F' L' F M' \n" +
                        "y' r' D' r U' r' D r2 U' r' U r U r' \n" +
                        "y2 F R' F R2 U' R' U' R U R' F2 \n" +
                        "R2 U R' B' R U' R2 U R B R'"

                "OLL 31" -> return "R' U' F U R U' R' F' R \n" +
                        "y2 S' L' U' L U L F' L' f \n" +
                        "y' F R' F' R U R U R' U' R U' R' \n" +
                        "y S R U R' U' f' U' F"

                "OLL 32" -> return "S R U R' U' R' F R f' \n" +
                        "R U B' U' R' U R B R' \n" +
                        "y2 L U F' U' L' U L F L' \n" +
                        "R d L' d' R' U l U l'"

                "OLL 33" -> return "R U R' U' R' F R F' \n" +
                        "F R U' R' U R U R' F' \n" +
                        "y2 L' U' L U L F' L' F \n" +
                        "y' r' U' r' D' r U r' D r2"

                "OLL 34" -> return "y2 R U R' U' B' R' F R F' B \n" +
                        "y2 R U R2 U' R' F R U R U' F' \n" +
                        "F R U R' U' R' F' r U R U' r' \n" +
                        "y2 R U R' U' y' r' U' R U M'"

                "OLL 35" -> return "R U2 R2' F R F' R U2 R' \n" +
                        "f R U R' U' f' R U R' U R U2 R' \n" +
                        "y' R U2 R' U' R U' R' U2 F R U R' U' F' \n" +
                        "R U2 R' U' y' r' U r U' r' U' r"

                "OLL 36" -> return "y2 L' U' L U' L' U L U L F' L' F \n" +
                        "R' U' R U' R' U R U l U' R' U x \n" +
                        "R' U' R U' R' U R U R y R' F' R \n" +
                        "R U2 r D r' U2 r D' R' r'"

                "OLL 37" -> return "F R U' R' U' R U R' F' \n" +
                        "F R' F' R U R U' R' \n" +
                        "R' F R F' U' F' U F \n" +
                        "y' R U2 R' F R' F' R2 U2 R'"

                "OLL 38" -> return "R U R' U R U' R' U' R' F R F' \n" +
                        "L' U' L F L' U' L U L F' L' U L F' L' F"

                "OLL 39" -> return "y L F' L' U' L U F U' L' \n" +
                        "y' R U R' F' U' F U R U2 R' \n" +
                        "y' R B' R' U' R U B U' R' \n" +
                        "R' r' D' r U' r' D r U R"

                "OLL 40" -> return "y R' F R U R' U' F' U R \n" +
                        "R r D r' U r D' r' U' R' \n" +
                        "y' f R' F' R U R U' R' S' \n" +
                        "y' F R U R' U' F' R U R' U R U2 R'"

                "OLL 41" -> return "y2 R U R' U R U2' R' F R U R' U' F' \n" +
                        "R U' R' U2 R U y R U' R' U' F' \n" +
                        "y' L F' L' F L F' L' F L' U' L U L' U' L \n" +
                        "f R U R' U' f' U' R U R' U R U2 R'"

                "OLL 42" -> return "R' U' R U' R' U2 R F R U R' U' F' \n" +
                        "y R' F R F' R' F R F' R U R' U' R U R' \n" +
                        "L' U L U2 L' U' y' L' U L U F \n" +
                        "R' U R U2 R' U' F' U F U R"

                "OLL 43" -> return "f' L' U' L U f \n" +
                        "y2 F' U' L' U L F \n" +
                        "y R' U' F' U F R \n" +
                        "y2 R' U' F R' F' R U R"

                "OLL 44" -> return "f R U R' U' f' \n" +
                        "y2 F U R U' R' F' \n" +
                        "y2 r U x' R U' R' U x U' r' \n" +
                        "y' L d R U' R' F'"

                "OLL 45" -> return "F R U R' U' F' \n" +
                        "y2 f U R U' R' f' \n" +
                        "y2 F' L' U' L U F \n" +
                        "F R2 D R' U R D' R2 U' F'"

                "OLL 46" -> return "R' U' R' F R F' U R \n" +
                        "y F R U R' y' R' U R U2 R' \n" +
                        "y2 r' F' L' U L U' F r"

                "OLL 47" -> return "F' L' U' L U L' U' L U F \n" +
                        "R' U' R' F R F' R' F R F' U R \n" +
                        "R' U' l' U R U' R' U R U' x' U R \n" +
                        "y2 B' R' U' R U R' U' R U B"

                "OLL 48" -> return "F R U R' U' R U R' U' F' \n" +
                        "R U2 R' U' R U R' U2 R' F R F'"

                "OLL 49" -> return "y2 r U' r2 U r2 U r2 U' r \n" +
                        "l U' l2 U l2 U l2 U' l \n" +
                        "R B' R2 F R2 B R2 F' R \n" +
                        "y2 R' F R' F' R2 U2 B' R B R'"

                "OLL 50" -> return "r' U r2 U' r2' U' r2 U r' \n" +
                        "y2 R' F R2 B' R2 F' R2 B R' \n" +
                        "y' R U2 R' U' R U' R' F R U R' U' F' \n" +
                        "y2 l' U l2 U' l2 U' l2 U l'"

                "OLL 51" -> return "f R U R' U' R U R' U' f' \n" +
                        "y2 F U R U' R' U R U' R' F' \n" +
                        "y' R' U' R' F R F' R U' R' U2 R \n" +
                        "y2 f' L' U' L U L' U' L U f"

                "OLL 52" -> return "R U R' U R d' R U' R' F' \n" +
                        "R' U' R U' R' d R' U R B \n" +
                        "R' U' R U' R' U F' U F R \n" +
                        "R U R' U R U' y R U' R' F'"

                "OLL 53" -> return "r' U' R U' R' U R U' R' U2 r \n" +
                        "y2 l' U' L U' L' U L U' L' U2 l \n" +
                        "y r' U2 R U R' U' R U R' U r \n" +
                        "y' l' U2 L U L' U' L U L' U l"

                "OLL 54" -> return "r U R' U R U' R' U R U2 r' \n" +
                        "y' r U2 R' U' R U R' U' R U' r' \n" +
                        "F' L' U' L U L' U L U' L' U' L F \n" +
                        "y2 F R' F' R U2 F2 L F L' F"

                "OLL 55" -> return "R U2 R2 U' R U' R' U2 F R F' \n" +
                        "y R' F R U R U' R2 F' R2 U' R' U R U R' \n" +
                        "r U2 R2 F R F' U2 r' F R F' \n" +
                        "R' U2 R2 U R' U R U2 y R' F' R"

                "OLL 56" -> return "r U r' U R U' R' U R U' R' r U' r' \n" +
                        "F R U R' U' R F' r U R' U' r' \n" +
                        "y f R U R' U' f' F R U R' U' R U R' U' F' \n" +
                        "r' U' r U' R' U R U' R' U R r' U r"

                "OLL 57" -> return "R U R' U' M' U R U' r' \n" +
                        "M' U M' U M' U2 M U M U M \n" +
                        "R U R' U' r R' U R U' r' \n" +
                        "M' U M' U M' U M' U2 M' U M' U M' U M'"
            }

            "PLL" -> when (name) {
                "Aa" -> return "R' F R' B2 R F' R' B2 R2\n" +
                        "(x') R' D R' U2 R D' R' U2 R2 (x)\n" +
                        "(x) R' U R' D2 R U' R' D2 R2 (x')\n" +
                        "l' U R' D2 R U' R' D2 R2 (x')"

                "Ab" -> return "R B' R F2 R' B R F2 R2\n" +
                        "(x) R D' R U2 R' D R U2 R2 (x')\n" +
                        "(x') R U' R D2 R' U R D2 R2 (x)\n" +
                        "(y' x) R2 D2 R U R' D2 R U' R (x')"

                "E" -> return "y x' R U' R' D R U R' D' R U R' D R U' R' D' x\n" +
                        "R2 U R' U' y R U R' U' R U R' U' R U R' y' R U' R2\n" +
                        "z U2' R2' F R U R' U' R U R' U' R U R' U' F' R2 U2'\n" +
                        "y x' R U' R' D R U R' u2 R' U R D R' U' R x"

                "F" -> return "R' U R U' R2 F' U' F U R F R' F' R2 U'\n" +
                        "R' U R U' R2 (y') R' U' R U (y x) R U R' U' R2 (x')\n" +
                        "R' U' F' R U R' U' R' F R2 U' R' U' R U R' U R\n" +
                        "(y’) L U F L' U' L U L F' L2 U L U L' U' L U' L'"

                "Ga" -> return "R2 U (R' U R' U') R U' R2 D U' R' U R D'\n" +
                        "(y) R2' u R' U R' U' R u' R2 (y') R' U R\n" +
                        "(y) R2 U R' U R' U' R U' R2 D U' R' U R D'\n" +
                        "(y2) F2' D R' U R' U' R D' F2 L' U L"

                "Gb" -> return "R' U' R y R2 u R' U R U' R u' R2\n" +
                        "R' U' R U D' R2 U R' U R U' R U' R2 D\n" +
                        "y F' U' F R2 u R' U R U' R u' R2\n" +
                        "R' d' F R2 u R' U R U' R u' R2"

                "Gc" -> return "(y) R2' u' R U' R U R' u R2 (y) R U' R'\n" +
                        "(y) R2' u' R U' R U R' u R2 B U' B'\n" +
                        "(y) R2' U' R U' R U R' U R2 D' U R U' R' D\n" +
                        "(y) R2' D' F U' F U F' D R2 B U' B'"

                "Gd" -> return "(y2) R U R' (y') R2 u' R U' R' U R' u R2\n" +
                        "(y2) R U R' F2 D' L U' L' U L' D F2\n" +
                        "(y2) L U2 L' U F' L' U' L U L F U L' U' L' U L	\n" +
                        "(y2) l2 U' L2 U' F2 L' U' R U2 L' U l (x')"

                "H" -> return "M2 U M2 U2 M2 U M2\n" +
                        "M2 U' M2 U2 M2 U' M2\n" +
                        "R2 U2 R U2 R2 U2 R2 U2 R U2 R2\n" +
                        "M2' U' M2' U2' M2' U' M2'"

                "Ja" -> return "B' U F' U2 B U' B' U2 F B U'\n" +
                        "(y) R' U L' U2 R U' R' U2 R L\n" +
                        "(y') L' U R' U2 L U' L' U2 R L\n" +
                        "(y') L' U R' (z) R2 U R' U' R2 U D (z')"

                "Jb" -> return "R U R' F' R U R' U' R' F R2 U' R' U'\n" +
                        "R U2 R' U' R U2 L' U R' U' r x\n" +
                        "R U2 R' U' R U2 L' U R' U' L\n" +
                        "L' U R U' L U2' R' U R U2' R'"

                "Na" -> return "R U R' U R U R' F' R U R' U' R' F R2 U' R' U2 R U' R'\n" +
                        "L U' R U2 L' U R' L U' R U2 L' U R'\n" +
                        "z U R' D R2 U' R D' U R' D R2 U' R D' z'\n" +
                        "r' D r U2 r' D r U2 r' D r U2 r' D r U2 r' D r"

                "Nb" -> return "R' U L' U2 R U' L R' U L' U2 R U' L\n" +
                        "R' U R U' R' F' U' F R U R' F R' F' R U' R\n" +
                        "z D' R U' R2 D R' U D' R U' R2 D R' U z'\n" +
                        "z U' R D' R2 U R' D U' R D' R2 U R' D z'"

                "Ra" -> return "(y2) L U2 L' U2 L F' L' U' L U L F L2\n" +
                        "(y') R U R' F' R U2 R' U2 R' F R U R U2 R'\n" +
                        "(y') R U' R' U' R U R D R' U' R D' R' U2 R'\n" +
                        "R U2 R' U2 R B' R' U' R U R B R2 U"

                "Rb" -> return "R' U2 R U2 R' F R U R' U' R' F' R2\n" +
                        "R' U2 R' D' R U' R' D R U R U' R' U' R\n" +
                        "y R2 F R U R U' R' F' R U2 R' U2 R\n" +
                        "y' R U2' R' U2 R' F R2 U' R' U' R U R' F' R U R' U R U2 R'"

                "T" -> return "R U R' U' R' F R2 U' R' U' R U R' F'\n" +
                        "R U R' U' R' F R2 U' R' U F' L' U L\n" +
                        "R2 U R2 U' R2 U' D R2 U' R2 U R2 D'\n" +
                        "y F2 D R2 U' R2 F2 D' L2 U L2 U'"

                "Ua" -> return "R2 U' R' U' R U R U R U' R\n" +
                        "y2 R U' R U R U R U' R' U' R2\n" +
                        "M2 U M' U2 M U M2\n" +
                        "y2 M2 U M U2 M' U M2"

                "Ub" -> return "R' U R' U' R' U' R' U R U R2\n" +
                        "y2 M2 U' M U2 M' U' M2\n" +
                        "y2 R2' U R U R' U' R' U' R' U R'\n" +
                        "M2 U' M' U2 M U' M2"

                "V" -> return "R' U R' d' R' F' R2 U' R' U R' F R F\n" +
                        "R' U R' U' y R' F' R2 U' R' U R' F R F\n" +
                        "z D' R2 D R2' U R' D' R U' R U R' D R U'\n" +
                        "R U2 R' D R U' R U' R U R2 D R' U' R D2"

                "Y" -> return "F R U' R' U' R U R' F' R U R' U' R' F R F'\n" +
                        "F R' F R2 U' R' U' R U R' F' R U R' U' F'\n" +
                        "R2 U' R2 U' R2 U y' R U R' B2 R U' R'\n" +
                        "R2 U' R' U R U' y' x' L' U' R U' R' U' L U"

                "Z" -> return "M2 U M2 U M' U2 M2 U2 M'\n" +
                        "y M2' U' M2' U' M' U2' M2' U2' M'\n" +
                        "M' U' M2' U' M2' U' M' U2' M2'\n" +
                        "R' U' R2 U R U R' U' R U R U' R U' R'"
            }

            "CMLL" -> when (name) {
                "U fs" -> return "U2 R2 D R' U2 R D' R' U2 R'"
                "U bs" -> return "R2' D' R U2 R' D R U2 R"
                "U fr" -> return "R' U' R U' R' U2 R2 U R' U R U2 R'"
                "U rows" -> return "U' F R2 D R' U R D' R2' U' F'"
                "U xc" -> return "U2 r U' r' U r' D' r U' r' D r"
                "U br" -> return "U' F R U R' U' F'"
                "T lb" -> return "U' R U R' U' R' F R F'"
                "T rb" -> return "U L' U' L U L F' L' F"
                "T rows" -> return "R U2 R' U' R U' R2' U2' R U R' U R"
                "T fr" -> return "r' U r U2' R2' F R F' R"
                "T br" -> return "r' D' r U r' D r U' r U r'"
                "T cols" -> return "U2 r2' D' r U r' D r2 U' r' U' r"
                "L mir" -> return "F R U' R' U' R U R' F'"
                "L inv" -> return "F R' F' R U R U' R'"
                "L pure" -> return "U2 R U R' U R U' R' U R U' R' U R U2' R'"
                "L fc" -> return "R U2 R D R' U2 R D' R2'"
                "L diag" -> return "U2 R' U' R U R' F' R U R' U' R' F R2"
                "L bc" -> return "U' R' U2 R' D' R U2 R' D R2"
                "S lb" -> return "R U R' U R U2 R'"
                "S xc" -> return "L' U2 L U2' L F' L' F"
                "S fs" -> return "F R' F' R U2 R U2' R'"
                "S cols" -> return "R U R' U' R' F R F' R U R' U R U2' R'"
                "S rb" -> return "U2' R U R' U R' F R F' R U2' R'"
                "S bs" -> return "R U' L' U R' U' L"
                "As rb" -> return "U' R U2' R' U' R U' R'"
                "As cols" -> return "R2 D R' U R D' R' U R' U' R U' R'"
                "As bs" -> return "F' r U r' U2' r' F2 r"
                "As xc" -> return "R U2' R' U2' R' F R F'"
                "As fs" -> return "L' U R U' L U R'"
                "As lb" -> return "U2' R U2' R' F R' F' R U' R U' R'"
                "H cols" -> return "U' R U R' U R U' R' U R U2 R'"
                "H rows" -> return "F R U R' U' R U R' U' R U R' U' F'"
                "H col" -> return "U' R U2' R2' F R F' U2 R' F R F'"
                "H row" -> return "r U' r2' D' r U' r' D r2 U r'"
                "Pi rb" -> return "F R U R' U' R U R' U' F'"
                "Pi bs" -> return "U F R' F' R U2 R U' R' U R U2' R'"
                "Pi xc" -> return "U' R' F R U F U' R U R' U' F'"
                "Pi fs" -> return "R U2 R' U' R U R' U2' R' F R F'"
                "Pi cols" -> return "U' r U' r2' D' r U r' D r2 U r'"
                "Pi lb" -> return "U' R' U' R' F R F' R U' R' U2 R"
                "O adj" -> return "R U R' F' R U R' U' R' F R2 U' R'"
                "O diag" -> return "F R U' R' U' R U R' F' R U R' U' R' F R F'"
            }

        }
        return ""
    }
}
