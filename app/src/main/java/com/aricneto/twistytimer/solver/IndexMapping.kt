package com.aricneto.twistytimer.solver

object IndexMapping {
    // permutation
    fun permutationToIndex(permutation: ByteArray): Int {
        var index = 0
        for (i in 0..<permutation.size - 1) {
            index *= permutation.size - i
            for (j in i + 1..<permutation.size) {
                if (permutation[i] > permutation[j]) {
                    index++
                }
            }
        }

        return index
    }

    fun indexToPermutation(index: Int, length: Int): ByteArray {
        var index = index
        val permutation = ByteArray(length)
        permutation[length - 1] = 0
        for (i in length - 2 downTo 0) {
            permutation[i] = (index % (length - i)).toByte()
            index /= length - i
            for (j in i + 1..<length) {
                if (permutation[j] >= permutation[i]) {
                    permutation[j]++
                }
            }
        }

        return permutation
    }

    // even permutation
    fun evenPermutationToIndex(permutation: ByteArray): Int {
        var index = 0
        for (i in 0..<permutation.size - 2) {
            index *= permutation.size - i
            for (j in i + 1..<permutation.size) {
                if (permutation[i] > permutation[j]) {
                    index++
                }
            }
        }

        return index
    }

    fun indexToEvenPermutation(index: Int, length: Int): ByteArray {
        var index = index
        var sum = 0
        val permutation = ByteArray(length)

        permutation[length - 1] = 1
        permutation[length - 2] = 0
        for (i in length - 3 downTo 0) {
            permutation[i] = (index % (length - i)).toByte()
            sum += permutation[i].toInt()
            index /= length - i
            for (j in i + 1..<length) {
                if (permutation[j] >= permutation[i]) {
                    permutation[j]++
                }
            }
        }

        if (sum % 2 != 0) {
            val temp = permutation[permutation.size - 1]
            permutation[permutation.size - 1] = permutation[permutation.size - 2]
            permutation[permutation.size - 2] = temp
        }

        return permutation
    }

    // orientation
    fun orientationToIndex(orientation: ByteArray, nValues: Int): Int {
        var index = 0
        for (i in orientation.indices) {
            index = nValues * index + orientation[i]
        }

        return index
    }

    fun indexToOrientation(index: Int, nValues: Int, length: Int): ByteArray {
        var index = index
        val orientation = ByteArray(length)
        for (i in length - 1 downTo 0) {
            orientation[i] = (index % nValues).toByte()
            index /= nValues
        }

        return orientation
    }

    // zero sum orientation
    fun zeroSumOrientationToIndex(orientation: ByteArray, nValues: Int): Int {
        var index = 0
        for (i in 0..<orientation.size - 1) {
            index = nValues * index + orientation[i]
        }

        return index
    }

    fun indexToZeroSumOrientation(index: Int, nValues: Int, length: Int): ByteArray {
        var index = index
        val orientation = ByteArray(length)
        orientation[length - 1] = 0
        for (i in length - 2 downTo 0) {
            orientation[i] = (index % nValues).toByte()
            index /= nValues

            orientation[length - 1] = (orientation[length - 1] + orientation[i]).toByte()
        }
        orientation[length - 1] = ((nValues - orientation[length - 1] % nValues) % nValues).toByte()

        return orientation
    }

    // combinations
    private fun nChooseK(n: Int, k: Int): Int {
        var value = 1

        for (i in 0..<k) {
            value *= n - i
        }

        for (i in 0..<k) {
            value /= k - i
        }

        return value
    }

    fun combinationToIndex(combination: BooleanArray, k: Int): Int {
        var k = k
        var index = 0
        var i = combination.size - 1
        while (i >= 0 && k > 0) {
            if (combination[i]) {
                index += nChooseK(i, k--)
            }
            i--
        }

        return index
    }

    fun indexToCombination(index: Int, k: Int, length: Int): BooleanArray {
        var index = index
        var k = k
        val combination = BooleanArray(length)
        var i = length - 1
        while (i >= 0 && k >= 0) {
            if (index >= nChooseK(i, k)) {
                combination[i] = true
                index -= nChooseK(i, k--)
            }
            i--
        }

        return combination
    }
}