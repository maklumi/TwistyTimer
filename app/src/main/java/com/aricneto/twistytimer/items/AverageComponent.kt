package com.aricneto.twistytimer.items

import android.util.Log
import com.aricneto.twistytimer.stats.AverageCalculator.Companion.DNF
import com.aricneto.twistytimer.stats.AverageCalculator.Companion.UNKNOWN
import com.aricneto.twistytimer.structures.RedBlackTree

/**
 * Stores a balanced tree, its sum, and its least and greatest times
 */
class AverageComponent {
    private var sum: Long = UNKNOWN
    private var least: Long = UNKNOWN
    private var greatest: Long = UNKNOWN
    private val tree: RedBlackTree<Long> = RedBlackTree()

    /**
     * Inserts an element into the tree and updates its sum and best/worst cache
     * @param value The value to be inserted
     */
    fun put(value: Long) {
        tree.add(value)
        addSum(value)

        // Update least/greatest caches if necessary
        if (least != UNKNOWN && value < least) {
            least = value
        }
        if (greatest != UNKNOWN && value > greatest) {
            greatest = value
        }
    }

    /**
     * Removes an element from tree and updates its sum and best/worst cache
     * @param value The value to be removed
     */
    fun remove(value: Long) {
        try {
            tree.remove(value)
        } catch (e: Exception) {
            Log.d("AverageComponent", "Error $e while trying to remove value: $value")
        }
        subSum(value)

        // Update least/greatest caches if necessary
        if (least != UNKNOWN && value == least) {
            least = UNKNOWN
        }
        if (greatest != UNKNOWN && value == greatest) {
            greatest = UNKNOWN
        }
    }

    /**
     * Gets the smallest element of the tree
     * @return The smallest element of the tree
     */
    fun getLeast(): Long {
        // Cache request
        if (least == UNKNOWN && tree.size() > 0) {
            least = tree.getLeast() ?: UNKNOWN
        }
        return least
    }

    /**
     * Gets the biggest element of the tree
     * @return The biggest element of the tree
     */
    fun getGreatest(): Long {
        // Cache request
        if (greatest == UNKNOWN && tree.size() > 0) {
            greatest = tree.getGreatest() ?: UNKNOWN
        }
        return greatest
    }

    /**
     * Gets the sum of all elements of the tree
     * @return The sum of all elements of the tree
     */
    fun getSum(): Long {
        return sum
    }

    /**
     * Adds a value to the total sum of the tree
     * @param value The value to be added
     */
    private fun addSum(value: Long) {
        if (value != DNF) {
            sum = (if (sum == UNKNOWN) 0L else sum) + value
        }
    }

    /**
     * Removes a value from the total sum of the tree
     * @param value The value to be removed
     */
    private fun subSum(value: Long) {
        if (value != DNF && sum != 0L) {
            sum = (if (sum == UNKNOWN) 0L else sum) - value
        }
    }
}
