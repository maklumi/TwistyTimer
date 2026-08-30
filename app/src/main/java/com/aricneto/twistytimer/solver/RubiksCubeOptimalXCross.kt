package com.aricneto.twistytimer.solver

import com.aricneto.twistify.R
import com.aricneto.twistytimer.solver.RubiksCubeXCrossSolver.solve
import com.aricneto.twistytimer.utils.DefaultPrefs.getBoolean
import com.aricneto.twistytimer.utils.Prefs.getBoolean

class RubiksCubeOptimalXCross(private val description: String) : Tip {
    override val tipId: String  = "RUBIKS-CUBE-OPTIMAL-X-CROSS"


    override val puzzleId: String =  "RUBIKS-CUBE"

    override val tipDescription: String = "RUBIKS-CUBE-OPTIMAL-X-CROSS"

    override fun getTip(scramble: String): String {
        val state = RubiksCubeSolver.State.id.applySequence(
            scramble.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )

        val tip = StringBuilder()


        // x-cross on U
        if (getBoolean(
                R.string.pk_cross_hint_top_enabled,
                getBoolean(R.bool.default_crossHintTopEnabled)
            )
        ) {
            val stateU: RubiksCubeSolver.State = x.multiply(x).multiply(state).multiply(x).multiply(
                x
            )
            tip.append(String.format(description, "U")).append("\n")
            tip.append(getOptimalSolutions(stateU, "x2 "))
            tip.append("\n")
        }

        // x-cross on D
        if (getBoolean(
                R.string.pk_cross_hint_down_enabled,
                getBoolean(R.bool.default_crossHintDownEnabled)
            )
        ) {
            tip.append(String.format(description, "D")).append("\n")
            tip.append(getOptimalSolutions(state, "")) // state == "stateD"
            tip.append("\n")
        }

        // x-cross on L
        if (getBoolean(
                R.string.pk_cross_hint_left_enabled,
                getBoolean(R.bool.default_crossHintLeftEnabled)
            )
        ) {
            val stateL: RubiksCubeSolver.State = z.multiply(state).multiply(z).multiply(z).multiply(
                z
            )
            tip.append(String.format(description, "L")).append("\n")
            tip.append(getOptimalSolutions(stateL, "z' "))
            tip.append("\n")
        }

        // x-cross on R
        if (getBoolean(
                R.string.pk_cross_hint_right_enabled,
                getBoolean(R.bool.default_crossHintRightEnabled)
            )
        ) {
            val stateR: RubiksCubeSolver.State = z.multiply(z).multiply(z).multiply(state).multiply(
                z
            )
            tip.append(String.format(description, "R")).append("\n")
            tip.append(getOptimalSolutions(stateR, "z "))
            tip.append("\n")
        }

        // x-cross on F
        if (getBoolean(
                R.string.pk_cross_hint_front_enabled,
                getBoolean(R.bool.default_crossHintFrontEnabled)
            )
        ) {
            val stateF: RubiksCubeSolver.State = x.multiply(state).multiply(x).multiply(x).multiply(
                x
            )
            tip.append(String.format(description, "F")).append("\n")
            tip.append(getOptimalSolutions(stateF, "x' "))
            tip.append("\n")
        }

        // x-cross on B
        if (getBoolean(
                R.string.pk_cross_hint_back_enabled,
                getBoolean(R.bool.default_crossHintBackEnabled)
            )
        ) {
            val stateB: RubiksCubeSolver.State = x.multiply(x).multiply(x).multiply(state).multiply(
                x
            )
            tip.append(String.format(description, "B")).append("\n")
            tip.append(getOptimalSolutions(stateB, "x "))
            tip.append("\n")
        }

        return tip.toString().trim { it <= ' ' }
    }

    private fun getOptimalSolutions(state: RubiksCubeSolver.State, prefix: String): String {
        var count = 0

        val prefixes = ArrayList<String>()
        val solutions = ArrayList<Array<String>>()

        // id
        for (solution in solve(state)) {
            prefixes.add(prefix)
            solutions.add(solution)
            count++
            if (count == 2) {
                break
            }
        }

        // y
        count = 0
        val stateY: RubiksCubeSolver.State = y.multiply(y).multiply(y).multiply(state).multiply(y)
        for (solution in solve(stateY)) {
            prefixes.add(prefix + "y ")
            solutions.add(solution)
            count++
            if (count == 2) {
                break
            }
        }

        // y2
        count = 0
        val stateY2: RubiksCubeSolver.State = y.multiply(y).multiply(state).multiply(y).multiply(y)
        for (solution in solve(stateY2)) {
            prefixes.add(prefix + "y2 ")
            solutions.add(solution)
            count++
            if (count == 2) {
                break
            }
        }

        // y'
        count = 0
        val stateY3: RubiksCubeSolver.State = y.multiply(state).multiply(y).multiply(y).multiply(y)
        for (solution in solve(stateY3)) {
            prefixes.add(prefix + "y' ")
            solutions.add(solution)
            count++
            if (count == 2) {
                break
            }
        }

        var minLength = Int.MAX_VALUE
        for (solution in solutions) {
            if (solution.size < minLength) {
                minLength = solution.size
            }
        }

        val output = StringBuilder()
        for (i in solutions.indices) {
            if (solutions[i].size == minLength) {
                output.append("  ")
                    .append(prefixes[i])
                    .append(StringUtils.join(" ", solutions[i]))
                    .append('\n')
            }
        }

        return output.toString()
    }

    override fun toString(): String {
        return tipDescription
    }

    companion object {
        private val x: RubiksCubeSolver.State
        private val y: RubiksCubeSolver.State
        private val z: RubiksCubeSolver.State

        init {
            x = RubiksCubeSolver.State(
                byteArrayOf(3, 2, 6, 7, 0, 1, 5, 4),
                byteArrayOf(2, 1, 2, 1, 1, 2, 1, 2),
                byteArrayOf(7, 5, 9, 11, 6, 2, 10, 3, 4, 1, 8, 0),
                byteArrayOf(0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0)
            )

            y = RubiksCubeSolver.State(
                byteArrayOf(3, 0, 1, 2, 7, 4, 5, 6),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
                byteArrayOf(3, 0, 1, 2, 7, 4, 5, 6, 11, 8, 9, 10),
                byteArrayOf(1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0)
            )

            z = RubiksCubeSolver.State(
                byteArrayOf(4, 0, 3, 7, 5, 1, 2, 6),
                byteArrayOf(1, 2, 1, 2, 2, 1, 2, 1),
                byteArrayOf(8, 4, 6, 10, 0, 7, 3, 11, 1, 5, 2, 9),
                byteArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
            )
        }
    }
}
