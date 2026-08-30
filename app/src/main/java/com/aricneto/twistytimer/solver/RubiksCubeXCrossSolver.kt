package com.aricneto.twistytimer.solver

object RubiksCubeXCrossSolver {
    // moves
    private val moveNames: Array<String>
    private val moves: Array<RubiksCubeSolver.State>

    init {
        moveNames = arrayOf(
            "U", "U2", "U'",
            "D", "D2", "D'",
            "L", "L2", "L'",
            "R", "R2", "R'",
            "F", "F2", "F'",
            "B", "B2", "B'",
        )

        moves = Array(moveNames.size) { i ->
            requireNotNull(RubiksCubeSolver.State.moves[moveNames[i]]) { "Move ${moveNames[i]} not found" }
        }
    }

    // constants
    private const val N_CORNERS_COMBINATIONS = 8
    private const val N_CORNERS_PERMUTATIONS = 1
    private const val N_CORNERS_ORIENTATIONS = 3
    private const val N_EDGES_COMBINATIONS = 792
    private const val N_EDGES_PERMUTATIONS = 120
    private const val N_EDGES_ORIENTATIONS = 32

    private var goalCornersPermutation = 0
    private var goalCornersOrientation = 0
    private var goalEdgesPermutation = 0
    private var goalEdgesOrientation = 0

    init {
        val goalIndices =
            stateToIndices(RubiksCubeSolver.State.id)

        goalCornersPermutation =
            goalIndices[0] * N_CORNERS_PERMUTATIONS + goalIndices[1]
        goalCornersOrientation =
            goalIndices[0] * N_CORNERS_ORIENTATIONS + goalIndices[2]
        goalEdgesPermutation =
            goalIndices[3] * N_EDGES_PERMUTATIONS + goalIndices[4]
        goalEdgesOrientation =
            goalIndices[3] * N_EDGES_ORIENTATIONS + goalIndices[5]
    }

    // move tables
    private val cornersPermutationMove: Array<IntArray>
    private val cornersOrientationMove: Array<IntArray>
    private val edgesPermutationMove: Array<IntArray>
    private val edgesOrientationMove: Array<IntArray>

    init {
        // corners permutation
        cornersPermutationMove = Array<IntArray>(N_CORNERS_COMBINATIONS * N_CORNERS_PERMUTATIONS) {
            IntArray(
                moves.size
            )
        }
        for (i in 0..<N_CORNERS_COMBINATIONS) {
            for (j in 0..<N_CORNERS_PERMUTATIONS) {
                val state = indicesToState(intArrayOf(i, j, 0, 0, 0, 0))
                for (k in moves.indices) {
                    val indices = stateToIndices(state.multiply(moves[k]))
                    cornersPermutationMove[i * N_CORNERS_PERMUTATIONS + j][k] =
                        indices[0] * N_CORNERS_PERMUTATIONS + indices[1]
                }
            }
        }

        // corners orientation
        cornersOrientationMove = Array<IntArray>(N_CORNERS_COMBINATIONS * N_CORNERS_ORIENTATIONS) {
            IntArray(
                moves.size
            )
        }
        for (i in 0..<N_CORNERS_COMBINATIONS) {
            for (j in 0..<N_CORNERS_ORIENTATIONS) {
                val state = indicesToState(intArrayOf(i, 0, j, 0, 0, 0))
                for (k in moves.indices) {
                    val indices = stateToIndices(state.multiply(moves[k]))
                    cornersOrientationMove[i * N_CORNERS_ORIENTATIONS + j][k] =
                        indices[0] * N_CORNERS_ORIENTATIONS + indices[2]
                }
            }
        }

        // edges permutation
        edgesPermutationMove = Array<IntArray>(N_EDGES_COMBINATIONS * N_EDGES_PERMUTATIONS) {
            IntArray(
                moves.size
            )
        }
        for (i in 0..<N_EDGES_COMBINATIONS) {
            for (j in 0..<N_EDGES_PERMUTATIONS) {
                val state = indicesToState(intArrayOf(0, 0, 0, i, j, 0))
                for (k in moves.indices) {
                    val indices = stateToIndices(state.multiply(moves[k]))
                    edgesPermutationMove[i * N_EDGES_PERMUTATIONS + j][k] =
                        indices[3] * N_EDGES_PERMUTATIONS + indices[4]
                }
            }
        }

        // edges orientation
        edgesOrientationMove = Array<IntArray>(N_EDGES_COMBINATIONS * N_EDGES_ORIENTATIONS) {
            IntArray(
                moves.size
            )
        }
        for (i in 0..<N_EDGES_COMBINATIONS) {
            for (j in 0..<N_EDGES_ORIENTATIONS) {
                val state = indicesToState(intArrayOf(0, 0, 0, i, 0, j))
                for (k in moves.indices) {
                    val indices = stateToIndices(state.multiply(moves[k]))
                    edgesOrientationMove[i * N_EDGES_ORIENTATIONS + j][k] =
                        indices[3] * N_EDGES_ORIENTATIONS + indices[5]
                }
            }
        }
    }

