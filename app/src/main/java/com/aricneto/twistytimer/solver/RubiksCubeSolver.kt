package com.aricneto.twistytimer.solver

import com.aricneto.twistytimer.solver.IndexMapping.combinationToIndex
import com.aricneto.twistytimer.solver.IndexMapping.indexToCombination
import com.aricneto.twistytimer.solver.IndexMapping.indexToOrientation
import com.aricneto.twistytimer.solver.IndexMapping.indexToPermutation
import com.aricneto.twistytimer.solver.IndexMapping.indexToZeroSumOrientation
import com.aricneto.twistytimer.solver.IndexMapping.permutationToIndex
import com.aricneto.twistytimer.solver.IndexMapping.zeroSumOrientationToIndex
import kotlin.math.min

// kociemba's two phase algorithm
// references: http://kociemba.org/cube.htm
//             http://www.jaapsch.net/puzzles/compcube.htm

object RubiksCubeSolver {
    // constants
    const val N_CORNERS_ORIENTATIONS: Int = 2187
    const val N_EDGES_ORIENTATIONS: Int = 2048
    const val N_E_EDGES_COMBINATIONS: Int = 495
    const val N_CORNERS_PERMUTATIONS: Int = 40320
    const val N_U_D_EDGES_PERMUTATIONS: Int = 40320
    const val N_E_EDGES_PERMUTATIONS: Int = 24
    const val N_EDGES_PERMUTATIONS: Int = 479001600

    // moves
    private val moveNames1: Array<String>
    private var moves1: Array<State>
    private val sides1: IntArray
    private val axes1: IntArray
    private val moveNames2: Array<String>
    private var moves2: Array<State>
    private val sides2: IntArray
    private val axes2: IntArray

    init {
        // phase 1
        moveNames1 = arrayOf<String>(
            "U", "U2", "U'",
            "D", "D2", "D'",
            "L", "L2", "L'",
            "R", "R2", "R'",
            "F", "F2", "F'",
            "B", "B2", "B'",
        )

        moves1 = Array(moveNames1.size) { i ->
            requireNotNull(State.moves[moveNames1[i]]) { "Move ${moveNames1[i]} not found" }
        }

        sides1 = intArrayOf(
            0, 0, 0,
            1, 1, 1,
            2, 2, 2,
            3, 3, 3,
            4, 4, 4,
            5, 5, 5,
        )

        axes1 = intArrayOf(
            0, 0, 0,
            0, 0, 0,
            1, 1, 1,
            1, 1, 1,
            2, 2, 2,
            2, 2, 2,
        )

        // phase 2
        moveNames2 = arrayOf<String>(
            "U", "U2", "U'",
            "D", "D2", "D'",
            "L2",
            "R2",
            "F2",
            "B2",
        )

        moves2 = Array(moveNames2.size) { i ->
            requireNotNull(State.moves[moveNames2[i]]) { "Move ${moveNames2[i]} not found" }
        }

        sides2 = intArrayOf(
            0, 0, 0,
            1, 1, 1,
            2,
            3,
            4,
            5,
        )

        axes2 = intArrayOf(
            0, 0, 0,
            0, 0, 0,
            1,
            1,
            2,
            2,
        )
    }

    // move tables
    private var cornersOrientationMove: Array<IntArray>
    private var edgesOrientationMove: Array<IntArray>
    private var eEdgesCombinationMove: Array<IntArray>
    private var cornersPermutationMove: Array<IntArray>
    private var uDEdgesPermutationMove: Array<IntArray>
    private var eEdgesPermutationMove: Array<IntArray>

