package com.aricneto.twistytimer.solver

import com.aricneto.twistytimer.solver.StringUtils.join

class RubiksCubeOptimalSecondBlock(private val description: String) : Tip {
    override val tipId: String = "RUBIKS-CUBE-OPTIMAL-SECOND-BLOCK"

    override val puzzleId: String = "RUBIKS-CUBE"

    override val tipDescription: String = "Roux Second Block (1x2x3)"

    override fun getTip(scramble: String): String {
        val moves = scramble.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val state = RubiksCubeSolver.State.id.applySequence(moves)

        val tip = StringBuilder()
        tip.append(description).append("\n")

        val solutions = RubiksCubeSecondBlockSolver.solve(state)
        if (solutions.isEmpty()) {
            tip.append("  No short solutions found.\n")
        } else {
            for (solution in solutions) {
                tip.append("  ").append(join(" ", solution)).append('\n')
            }
        }

        return tip.toString().trim { it <= ' ' }
    }

    override fun toString(): String {
        return tipDescription
    }
}
