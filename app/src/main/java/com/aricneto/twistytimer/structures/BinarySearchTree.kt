package com.aricneto.twistytimer.structures

import java.lang.reflect.Array as javaArray
import java.util.ArrayDeque
import java.util.Deque
import java.util.HashSet
import java.util.Queue
import java.util.Random

/**
 * A binary search tree (BST), which may sometimes also be called an ordered or
 * sorted binary tree, is a node-based binary tree data structure which has the
 * following properties: 1) The left subtree of a node contains only nodes with
 * keys less than the node's key. 2) The right subtree of a node contains only
 * nodes with keys greater than the node's key. 3) Both the left and right
 * subtrees must also be binary search trees.
 * <p>
 * @see <a href="https://en.wikipedia.org/wiki/Binary_search_tree">Binary Search Tree (Wikipedia)</a>
 * <br>
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
@Suppress("UNCHECKED_CAST")
open class BinarySearchTree<T : Comparable<T>> : ITree<T> {

    private var modifications = 0

    protected var root: Node<T>? = null
    protected var size = 0
    protected var creator: INodeCreator<T>

    enum class DepthFirstSearchOrder {
        IN_ORDER, PRE_ORDER, POST_ORDER
    }

    /**
     * Default constructor.
     */
    constructor() {
        this.creator = object : INodeCreator<T> {
            /**
             * {@inheritDoc}
             */
            override fun createNewNode(parent: Node<T>?, id: T?): Node<T> {
                return Node(parent, id)
            }
        }
    }

    /**
     * Constructor with external Node creator.
     */
    constructor(creator: INodeCreator<T>) {
        this.creator = creator
    }

    /**
     * {@inheritDoc}
     */
    override fun add(value: T): Boolean {
        val nodeAdded = this.addValue(value)
        return nodeAdded != null
    }

    /**
     * Add value to the tree and return the Node that was added. Tree can
     * contain multiple equal values.
     *
     * @param value
     *            T to add to the tree.
     * @return Node<T> which was added to the tree.
     */
    protected open fun addValue(value: T): Node<T>? {
        val newNode = this.creator.createNewNode(null, value)

        // If root is null, assign
        if (root == null) {
            root = newNode
            size++
            return newNode
        }

        var node = root
        while (node != null) {
            val nodeId = node.id ?: return null
            if (value <= nodeId) {
                // Less than or equal to goes left
                if (node.lesser == null) {
                    // New left node
                    node.lesser = newNode
                    newNode.parent = node
                    size++
                    return newNode
                }
                node = node.lesser
            } else {
                // Greater than goes right
                if (node.greater == null) {
                    // New right node
                    node.greater = newNode
                    newNode.parent = node
                    size++
                    return newNode
                }
                node = node.greater
            }
        }

        return newNode
    }

    /**
     * {@inheritDoc}
     */
    override fun contains(value: T): Boolean {
        val node = getNode(value)
        return node != null
    }

    /**
     * Locate T in the tree.
     *
     * @param value
     *            T to locate in the tree.
     * @return Node<T> representing first reference of value in tree or NULL if
     *         not found.
     */
    protected open fun getNode(value: T): Node<T>? {
        var node = root
        while (node != null && node.id != null) {
            val nodeId = node.id!!
            if (value < nodeId) {
                node = node.lesser
            } else if (value > nodeId) {
                node = node.greater
            } else {
                return node
            }
        }
        return null
    }

    /**
     * Rotate tree left at sub-tree rooted at node.
     *
     * @param node
     *            Root of tree to rotate left.
     */
    protected open fun rotateLeft(node: Node<T>) {
        val parent = node.parent
        val greater = node.greater ?: return
        val lesser = greater.lesser

        greater.lesser = node
        node.parent = greater

        node.greater = lesser

        if (lesser != null)
            lesser.parent = node

        if (parent != null) {
            if (node === parent.lesser) {
                parent.lesser = greater
            } else if (node === parent.greater) {
                parent.greater = greater
            } else {
                throw RuntimeException("Yikes! I'm not related to my parent. $node")
            }
            greater.parent = parent
        } else {
            root = greater
            root?.parent = null
        }
    }

    /**
     * Rotate tree right at sub-tree rooted at node.
     *
     * @param node
     *            Root of tree to rotate right.
     */
    protected open fun rotateRight(node: Node<T>) {
        val parent = node.parent
        val lesser = node.lesser ?: return
        val greater = lesser.greater

        lesser.greater = node
        node.parent = lesser

        node.lesser = greater

        if (greater != null)
            greater.parent = node

        if (parent != null) {
            if (node === parent.lesser) {
                parent.lesser = lesser
            } else if (node === parent.greater) {
                parent.greater = lesser
            } else {
                throw RuntimeException("Yikes! I'm not related to my parent. $node")
            }
            lesser.parent = parent
        } else {
            root = lesser
            root?.parent = null
        }
    }

    /**
     * Get greatest node in sub-tree rooted at startingNode. The search does not
     * include startingNode in it's results.
     *
     * @param startingNode
     *            Root of tree to search.
     * @return Node<T> which represents the greatest node in the startingNode
     *         sub-tree or NULL if startingNode has no greater children.
     */
    protected open fun getGreatest(startingNode: Node<T>?): Node<T>? {
        if (startingNode == null)
            return null

        var greater = startingNode.greater
        while (greater != null && greater.id != null) {
            val node = greater.greater
            if (node != null && node.id != null)
                greater = node
            else
                break
        }
        return greater
    }

    /**
     * Get greatest value in root tree
     *
     * @return Value of the greatest node in root, or NULL if root is empty
     */
    override fun getGreatest(): T? {
        val rootNode = root ?: return null

        var greater = rootNode
        while (greater.id != null) {
            val node = greater.greater
            if (node != null && node.id != null)
                greater = node
            else
                break
        }
        return greater.id
    }

    /**
     * Get least node in sub-tree rooted at startingNode. The search does not
     * include startingNode in it's results.
     *
     * @param startingNode
     *            Root of tree to search.
     * @return Node<T> which represents the least node in the startingNode
     *         sub-tree or NULL if startingNode has no lesser children.
     */
    protected open fun getLeast(startingNode: Node<T>?): Node<T>? {
        if (startingNode == null)
            return null

        var lesser = startingNode.lesser
        while (lesser != null && lesser.id != null) {
            val node = lesser.lesser
            if (node != null && node.id != null)
                lesser = node
            else
                break
        }
        return lesser
    }

    /**
     * Get least value in root tree
     *
     * @return Value of the least node in root, or NULL if root is empty
     */
    override fun getLeast(): T? {
        val rootNode = root ?: return null

        var lesser = rootNode
        while (lesser.id != null) {
            val node = lesser.lesser
            if (node != null && node.id != null)
                lesser = node
            else
                break
        }
        return lesser.id
    }

    /**
     * {@inheritDoc}
     */
    override fun remove(value: T): T? {
        val nodeToRemove = this.removeValue(value)
        return nodeToRemove?.id
    }

    /**
     * Remove first occurrence of value in the tree.
     *
     * @param value
     *            T to remove from the tree.
     * @return Node<T> which was removed from the tree.
     */
    protected open fun removeValue(value: T): Node<T>? {
        var nodeToRemoved = this.getNode(value)
        if (nodeToRemoved != null)
            nodeToRemoved = removeNode(nodeToRemoved)
        return nodeToRemoved
    }

    /**
     * Remove the node using a replacement
     *
     * @param nodeToRemoved
     *            Node<T> to remove from the tree.
     * @return nodeRemove
     *            Node<T> removed from the tree, it can be different
     *            then the parameter in some cases.
     */
    open fun removeNode(nodeToRemoved: Node<T>?): Node<T>? {
        if (nodeToRemoved != null) {
            val replacementNode = this.getReplacementNode(nodeToRemoved)
            replaceNodeWithNode(nodeToRemoved, replacementNode)
        }
        return nodeToRemoved
    }

    /**
     * Get the proper replacement node according to the binary search tree
     * algorithm from the tree.
     *
     * @param nodeToRemoved
     *            Node<T> to find a replacement for.
     * @return Node<T> which can be used to replace nodeToRemoved. nodeToRemoved
     *         should NOT be NULL.
     */
    protected open fun getReplacementNode(nodeToRemoved: Node<T>): Node<T>? {
        var replacement: Node<T>? = null
        if (nodeToRemoved.greater != null && nodeToRemoved.lesser != null) {
            // Two children.
            // Add some randomness to deletions, so we don't always use the
            // greatest/least on deletion
            if (modifications % 2 != 0) {
                replacement = this.getGreatest(nodeToRemoved.lesser)
                if (replacement == null)
                    replacement = nodeToRemoved.lesser
            } else {
                replacement = this.getLeast(nodeToRemoved.greater)
                if (replacement == null)
                    replacement = nodeToRemoved.greater
            }
            modifications++
        } else if (nodeToRemoved.lesser != null) {
            // Using the less subtree
            replacement = nodeToRemoved.lesser
        } else if (nodeToRemoved.greater != null) {
            // Using the greater subtree (there is no lesser subtree, no refactoring)
            replacement = nodeToRemoved.greater
        }
        return replacement
    }

    /**
     * Replace nodeToRemoved with replacementNode in the tree.
     *
     * @param nodeToRemoved
     *            Node<T> to remove replace in the tree. nodeToRemoved should
     *            NOT be NULL.
     * @param replacementNode
     *            Node<T> to replace nodeToRemoved in the tree. replacementNode
     *            can be NULL.
     */
    protected open fun replaceNodeWithNode(nodeToRemoved: Node<T>, replacementNode: Node<T>?) {
        if (replacementNode != null) {
            // Save for later
            val replacementNodeLesser = replacementNode.lesser
            val replacementNodeGreater = replacementNode.greater

            // Replace replacementNode's branches with nodeToRemove's branches
            val nodeToRemoveLesser = nodeToRemoved.lesser
            if (nodeToRemoveLesser != null && nodeToRemoveLesser !== replacementNode) {
                replacementNode.lesser = nodeToRemoveLesser
                nodeToRemoveLesser.parent = replacementNode
            }
            val nodeToRemoveGreater = nodeToRemoved.greater
            if (nodeToRemoveGreater != null && nodeToRemoveGreater !== replacementNode) {
                replacementNode.greater = nodeToRemoveGreater
                nodeToRemoveGreater.parent = replacementNode
            }

            // Remove link from replacementNode's parent to replacement
            val replacementParent = replacementNode.parent
            if (replacementParent != null && replacementParent !== nodeToRemoved) {
                val replacementParentLesser = replacementParent.lesser
                val replacementParentGreater = replacementParent.greater
                if (replacementParentLesser != null && replacementParentLesser === replacementNode) {
                    replacementParent.lesser = replacementNodeGreater
                    replacementNodeGreater?.parent = replacementParent
                } else if (replacementParentGreater != null && replacementParentGreater === replacementNode) {
                    replacementParent.greater = replacementNodeLesser
                    replacementNodeLesser?.parent = replacementParent
                }
            }
        }

        // Update the link in the tree from the nodeToRemoved to the
        // replacementNode
        val parent = nodeToRemoved.parent
        if (parent == null) {
            // Replacing the root node
            root = replacementNode
            root?.parent = null
        } else if (parent.lesser != null && parent.lesser!!.id!!.compareTo(nodeToRemoved.id!!) == 0) {
            parent.lesser = replacementNode
            replacementNode?.parent = parent
        } else if (parent.greater != null && parent.greater!!.id!!.compareTo(nodeToRemoved.id!!) == 0) {
            parent.greater = replacementNode
            replacementNode?.parent = parent
        }
        size--
    }

    /**
     * {@inheritDoc}
     */
    override fun clear() {
        root = null
        size = 0
    }

    /**
     * {@inheritDoc}
     */
    override fun size(): Int {
        return size
    }

    /**
     * {@inheritDoc}
     */
    override fun validate(): Boolean {
        val rootNode = root ?: return true
        return validateNode(rootNode)
    }

    /**
     * Validate the node for all Binary Search Tree invariants.
     *
     * @param node
     *            Node<T> to validate in the tree. node should NOT be NULL.
     * @return True if the node is valid.
     */
    protected open fun validateNode(node: Node<T>): Boolean {
        val lesser = node.lesser
        val greater = node.greater

        var lesserCheck = true
        if (lesser != null && lesser.id != null) {
            lesserCheck = lesser.id!!.compareTo(node.id!!) <= 0
            if (lesserCheck)
                lesserCheck = validateNode(lesser)
        }
        if (!lesserCheck)
            return false

        var greaterCheck = true
        if (greater != null && greater.id != null) {
            greaterCheck = greater.id!!.compareTo(node.id!!) > 0
            if (greaterCheck)
                greaterCheck = validateNode(greater)
        }
        return greaterCheck
    }

    /**
     * Get an array representation of the tree in breath first search order.
     *
     * @return breath first search sorted array representing the tree.
     */
    fun getBFS(): Array<T>? {
        val rootNode = root ?: return null
        return getBFS(rootNode, this.size)
    }

    /**
     * Get an array representation of the tree in level order.
     *
     * @return level order sorted array representing the tree.
     */
    fun getLevelOrder(): Array<T>? {
        return getBFS()
    }

    /**
     * Get an array representation of the tree in-order.
     *
     * @param order of search
     *
     * @return order sorted array representing the tree.
     */
    fun getDFS(order: DepthFirstSearchOrder): Array<T>? {
        val rootNode = root ?: return null
        return getDFS(order, rootNode, this.size)
    }

    /**
     * Get an array representation of the tree in sorted order.
     *
     * @return sorted array representing the tree.
     */
    fun getSorted(): Array<T>? {
        // Depth first search to traverse the tree in-order sorted.
        return getDFS(DepthFirstSearchOrder.IN_ORDER)
    }

    /**
     * {@inheritDoc}
     */
    override fun toCollection(): Collection<T> {
        return JavaCompatibleBinarySearchTree(this)
    }

    /**
     * {@inheritDoc}
     */
    override fun toString(): String {
        return TreePrinter.getString(this)
    }

    open class Node<T : Comparable<T>>(
        var parent: Node<T>? = null,
        var id: T? = null
    ) {
        var lesser: Node<T>? = null
        var greater: Node<T>? = null

        /**
         * {@inheritDoc}
         */
        override fun toString(): String {
            return "id=$id parent=${parent?.id ?: "NULL"} lesser=${lesser?.id ?: "NULL"} greater=${greater?.id ?: "NULL"}"
        }
    }

    interface INodeCreator<T : Comparable<T>> {

        /**
         * Create a new Node with the following parameters.
         *
         * @param parent
         *            of this node.
         * @param id
         *            of this node.
         * @return new Node
         */
        fun createNewNode(parent: Node<T>?, id: T?): Node<T>
    }

    protected object TreePrinter {

        fun <T : Comparable<T>> getString(tree: BinarySearchTree<T>): String {
            val root = tree.root ?: return "Tree has no nodes."
            return getString(root, "", true)
        }

        private fun <T : Comparable<T>> getString(node: Node<T>, prefix: String, isTail: Boolean): String {
            val builder = StringBuilder()

            val parent = node.parent
            if (parent != null) {
                var side = "left"
                if (node === parent.greater)
                    side = "right"
                builder.append("$prefix${if (isTail) "└── " else "├── "}($side) ${node.id}\n")
            } else {
                builder.append("$prefix${if (isTail) "└── " else "├── "}${node.id}\n")
            }
            
            val children = mutableListOf<Node<T>>()
            node.lesser?.let { children.add(it) }
            node.greater?.let { children.add(it) }

            for (i in 0 until children.size - 1) {
                builder.append(getString(children[i], prefix + if (isTail) "    " else "│   ", false))
            }
            if (children.isNotEmpty()) {
                builder.append(getString(children.last(), prefix + if (isTail) "    " else "│   ", true))
            }

            return builder.toString()
        }
    }

    private class JavaCompatibleBinarySearchTree<T : Comparable<T>>(
        private val tree: BinarySearchTree<T>
    ) : AbstractMutableCollection<T>() {

        override fun add(element: T): Boolean {
            return tree.add(element)
        }

        override fun remove(element: T): Boolean {
            return tree.remove(element) != null
        }

        override fun contains(element: T): Boolean {
            return tree.contains(element)
        }

        override val size: Int
            get() = tree.size()

        override fun iterator(): MutableIterator<T> {
            return BinarySearchTreeIterator(this.tree)
        }

        private class BinarySearchTreeIterator<C : Comparable<C>>(
            private val tree: BinarySearchTree<C>
        ) : MutableIterator<C> {

            private var last: Node<C>? = null
            private val toVisit: Deque<Node<C>> = ArrayDeque()

            init {
                tree.root?.let { toVisit.add(it) }
            }

            /**
             * {@inheritDoc}
             */
            override fun hasNext(): Boolean {
                return toVisit.isNotEmpty()
            }

            /**
             * {@inheritDoc}
             */
            override fun next(): C {
                if (toVisit.isEmpty()) throw NoSuchElementException()
                val n = toVisit.pop()
                n.lesser?.let { toVisit.add(it) }
                n.greater?.let { toVisit.add(it) }
                last = n
                return n.id!!
            }

            /**
             * {@inheritDoc}
             */
            override fun remove() {
                tree.removeNode(last)
            }
        }
    }

    companion object {
        protected val RANDOM = Random()

        /**
         * Get an array representation of the tree in breath first search order.
         *
         * @param start rooted node
         * @param size of tree rooted at start
         *
         * @return breath first search sorted array representing the tree.
         */
        fun <T : Comparable<T>> getBFS(start: Node<T>, size: Int): Array<T> {
            val queue: Queue<Node<T>> = ArrayDeque()
            val values = javaArray.newInstance(start.id!!.javaClass, size) as Array<T>
            var count = 0
            var node: Node<T>? = start
            while (node != null) {
                values[count++] = node.id!!
                node.lesser?.let { queue.add(it) }
                node.greater?.let { queue.add(it) }
                node = if (queue.isNotEmpty()) queue.remove() else null
            }
            return values
        }

        /**
         * Get an array representation of the tree in-order.
         *
         * @param order of search
         * @param start rooted node
         * @param size of tree rooted at start
         *
         * @return order sorted array representing the tree.
         */
        fun <T : Comparable<T>> getDFS(order: DepthFirstSearchOrder, start: Node<T>, size: Int): Array<T> {
            val added = HashSet<Node<T>>(2)
            val nodes = javaArray.newInstance(start.id!!.javaClass, size) as Array<T>
            var index = 0
            var node: Node<T>? = start
            while (index < size && node != null) {
                val parent = node.parent
                val lesser = if (node.lesser != null && !added.contains(node.lesser)) node.lesser else null
                val greater = if (node.greater != null && !added.contains(node.greater)) node.greater else null

                if (parent == null && lesser == null && greater == null) {
                    if (!added.contains(node))
                        nodes[index++] = node.id!!
                    break
                }

                when (order) {
                    DepthFirstSearchOrder.IN_ORDER -> {
                        if (lesser != null) {
                            node = lesser
                        } else {
                            if (!added.contains(node)) {
                                nodes[index++] = node.id!!
                                added.add(node)
                            }
                            if (greater != null) {
                                node = greater
                            } else if (added.contains(node)) {
                                node = parent
                            } else {
                                // We should not get here. Stop the loop!
                                node = null
                            }
                        }
                    }
                    DepthFirstSearchOrder.PRE_ORDER -> {
                        if (!added.contains(node)) {
                            nodes[index++] = node.id!!
                            added.add(node)
                        }
                        if (lesser != null) {
                            node = lesser
                        } else if (greater != null) {
                            node = greater
                        } else if (added.contains(node)) {
                            node = parent
                        } else {
                            // We should not get here. Stop the loop!
                            node = null
                        }
                    }
                    DepthFirstSearchOrder.POST_ORDER -> {
                        if (lesser != null) {
                            node = lesser
                        } else {
                            if (greater != null) {
                                node = greater
                            } else {
                                // lesser==null && greater==null
                                nodes[index++] = node.id!!
                                added.add(node)
                                node = parent
                            }
                        }
                    }
                }
            }
            return nodes
        }
    }
}