    init {
        // phase 1
        cornersOrientationMove = Array<IntArray>(N_CORNERS_ORIENTATIONS) { IntArray(moves1.size) }
        for (i in 0..<N_CORNERS_ORIENTATIONS) {
            val state = State(
                ByteArray(8),
                indexToZeroSumOrientation(i, 3, 8),
                ByteArray(12),
                ByteArray(12)
            )
            for (j in moves1.indices) {
                cornersOrientationMove[i][j] =
                    zeroSumOrientationToIndex(state.multiply(moves1[j]).cornersOrientation, 3)
            }
        }


        edgesOrientationMove = Array<IntArray>(N_EDGES_ORIENTATIONS) { IntArray(moves1.size) }
        for (i in 0..<N_EDGES_ORIENTATIONS) {
            val state = State(
                ByteArray(8),
                ByteArray(8),
                ByteArray(12),
                indexToZeroSumOrientation(i, 2, 12)
            )
            for (j in moves1.indices) {
                edgesOrientationMove[i][j] =
                    zeroSumOrientationToIndex(state.multiply(moves1[j]).edgesOrientation, 2)
            }
        }


        eEdgesCombinationMove = Array<IntArray>(N_E_EDGES_COMBINATIONS) { IntArray(moves1.size) }
        for (i in 0..<N_E_EDGES_COMBINATIONS) {
            val combination = indexToCombination(i, 4, 12)

            val edges = ByteArray(12)
            var nextE: Byte = 0
            var nextUD: Byte = 4

            for (j in edges.indices) {
                if (combination[j]) {
                    edges[j] = nextE++
                } else {
                    edges[j] = nextUD++
                }
            }

            val state = State(ByteArray(8), ByteArray(8), edges, ByteArray(12))
            for (j in moves1.indices) {
                val result = state.multiply(moves1[j])

                val isEEdge = BooleanArray(12)
                for (k in isEEdge.indices) {
                    isEEdge[k] = result.edgesPermutation[k] < 4
                }

                eEdgesCombinationMove[i][j] = combinationToIndex(isEEdge, 4)
            }
        }


        // phase 2
        cornersPermutationMove = Array<IntArray>(N_CORNERS_PERMUTATIONS) { IntArray(moves2.size) }
        for (i in 0..<N_CORNERS_PERMUTATIONS) {
            val state = State(indexToPermutation(i, 8), ByteArray(8), ByteArray(12), ByteArray(12))
            for (j in moves2.indices) {
                cornersPermutationMove[i][j] =
                    permutationToIndex(state.multiply(moves2[j]).cornersPermutation)
            }
        }


        uDEdgesPermutationMove =
            Array<IntArray>(N_U_D_EDGES_PERMUTATIONS) { IntArray(moves2.size) }
        for (i in 0..<N_U_D_EDGES_PERMUTATIONS) {
            val permutation = indexToPermutation(i, 8)

            val edges = ByteArray(12)
            for (j in edges.indices) {
                edges[j] = if (j >= 4) permutation[j - 4] else j.toByte()
            }

            val state = State(ByteArray(8), ByteArray(8), edges, ByteArray(12))
            for (j in moves2.indices) {
                val result = state.multiply(moves2[j])

                val uDEdges = ByteArray(8)
                for (k in uDEdges.indices) {
                    uDEdges[k] = (result.edgesPermutation[k + 4] - 4).toByte()
                }

                uDEdgesPermutationMove[i][j] = permutationToIndex(uDEdges)
            }
        }


        eEdgesPermutationMove = Array<IntArray>(N_E_EDGES_PERMUTATIONS) { IntArray(moves2.size) }
        for (i in 0..<N_E_EDGES_PERMUTATIONS) {
            val permutation = indexToPermutation(i, 4)

            val edges = ByteArray(12)
            for (j in edges.indices) {
                edges[j] = if (j >= 4) j.toByte() else permutation[j]
            }

            val state = State(ByteArray(8), ByteArray(8), edges, ByteArray(12))
            for (j in moves2.indices) {
                val result = state.multiply(moves2[j])

                val eEdges = result.edgesPermutation.copyOf(4)

                eEdgesPermutationMove[i][j] = permutationToIndex(eEdges)
            }
        }
    }

    // prune tables
    private var cornersOrientationDistance: Array<ByteArray>
    private var edgesOrientationDistance: Array<ByteArray>
    private var cornersPermutationDistance: Array<ByteArray>
    private var uDEdgesPermutationDistance: Array<ByteArray>

