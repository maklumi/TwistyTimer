package com.aricneto.twistytimer.solver

object RubiksCubeSecondBlockSolver {

    private val moveNames: Array<String> = arrayOf(
        "U", "U2", "U'",
        "D", "D2", "D'",
        "L", "L2", "L'",
        "R", "R2", "R'",
        "F", "F2", "F'",
        "B", "B2", "B'"
    )

    private val moves: Array<RubiksCubeSolver.State> = Array(moveNames.size) { i ->
        requireNotNull(RubiksCubeSolver.State.moves[moveNames[i]]) { "Move ${moveNames[i]} not found" }
    }

    // Goal piece indices for Left First Block (DL 1x2x3 slot) in Kociemba's State mapping:
    // Edges: BL (0), FL (3), DL (11)
    // Corners: DBL (4), DFL (7)
    private val fbLeftEdges = intArrayOf(0, 3, 11)
    private val fbLeftCorners = intArrayOf(4, 7)

    // Goal piece indices for Right Second Block (DR 1x2x3 slot) in Kociemba's State mapping:
    // Edges: BR (1), FR (2), DR (9)
    // Corners: DBR (5), DFR (6)
    private val sbRightEdges = intArrayOf(1, 2, 9)
    private val sbRightCorners = intArrayOf(5, 6)

    fun isFirstBlockSolved(state: RubiksCubeSolver.State): Boolean {
        for (edge in fbLeftEdges) {
            if (state.edgesPermutation[edge].toInt() != edge || state.edgesOrientation[edge].toInt() != 0) return false
        }
        for (corner in fbLeftCorners) {
            if (state.cornersPermutation[corner].toInt() != corner || state.cornersOrientation[corner].toInt() != 0) return false
        }
        return true
    }

    fun isSecondBlockSolved(state: RubiksCubeSolver.State): Boolean {
        for (edge in sbRightEdges) {
            if (state.edgesPermutation[edge].toInt() != edge || state.edgesOrientation[edge].toInt() != 0) return false
        }
        for (corner in sbRightCorners) {
            if (state.cornersPermutation[corner].toInt() != corner || state.cornersOrientation[corner].toInt() != 0) return false
        }
        return true
    }

    fun solve(state: RubiksCubeSolver.State): ArrayList<Array<String>> {
        val solutions = ArrayList<Array<String>>()

        val preserveFB = isFirstBlockSolved(state)

        var depth = 0
        while (depth <= 10) {
            val path = IntArray(depth)
            search(
                state = state,
                depth = depth,
                lastMoveIndex = -1,
                path = path,
                solutions = solutions,
                preserveFB = preserveFB
            )

            if (solutions.isNotEmpty()) {
                return solutions
            }
            depth++
        }

        return solutions
    }

    private fun search(
        state: RubiksCubeSolver.State,
        depth: Int,
        lastMoveIndex: Int,
        path: IntArray,
        solutions: ArrayList<Array<String>>,
        preserveFB: Boolean
    ) {
        if (solutions.size >= 3) return

        if (depth == 0) {
            val fbOk = !preserveFB || isFirstBlockSolved(state)
            if (fbOk && isSecondBlockSolved(state)) {
                val sequence = Array(path.size) { i -> moveNames[path[i]] }
                solutions.add(sequence)
            }
            return
        }

        // Heuristic distance pruning
        var unsolvedPieces = 0
        for (edge in sbRightEdges) {
            if (state.edgesPermutation[edge].toInt() != edge || state.edgesOrientation[edge].toInt() != 0) unsolvedPieces++
        }
        for (corner in sbRightCorners) {
            if (state.cornersPermutation[corner].toInt() != corner || state.cornersOrientation[corner].toInt() != 0) unsolvedPieces++
        }
        if (preserveFB) {
            for (edge in fbLeftEdges) {
                if (state.edgesPermutation[edge].toInt() != edge || state.edgesOrientation[edge].toInt() != 0) unsolvedPieces++
            }
            for (corner in fbLeftCorners) {
                if (state.cornersPermutation[corner].toInt() != corner || state.cornersOrientation[corner].toInt() != 0) unsolvedPieces++
            }
        }
        if ((unsolvedPieces + 1) / 2 > depth) return

        for (i in moves.indices) {
            if (lastMoveIndex != -1 && (i / 3) == (lastMoveIndex / 3)) {
                continue
            }

            path[path.size - depth] = i
            search(
                state = state.multiply(moves[i]),
                depth = depth - 1,
                lastMoveIndex = i,
                path = path,
                solutions = solutions,
                preserveFB = preserveFB
            )
        }
    }
}
