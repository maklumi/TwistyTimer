package com.aricneto.twistytimer.structures

/**
 * A tree can be defined recursively (locally) as a collection of nodes (starting at a root node),
 * where each node is a data structure consisting of a value, together with a list of nodes (the "children"),
 * with the constraints that no node is duplicated. A tree can be defined abstractly as a whole (globally)
 * as an ordered tree, with a value assigned to each node.
 * <p>
 * @see <a href="https://en.wikipedia.org/wiki/Tree_(data_structure)">Tree (Wikipedia)</a>
 * <br>
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
interface ITree<T> {

    /**
     * Add value to the tree. Tree can contain multiple equal values.
     *
     * @param value to add to the tree.
     * @return True if successfully added to tree.
     */
    fun add(value: T): Boolean

    /**
     * Remove first occurrence of value in the tree.
     *
     * @param value to remove from the tree.
     * @return T value removed from tree.
     */
    fun remove(value: T): T?

    /**
     * Clear the entire stack.
     */
    fun clear()

    /**
     * Does the tree contain the value.
     *
     * @param value to locate in the tree.
     * @return True if tree contains value.
     */
    fun contains(value: T): Boolean

    /**
     * Get number of nodes in the tree.
     *
     * @return Number of nodes in the tree.
     */
    fun size(): Int

    /**
     * Returns the smallest element of the tree
     *
     * @return Smallest element of the tree
     */
    fun getLeast(): T?

    /**
     * Returns the greatest element of the tree
     *
     * @return Greatest element of the tree
     */
    fun getGreatest(): T?

    /**
     * Validate the tree according to the invariants.
     *
     * @return True if the tree is valid.
     */
    fun validate(): Boolean

    /**
     * Get Tree as a Java compatible Collection
     *
     * @return Java compatible Collection
     */
    fun toCollection(): Collection<T>

}