    init {
        // phase 1
        cornersOrientationDistance = Array<ByteArray>(N_CORNERS_ORIENTATIONS) {
            ByteArray(
                N_E_EDGES_COMBINATIONS
            )
        }
        for (i in cornersOrientationDistance.indices) {
            for (j in cornersOrientationDistance[i].indices) {
                cornersOrientationDistance[i][j] = -1
            }
        }
        cornersOrientationDistance[0][0] = 0

        var distance = 0
        var nVisited = 1
        while (nVisited < N_CORNERS_ORIENTATIONS * N_E_EDGES_COMBINATIONS) {
            for (i in 0..<N_CORNERS_ORIENTATIONS) {
                for (j in cornersOrientationDistance[i].indices) {
                    if (cornersOrientationDistance[i][j].toInt() == distance) {
                        for (k in moves1.indices) {
                            val nextCornersOrientation = cornersOrientationMove[i][k]
                            val nextEEdgesCombination = eEdgesCombinationMove[j][k]
                            if (cornersOrientationDistance[nextCornersOrientation][nextEEdgesCombination] < 0) {
                                cornersOrientationDistance[nextCornersOrientation][nextEEdgesCombination] =
                                    (distance + 1).toByte()
                                nVisited++
                            }
                        }
                    }
                }
            }
            distance++
        }


        edgesOrientationDistance = Array<ByteArray>(N_EDGES_ORIENTATIONS) {
            ByteArray(
                N_E_EDGES_COMBINATIONS
            )
        }
        for (i in edgesOrientationDistance.indices) {
            for (j in edgesOrientationDistance[i].indices) {
                edgesOrientationDistance[i][j] = -1
            }
        }
        edgesOrientationDistance[0][0] = 0

        distance = 0
        nVisited = 1
        while (nVisited < N_EDGES_ORIENTATIONS * N_E_EDGES_COMBINATIONS) {
            for (i in 0..<N_EDGES_ORIENTATIONS) {
                for (j in 0..<N_E_EDGES_COMBINATIONS) {
                    if (edgesOrientationDistance[i][j].toInt() == distance) {
                        for (k in moves1.indices) {
                            val nextEdgesOrientation = edgesOrientationMove[i][k]
                            val nextEEdgesCombination = eEdgesCombinationMove[j][k]
                            if (edgesOrientationDistance[nextEdgesOrientation][nextEEdgesCombination] < 0) {
                                edgesOrientationDistance[nextEdgesOrientation][nextEEdgesCombination] =
                                    (distance + 1).toByte()
                                nVisited++
                            }
                        }
                    }
                }
            }
            distance++
        }


        // phase 2
        cornersPermutationDistance = Array<ByteArray>(N_CORNERS_PERMUTATIONS) {
            ByteArray(
                N_E_EDGES_PERMUTATIONS
            )
        }
        for (i in cornersPermutationDistance.indices) {
            for (j in cornersPermutationDistance[i].indices) {
                cornersPermutationDistance[i][j] = -1
            }
        }
        cornersPermutationDistance[0][0] = 0

        distance = 0
        nVisited = 1
        while (nVisited < N_CORNERS_PERMUTATIONS * N_E_EDGES_PERMUTATIONS) {
            for (i in 0..<N_CORNERS_PERMUTATIONS) {
                for (j in 0..<N_E_EDGES_PERMUTATIONS) {
                    if (cornersPermutationDistance[i][j].toInt() == distance) {
                        for (k in moves2.indices) {
                            val nextCornersPermutation = cornersPermutationMove[i][k]
                            val nextEEdgesPermutation = eEdgesPermutationMove[j][k]
                            if (cornersPermutationDistance[nextCornersPermutation][nextEEdgesPermutation] < 0) {
                                cornersPermutationDistance[nextCornersPermutation][nextEEdgesPermutation] =
                                    (distance + 1).toByte()
                                nVisited++
                            }
                        }
                    }
                }
            }
            distance++
        }


        uDEdgesPermutationDistance = Array<ByteArray>(N_U_D_EDGES_PERMUTATIONS) {
            ByteArray(
                N_E_EDGES_PERMUTATIONS
            )
        }
        for (i in uDEdgesPermutationDistance.indices) {
            for (j in uDEdgesPermutationDistance[i].indices) {
                uDEdgesPermutationDistance[i][j] = -1
            }
        }
        uDEdgesPermutationDistance[0][0] = 0

        distance = 0
        nVisited = 1
        while (nVisited < N_U_D_EDGES_PERMUTATIONS * N_E_EDGES_PERMUTATIONS) {
            for (i in 0..<N_U_D_EDGES_PERMUTATIONS) {
                for (j in 0..<N_E_EDGES_PERMUTATIONS) {
                    if (uDEdgesPermutationDistance[i][j].toInt() == distance) {
                        for (k in moves2.indices) {
                            val nextUDEdgesPermutation = uDEdgesPermutationMove[i][k]
                            val nextEEdgesPermutation = eEdgesPermutationMove[j][k]
                            if (uDEdgesPermutationDistance[nextUDEdgesPermutation][nextEEdgesPermutation] < 0) {
                                uDEdgesPermutationDistance[nextUDEdgesPermutation][nextEEdgesPermutation] =
                                    (distance + 1).toByte()
                                nVisited++
                            }
                        }
                    }
                }
            }
            distance++
        }
    }