    private fun stateToIndices(state: RubiksCubeSolver.State): IntArray {
        // corners
        val selectedCorners = booleanArrayOf(
            false, false, false, false,
            true, false, false, false,
        )

        val cornersMapping = byteArrayOf(
            -1, -1, -1, -1,
            0, -1, -1, -1,
        )

        val cornersCombination = BooleanArray(state.cornersPermutation.size)
        for (i in cornersCombination.indices) {
            cornersCombination[i] = selectedCorners[state.cornersPermutation[i].toInt()]
        }
        val cornersCombinationIndex =
            IndexMapping.combinationToIndex(cornersCombination, 1)

        val cornersPermutation = ByteArray(1)
        val cornersOrientation = ByteArray(1)
        var next = 0
        for (i in state.cornersPermutation.indices) {
            if (cornersCombination[i]) {
                cornersPermutation[next] = cornersMapping[state.cornersPermutation[i].toInt()]
                cornersOrientation[next] = state.cornersOrientation[i]
                next++
            }
        }
        val cornersPermutationIndex =
            IndexMapping.permutationToIndex(cornersPermutation)
        val cornersOrientationIndex =
            IndexMapping.orientationToIndex(cornersOrientation, 3)

        // edges
        val selectedEdges = booleanArrayOf(
            true, false, false, false,
            false, false, false, false,
            true, true, true, true,
        )

        val edgesMapping = byteArrayOf(
            0, -1, -1, -1,
            -1, -1, -1, -1,
            1, 2, 3, 4,
        )

        val edgesCombination = BooleanArray(state.edgesPermutation.size)
        for (i in edgesCombination.indices) {
            edgesCombination[i] = selectedEdges[state.edgesPermutation[i].toInt()]
        }
        val edgesCombinationIndex =
            IndexMapping.combinationToIndex(edgesCombination, 5)

        val edgesPermutation = ByteArray(5)
        val edgesOrientation = ByteArray(5)
        next = 0
        for (i in state.edgesPermutation.indices) {
            if (edgesCombination[i]) {
                edgesPermutation[next] = edgesMapping[state.edgesPermutation[i].toInt()]
                edgesOrientation[next] = state.edgesOrientation[i]
                next++
            }
        }
        val edgesPermutationIndex =
            IndexMapping.permutationToIndex(edgesPermutation)
        val edgesOrientationIndex =
            IndexMapping.orientationToIndex(edgesOrientation, 2)

        return intArrayOf(
            cornersCombinationIndex,
            cornersPermutationIndex,
            cornersOrientationIndex,
            edgesCombinationIndex,
            edgesPermutationIndex,
            edgesOrientationIndex,
        )
    }

    private fun indicesToState(indices: IntArray): RubiksCubeSolver.State {
        // corners
        var combination =
            IndexMapping.indexToCombination(indices[0], 1, 8)
        var permutation =
            IndexMapping.indexToPermutation(indices[1], 1)
        var orientation =
            IndexMapping.indexToOrientation(indices[2], 3, 1)

        val selectedCorners = byteArrayOf(4)
        var nextSelectedCornerIndex = 0
        val otherCorners = byteArrayOf(0, 1, 2, 3, 5, 6, 7)
        var nextOtherCornerIndex = 0

        val cornersPermutation = ByteArray(8)
        val cornersOrientation = ByteArray(8)
        for (i in cornersPermutation.indices) {
            if (combination[i]) {
                cornersPermutation[i] =
                    selectedCorners[permutation[nextSelectedCornerIndex].toInt()]
                cornersOrientation[i] = orientation[nextSelectedCornerIndex]
                nextSelectedCornerIndex++
            } else {
                cornersPermutation[i] = otherCorners[nextOtherCornerIndex]
                cornersOrientation[i] = 0
                nextOtherCornerIndex++
            }
        }

        // edges
        combination =
            IndexMapping.indexToCombination(indices[3], 5, 12)
        permutation =
            IndexMapping.indexToPermutation(indices[4], 5)
        orientation =
            IndexMapping.indexToOrientation(indices[5], 2, 5)

        val selectedEdges = byteArrayOf(0, 8, 9, 10, 11)
        var nextSelectedEdgeIndex = 0
        val otherEdges = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        var nextOtherEdgeIndex = 0

        val edgesPermutation = ByteArray(12)
        val edgesOrientation = ByteArray(12)
        for (i in edgesPermutation.indices) {
            if (combination[i]) {
                edgesPermutation[i] = selectedEdges[permutation[nextSelectedEdgeIndex].toInt()]
                edgesOrientation[i] = orientation[nextSelectedEdgeIndex]
                nextSelectedEdgeIndex++
            } else {
                edgesPermutation[i] = otherEdges[nextOtherEdgeIndex]
                edgesOrientation[i] = 0
                nextOtherEdgeIndex++
            }
        }

        return RubiksCubeSolver.State(
            cornersPermutation,
            cornersOrientation,
            edgesPermutation,
            edgesOrientation
        )
    }

