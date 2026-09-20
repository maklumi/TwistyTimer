package com.aricneto.twistytimer.utils

import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import com.aricneto.twistytimer.puzzle.NbyNCubePuzzle
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import org.worldcubeassociation.tnoodle.puzzle.*
import org.worldcubeassociation.tnoodle.scrambles.InvalidScrambleException
import org.worldcubeassociation.tnoodle.scrambles.Puzzle

/**
 * Util for generating and drawing scrambles
 */
class ScrambleGenerator(private val puzzleType: String) {
    var puzzle: Puzzle
        private set

    init {
        puzzle = when (puzzleType) {
            PuzzleUtils.TYPE_222 -> TwoByTwoCubePuzzle()
            PuzzleUtils.TYPE_333 -> ThreeByThreeCubePuzzle()
            PuzzleUtils.TYPE_444 -> FourByFourCubePuzzle()
            PuzzleUtils.TYPE_555 -> NbyNCubePuzzle(5)
            PuzzleUtils.TYPE_666 -> NbyNCubePuzzle(6)
            PuzzleUtils.TYPE_777 -> NbyNCubePuzzle(7)
            PuzzleUtils.TYPE_MEGA -> MegaminxPuzzle()
            PuzzleUtils.TYPE_PYRA -> PyraminxPuzzle()
            PuzzleUtils.TYPE_SKEWB -> SkewbPuzzle()
            PuzzleUtils.TYPE_CLOCK -> ClockPuzzle()
            PuzzleUtils.TYPE_SQUARE1 -> SquareOnePuzzle()
            else -> ThreeByThreeCubePuzzle()
        }
    }

    /**
     * Returns a scramble drawable showing the puzzled scrambled
     * Uses Tnoodle lib
     *
     */
    fun generateImageFromScramble(sp: SharedPreferences, scramble: String?): Drawable? {
        if (scramble == null || scramble.startsWith("To start training")) {
            return null
        }
        // Getting the color scheme
        val top: String
        val left: String
        val front: String
        val right: String
        val back: String
        val down: String
        // Due to a bug in the TNoodle library, the default Skewb scheme has the faces in a different order,
        // so we must account for this by creating a special case with some default colors flipped
        if (puzzleType != PuzzleUtils.TYPE_SKEWB) {
            top = sp.getString("cubeTop", "FFFFFF") ?: "FFFFFF"
            left = sp.getString("cubeLeft", "FF8B24") ?: "FF8B24"
            front = sp.getString("cubeFront", "02D040") ?: "02D040"
            right = sp.getString("cubeRight", "EC0000") ?: "EC0000"
            back = sp.getString("cubeBack", "304FFE") ?: "304FFE"
            down = sp.getString("cubeDown", "FDD835") ?: "FDD835"
        } else {
            top = sp.getString("cubeTop", "FFFFFF") ?: "FFFFFF"
            left = sp.getString("cubeFront", "02D040") ?: "02D040"
            front = sp.getString("cubeRight", "EC0000") ?: "EC0000"
            right = sp.getString("cubeBack", "304FFE") ?: "304FFE"
            back = sp.getString("cubeLeft", "EF6C00") ?: "EF6C00"
            down = sp.getString("cubeDown", "FDD835") ?: "FDD835"
        }
        var cubeImg: String? = null
        var pic: Drawable? = null
        try {
            cubeImg = puzzle.drawScramble(
                normalizeScramble(scramble),
                puzzle.parseColorScheme("$back,$down,$front,$left,$right,$top")
            )?.toString()
        } catch (e: InvalidScrambleException) {
            e.printStackTrace()
        }
        
        cubeImg?.let {
            try {
                pic = PictureDrawable(SVG.getFromString(it).renderToPicture())
            } catch (e: SVGParseException) {
                e.printStackTrace()
            }
        }
        return pic
    }

    /**
     * Normalizes a scramble by converting Roux-style lowercase moves and middle-slice moves
     * into standard WCA-style wide moves or equivalent move sequences that TNoodle can process for 3x3.
     */
    private fun normalizeScramble(scramble: String?): String? {
        if (scramble == null) return null
        
        // Normalize for any puzzle that uses 3x3 base (including trainer subsets)
        val isThreeByThree = puzzleType == PuzzleUtils.TYPE_333 || 
                           puzzleType == "OLL" || 
                           puzzleType == "PLL" || 
                           puzzleType == "CMLL"
        
        if (!isThreeByThree) return scramble

        val moves = scramble.split("\\s+".toRegex())
        val normalizedMoves = moves.map { move ->
            when {
                // Lowercase wide moves to WCA wide moves (e.g., r -> Rw, r' -> Rw')
                move.startsWith("r") -> move.replaceFirst("r", "Rw")
                move.startsWith("l") -> move.replaceFirst("l", "Lw")
                move.startsWith("f") -> move.replaceFirst("f", "Fw")
                move.startsWith("b") -> move.replaceFirst("b", "Bw")
                move.startsWith("u") -> move.replaceFirst("u", "Uw")
                move.startsWith("d") -> move.replaceFirst("d", "Dw")

                // Middle slice moves to wide + single move combinations
                move == "M" -> "Rw' R"
                move == "M'" -> "Rw R'"
                move == "M2" -> "Rw2 R2"
                move == "S" -> "Fw F'"
                move == "S'" -> "Fw' F"
                move == "S2" -> "Fw2 F2"
                move == "E" -> "Dw D'"
                move == "E'" -> "Dw' D"
                move == "E2" -> "Dw2 D2"

                else -> move
            }
        }
        return normalizedMoves.joinToString(" ")
    }
}