    // search
    private const val MAX_SOLUTION_LENGTH = 23
    private const val MAX_PHASE_2_SOLUTION_LENGTH = 12

    private var initialState: State? = null
    private var solution1 = ArrayList<Int>()
    private var solution2 = ArrayList<Int>()

    private fun solution(state: State): Array<String> {
        initialState = state

        // corners orientation index
        val cornersOrientation = zeroSumOrientationToIndex(state.cornersOrientation, 3)

        // edges orientation index
        val edgesOrientation = zeroSumOrientationToIndex(state.edgesOrientation, 2)

        // e edges combination index
        val isEEdge = BooleanArray(12)
        for (i in isEEdge.indices) {
            isEEdge[i] = state.edgesPermutation[i] < 4
        }
        val eEdgesCombination = combinationToIndex(isEEdge, 4)

        var depth = 0
        while (true) {
            solution1 = ArrayList<Int>(MAX_SOLUTION_LENGTH)
            if (search1(cornersOrientation, edgesOrientation, eEdgesCombination, depth)) {
                val sequence = ArrayList<String>()
                for (moveIndex in solution1) {
                    sequence.add(moveNames1[moveIndex])
                }
                for (moveIndex in solution2) {
                    sequence.add(moveNames2[moveIndex])
                }

                return sequence.toTypedArray()
            }
            depth++
        }
    }

    private fun search1(
        cornersOrientation: Int,
        edgesOrientation: Int,
        eEdgesCombinations: Int,
        depth: Int
    ): Boolean {
        if (depth == 0) {
            if (cornersOrientation == 0 && edgesOrientation == 0 && eEdgesCombinations == 0) {
                var state = requireNotNull(initialState)
                for (moveIndex in solution1) {
                    state = state.multiply(moves1[moveIndex])
                }

                return solution2(state, MAX_SOLUTION_LENGTH - solution1.size)
            }

            return false
        }

        if (cornersOrientationDistance[cornersOrientation][eEdgesCombinations] <= depth &&
            edgesOrientationDistance[edgesOrientation][eEdgesCombinations] <= depth
        ) {
            val lastMoves = intArrayOf(-1, -1)
            run {
                var i = 0
                while (i < lastMoves.size && i < solution1.size) {
                    lastMoves[i] = solution1[solution1.size - 1 - i]
                    i++
                }
            }

            for (i in moves1.indices) {
                // same side
                if (lastMoves[0] >= 0 && sides1[i] == sides1[lastMoves[0]]) {
                    continue
                }

                // same axis three times in a row
                if (lastMoves[0] >= 0 && axes1[i] == axes1[lastMoves[0]] && lastMoves[1] >= 0 && axes1[i] == axes1[lastMoves[1]]) {
                    continue
                }

                solution1.add(i)
                if (search1(
                        cornersOrientationMove[cornersOrientation][i],
                        edgesOrientationMove[edgesOrientation][i],
                        eEdgesCombinationMove[eEdgesCombinations][i],
                        depth - 1
                    )
                ) {
                    return true
                }
                solution1.removeAt(solution1.size - 1)
            }
        }

        return false
    }

