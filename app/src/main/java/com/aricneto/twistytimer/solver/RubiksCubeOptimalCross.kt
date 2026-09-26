package com.aricneto.twistytimer.solver

import com.aricneto.twistify.R
import com.aricneto.twistytimer.solver.StringUtils.join
import com.aricneto.twistytimer.utils.DefaultPrefs.getBoolean
import com.aricneto.twistytimer.utils.Prefs.getBoolean

class RubiksCubeOptimalCross(private val description: String) : Tip {
    override val tipId: String = "RUBIKS-CUBE-OPTIMAL-CROSS"

    override val puzzleId: String = "RUBIKS-CUBE"

    override val tipDescription: String = "RUBIKS-CUBE-OPTIMAL-CROSS"

    override fun getTip(scramble: String): String {
        val maxCount = 3
        var count: Int
        val state = RubiksCubeSolver.State.id.applySequence(
            scramble.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )

        val tip = StringBuilder()

        // cross on U
        if (getBoolean(
                R.string.pk_cross_hint_top_enabled,
                getBoolean(R.bool.default_crossHintTopEnabled)
            )
        ) {
            count = 0 // limit number of algs
            val stateU: RubiksCubeSolver.State =
                x.multiply(x).multiply(state).multiply(x).multiply(x)
            tip.append(String.format(description, "U")).append("\n")
            for (solution in RubiksCubeCrossSolver.solve(stateU)) {
                tip.append("  x2 ").append(join(" ", solution)).append('\n')
                count++
                if (count == maxCount) break
            }
            tip.append("\n")
        }

        // cross on D
        if (getBoolean(
                R.string.pk_cross_hint_down_enabled,
                getBoolean(R.bool.default_crossHintDownEnabled)
            )
        ) {
            count = 0
            tip.append(String.format(description, "D")).append("\n")
            for (solution in RubiksCubeCrossSolver.solve(state)) {
                tip.append("  ").append(join(" ", solution)).append('\n')
                count++
                if (count == maxCount) break
            }
            tip.append("\n")
        }

        // cross on L
        if (getBoolean(
                R.string.pk_cross_hint_left_enabled,
                getBoolean(R.bool.default_crossHintLeftEnabled)
            )
        ) {
            count = 0
            val stateL: RubiksCubeSolver.State =
                z.multiply(state).multiply(z).multiply(z).multiply(z)
            tip.append(String.format(description, "L")).append("\n")
            for (solution in RubiksCubeCrossSolver.solve(stateL)) {
                tip.append("  z' ").append(join(" ", solution)).append('\n')
                count++
                if (count == maxCount) break
            }
            tip.append("\n")
        }

        // cross on R
        if (getBoolean(
                R.string.pk_cross_hint_right_enabled,
                getBoolean(R.bool.default_crossHintRightEnabled)
            )
        ) {
            count = 0
            val stateR: RubiksCubeSolver.State =
                z.multiply(z).multiply(z).multiply(state).multiply(z)
            tip.append(String.format(description, "R")).append("\n")
            for (solution in RubiksCubeCrossSolver.solve(stateR)) {
                tip.append("  z ").append(join(" ", solution)).append('\n')
                count++
                if (count == maxCount) break
            }
            tip.append("\n")
        }

        // cross on F
        if (getBoolean(
                R.string.pk_cross_hint_front_enabled,
                getBoolean(R.bool.default_crossHintFrontEnabled)
            )
        ) {
            count = 0
            val stateF: RubiksCubeSolver.State =
                x.multiply(state).multiply(x).multiply(x).multiply(x)
            tip.append(String.format(description, "F")).append("\n")
            for (solution in RubiksCubeCrossSolver.solve(stateF)) {
                tip.append("  x' ").append(join(" ", solution)).append('\n')
                count++
                if (count == maxCount) break
            }
            tip.append("\n")
        }

        // cross on B
        if (getBoolean(
                R.string.pk_cross_hint_back_enabled,
                getBoolean(R.bool.default_crossHintBackEnabled)
            )
        ) {
            count = 0
            val stateB: RubiksCubeSolver.State =
                x.multiply(x).multiply(x).multiply(state).multiply(x)
            tip.append(String.format(description, "B")).append("\n")
            for (solution in RubiksCubeCrossSolver.solve(stateB)) {
                tip.append("  x ").append(join(" ", solution)).append('\n')
                count++
                if (count == maxCount) break
            }
            tip.append("\n")
        }

        return tip.toString().trim { it <= ' ' }
    }

    override fun toString(): String {
        return tipDescription
    }

    companion object {
        private val x: RubiksCubeSolver.State = RubiksCubeSolver.State(
            byteArrayOf(3, 2, 6, 7, 0, 1, 5, 4),
            byteArrayOf(2, 1, 2, 1, 1, 2, 1, 2),
            byteArrayOf(7, 5, 9, 11, 6, 2, 10, 3, 4, 1, 8, 0),
            byteArrayOf(0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0)
        )
        private val z: RubiksCubeSolver.State = RubiksCubeSolver.State(
            byteArrayOf(4, 0, 3, 7, 5, 1, 2, 6),
            byteArrayOf(1, 2, 1, 2, 2, 1, 2, 1),
            byteArrayOf(8, 4, 6, 10, 0, 7, 3, 11, 1, 5, 2, 9),
            byteArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
        )
    }
}