    // distance tables
    private var edgesPermutationDistance: ByteArray
    private var edgesOrientationDistance: ByteArray

    init {
        // edges permutation
        edgesPermutationDistance = ByteArray(N_EDGES_COMBINATIONS * N_EDGES_PERMUTATIONS)
        for (i in edgesPermutationDistance.indices) {
            edgesPermutationDistance[i] = -1
        }
        edgesPermutationDistance[goalEdgesPermutation] = 0

        var distance = 0
        var nVisited = 1
        while (nVisited < N_EDGES_COMBINATIONS * N_EDGES_PERMUTATIONS) {
            for (i in edgesPermutationDistance.indices) {
                if (edgesPermutationDistance[i].toInt() != distance) {
                    continue
                }

                for (j in edgesPermutationMove[i].indices) {
                    val next = edgesPermutationMove[i][j]
                    if (edgesPermutationDistance[next] < 0) {
                        edgesPermutationDistance[next] = (distance + 1).toByte()
                        nVisited++
                    }
                }
            }

            distance++
        }


        // edges orientation
        edgesOrientationDistance = ByteArray(N_EDGES_COMBINATIONS * N_EDGES_ORIENTATIONS)
        for (i in edgesOrientationDistance.indices) {
            edgesOrientationDistance[i] = -1
        }
        edgesOrientationDistance[goalEdgesOrientation] = 0

        distance = 0
        nVisited = 1
        while (nVisited < N_EDGES_COMBINATIONS * N_EDGES_ORIENTATIONS) {
            for (i in edgesOrientationDistance.indices) {
                if (edgesOrientationDistance[i].toInt() != distance) {
                    continue
                }

                for (j in edgesOrientationMove[i].indices) {
                    val next = edgesOrientationMove[i][j]
                    if (edgesOrientationDistance[next] < 0) {
                        edgesOrientationDistance[next] = (distance + 1).toByte()
                        nVisited++
                    }
                }
            }

            distance++
        }
    }

    fun solve(state: RubiksCubeSolver.State): ArrayList<Array<String>> {
        val indices = stateToIndices(state)

        val cornersPermutationIndex =
            indices[0] * N_CORNERS_PERMUTATIONS + indices[1]
        val cornersOrientationIndex =
            indices[0] * N_CORNERS_ORIENTATIONS + indices[2]
        val edgesPermutationIndex =
            indices[3] * N_EDGES_PERMUTATIONS + indices[4]
        val edgesOrientationIndex =
            indices[3] * N_EDGES_ORIENTATIONS + indices[5]

        val solutions = ArrayList<Array<String>>()

        var depth = 0
        while (true) {
            val path = IntArray(depth)

            search(
                cornersPermutationIndex,
                cornersOrientationIndex,
                edgesPermutationIndex,
                edgesOrientationIndex,
                depth,
                path,
                solutions
            )

            if (solutions.size > 0) {
                return solutions
            }
            depth++
        }
    }

    private fun search(
        cornersPermutation: Int,
        cornersOrientation: Int,
        edgesPermutation: Int,
        edgesOrientation: Int,
        depth: Int,
        path: IntArray,
        solutions: ArrayList<Array<String>>
    ) {
        if (solutions.size >= 2) {
            return
        }

        if (depth == 0) {
            if (cornersPermutation == goalCornersPermutation && cornersOrientation == goalCornersOrientation && edgesPermutation == goalEdgesPermutation && edgesOrientation == goalEdgesOrientation) {
                val sequence = Array(path.size) { i ->
                    moveNames[path[i]]
                }

                solutions.add(sequence)
            }

            return
        }

        if (edgesPermutationDistance[edgesPermutation] > depth ||
            edgesOrientationDistance[edgesOrientation] > depth
        ) {
            return
        }

        for (i in moves.indices) {
            path[path.size - depth] = i
            search(
                cornersPermutationMove[cornersPermutation][i],
                cornersOrientationMove[cornersOrientation][i],
                edgesPermutationMove[edgesPermutation][i],
                edgesOrientationMove[edgesOrientation][i],
                depth - 1,
                path,
                solutions
            )
        }
    }
}