    private fun solution2(state: State, maxDepth: Int): Boolean {
        if (solution1.size > 0) {
            val lastMove = solution1[solution1.size - 1]
            for (i in moveNames2.indices) {
                if (moveNames1[lastMove] == moveNames2[i]) {
                    return false
                }
            }
        }

        // corners permutation index
        val cornersPermutation = permutationToIndex(state.cornersPermutation)

        // u and d edges permutation index
        val uDEdges = ByteArray(8)
        for (i in uDEdges.indices) {
            uDEdges[i] = (state.edgesPermutation[i + 4] - 4).toByte()
        }
        val uDEdgesPermutation = permutationToIndex(uDEdges)

        // e edges permutation index
        val eEdges = state.edgesPermutation.copyOf(4)
        val eEdgesPermutation = permutationToIndex(eEdges)

        for (depth in 0..<min(MAX_PHASE_2_SOLUTION_LENGTH, maxDepth)) {
            solution2 = ArrayList<Int>(MAX_SOLUTION_LENGTH)
            if (search2(cornersPermutation, uDEdgesPermutation, eEdgesPermutation, depth)) {
                return true
            }
        }

        return false
    }

    private fun search2(
        cornersPermutation: Int,
        uDEdgesPermutation: Int,
        eEdgesPermutation: Int,
        depth: Int
    ): Boolean {
        if (depth == 0) {
            return cornersPermutation == 0 && uDEdgesPermutation == 0 && eEdgesPermutation == 0
        }

        if (cornersPermutationDistance[cornersPermutation][eEdgesPermutation] <= depth &&
            uDEdgesPermutationDistance[uDEdgesPermutation][eEdgesPermutation] <= depth
        ) {
            var lastSide = Int.MAX_VALUE
            if (solution2.size > 0) {
                lastSide = sides2[solution2[solution2.size - 1]]
            }

            for (i in moves2.indices) {
                // avoid superfluous moves between phases
                if (solution2.size == 0) {
                    var lastPhase1Axis = Int.MAX_VALUE
                    if (solution1.size > 0) {
                        lastPhase1Axis = axes1[solution1[solution1.size - 1]]
                    }

                    if (axes2[i] == lastPhase1Axis) {
                        continue
                    }
                }

                // same side
                if (sides2[i] == lastSide) {
                    continue
                }

                solution2.add(i)
                if (search2(
                        cornersPermutationMove[cornersPermutation][i],
                        uDEdgesPermutationMove[uDEdgesPermutation][i],
                        eEdgesPermutationMove[eEdgesPermutation][i],
                        depth - 1
                    )
                ) {
                    return true
                }
                solution2.removeAt(solution2.size - 1)
            }
        }

        return false
    }

    fun generate(state: State): Array<String> {
        val solution = solution(state)

        val inverseMoveNames = HashMap<String, String>()
        inverseMoveNames["U"] = "U'"
        inverseMoveNames["U2"] = "U2"
        inverseMoveNames["U'"] = "U"
        inverseMoveNames["D"] = "D'"
        inverseMoveNames["D2"] = "D2"
        inverseMoveNames["D'"] = "D"
        inverseMoveNames["L"] = "L'"
        inverseMoveNames["L2"] = "L2"
        inverseMoveNames["L'"] = "L"
        inverseMoveNames["R"] = "R'"
        inverseMoveNames["R2"] = "R2"
        inverseMoveNames["R'"] = "R"
        inverseMoveNames["F"] = "F'"
        inverseMoveNames["F2"] = "F2"
        inverseMoveNames["F'"] = "F"
        inverseMoveNames["B"] = "B'"
        inverseMoveNames["B2"] = "B2"
        inverseMoveNames["B'"] = "B"

        return Array(solution.size) { i ->
            requireNotNull(inverseMoveNames[solution[solution.size - i - 1]])
        }
    }

    class State(
        var cornersPermutation: ByteArray,
        var cornersOrientation: ByteArray,
        var edgesPermutation: ByteArray,
        var edgesOrientation: ByteArray
    ) {
        fun multiply(move: State): State {
            // corners
            val cornersPermutation = ByteArray(8)
            val cornersOrientation = ByteArray(8)

            for (i in 0..7) {
                cornersPermutation[i] = this.cornersPermutation[move.cornersPermutation[i].toInt()]
                cornersOrientation[i] =
                    ((this.cornersOrientation[move.cornersPermutation[i].toInt()] + move.cornersOrientation[i]) % 3).toByte()
            }

            // edges
            val edgesPermutation = ByteArray(12)
            val edgesOrientation = ByteArray(12)

            for (i in 0..11) {
                edgesPermutation[i] = this.edgesPermutation[move.edgesPermutation[i].toInt()]
                edgesOrientation[i] =
                    ((this.edgesOrientation[move.edgesPermutation[i].toInt()] + move.edgesOrientation[i]) % 2).toByte()
            }

            return State(cornersPermutation, cornersOrientation, edgesPermutation, edgesOrientation)
        }

        fun applySequence(sequence: Array<String>): State {
            var state = this
            for (move in sequence) {
                state = state.multiply(requireNotNull(moves[move]))
            }

            return state
        }

        companion object {
            var moves: HashMap<String, State> = HashMap()

            init {
                val moveU = State(
                    byteArrayOf(3, 0, 1, 2, 4, 5, 6, 7),
                    byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
                    byteArrayOf(0, 1, 2, 3, 7, 4, 5, 6, 8, 9, 10, 11),
                    byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                )
                val moveD = State(
                    byteArrayOf(0, 1, 2, 3, 5, 6, 7, 4),
                    byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
                    byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 8),
                    byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                )
                val moveL = State(
                    byteArrayOf(4, 1, 2, 0, 7, 5, 6, 3),
                    byteArrayOf(2, 0, 0, 1, 1, 0, 0, 2),
                    byteArrayOf(11, 1, 2, 7, 4, 5, 6, 0, 8, 9, 10, 3),
                    byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                )
                val moveR = State(
                    byteArrayOf(0, 2, 6, 3, 4, 1, 5, 7),
                    byteArrayOf(0, 1, 2, 0, 0, 2, 1, 0),
                    byteArrayOf(0, 5, 9, 3, 4, 2, 6, 7, 8, 1, 10, 11),
                    byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                )
                val moveF = State(
                    byteArrayOf(0, 1, 3, 7, 4, 5, 2, 6),
                    byteArrayOf(0, 0, 1, 2, 0, 0, 2, 1),
                    byteArrayOf(0, 1, 6, 10, 4, 5, 3, 7, 8, 9, 2, 11),
                    byteArrayOf(0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0)
                )
                val moveB = State(
                    byteArrayOf(1, 5, 2, 3, 0, 4, 6, 7),
                    byteArrayOf(1, 2, 0, 0, 2, 1, 0, 0),
                    byteArrayOf(4, 8, 2, 3, 1, 5, 6, 7, 0, 9, 10, 11),
                    byteArrayOf(1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0)
                )

                moves = HashMap<String, State>()
                moves.put("U", moveU)
                moves.put("U2", moveU.multiply(moveU))
                moves.put("U'", moveU.multiply(moveU).multiply(moveU))
                moves.put("D", moveD)
                moves.put("D2", moveD.multiply(moveD))
                moves.put("D'", moveD.multiply(moveD).multiply(moveD))
                moves.put("L", moveL)
                moves.put("L2", moveL.multiply(moveL))
                moves.put("L'", moveL.multiply(moveL).multiply(moveL))
                moves.put("R", moveR)
                moves.put("R2", moveR.multiply(moveR))
                moves.put("R'", moveR.multiply(moveR).multiply(moveR))
                moves.put("F", moveF)
                moves.put("F2", moveF.multiply(moveF))
                moves.put("F'", moveF.multiply(moveF).multiply(moveF))
                moves.put("B", moveB)
                moves.put("B2", moveB.multiply(moveB))
                moves.put("B'", moveB.multiply(moveB).multiply(moveB))
            }

            var id: State = State(
                indexToPermutation(0, 8),
                indexToOrientation(0, 3, 8),
                indexToPermutation(0, 12),
                indexToOrientation(0, 2, 12)
            )
        }
    }
}
