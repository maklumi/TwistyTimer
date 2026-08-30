package com.aricneto.twistytimer.structures

import java.util.ArrayDeque
import java.util.Deque

/**
 * A red–black tree is a type of self-balancing binary search tree, a data
 * structure used in computer science, typically to implement associative
 * arrays. A red–black tree is a binary search tree that inserts and deletes in
 * such a way that the tree is always reasonably balanced. Red-black trees are
 * often compared with AVL trees. AVL trees are more rigidly balanced, they are
 * faster than red-black trees for lookup intensive applications. However,
 * red-black trees are faster for insertion and removal.
 * <p>
 * @see <a href="https://en.wikipedia.org/wiki/Red%E2%80%93black_tree">Red-Black Tree (Wikipedia)</a>
 * <br>
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
@Suppress("UNCHECKED_CAST")
open class RedBlackTree<T : Comparable<T>> : BinarySearchTree<T> {

    companion object {
        protected const val BLACK = false
        protected const val RED = true
    }

    /**
     * Default constructor.
     */
    constructor() : super() {
        this.creator = object : INodeCreator<T> {
            /**
             * {@inheritDoc}
             */
            override fun createNewNode(parent: Node<T>?, id: T?): Node<T> {
                return RedBlackNode(parent, id, BLACK)
            }
        }
    }

    /**
     * Constructor with external Node creator.
     */
    constructor(creator: INodeCreator<T>) : super(creator)

    /**
     * {@inheritDoc}
     */
    override fun addValue(value: T): Node<T>? {
        if (root == null) {
            // Case 1 - The current node is at the root of the tree.

            // Defaulted to black in our creator
            val newRoot = this.creator.createNewNode(null, value) as RedBlackNode<T>
            newRoot.lesser = this.creator.createNewNode(newRoot, null)
            newRoot.greater = this.creator.createNewNode(newRoot, null)
            root = newRoot

            size++
            return root
        }

        var nodeAdded: RedBlackNode<T>? = null
        // Insert node like a BST would
        var node = root
        while (node != null) {
            if (node.id == null) {
                node.id = value
                (node as RedBlackNode<T>).color = RED

                // Defaulted to black in our creator
                node.lesser = this.creator.createNewNode(node, null)
                node.greater = this.creator.createNewNode(node, null)

                nodeAdded = node
                break
            } else {
                val nodeId = node.id!!
                if (value.compareTo(nodeId) <= 0) {
                    node = node.lesser
                } else {
                    node = node.greater
                }
            }
        }

        nodeAdded?.let { balanceAfterInsert(it) }

        size++
        return nodeAdded
    }

    /**
     * Post insertion balancing algorithm.
     *
     * @param begin
     *            to begin balancing at.
     */
    private fun balanceAfterInsert(begin: RedBlackNode<T>) {
        var node = begin
        var parent = node.parent as? RedBlackNode<T>

        if (parent == null) {
            // Case 1 - The current node is at the root of the tree.
            node.color = BLACK
            return
        }

        if (parent.color == BLACK) {
            // Case 2 - The current node's parent is black, so property 4 (both
            // children of every red node are black) is not invalidated.
            return
        }

        var grandParent = node.getGrandParent()
        var uncle = node.getUncle(grandParent)
        if (parent.color == RED && uncle?.color == RED) {
            // Case 3 - If both the parent and the uncle are red, then both of
            // them can be repainted black and the grandparent becomes
            // red (to maintain property 5 (all paths from any given node to its
            // leaf nodes contain the same number of black nodes)).
            parent.color = BLACK
            uncle.color = BLACK
            grandParent?.let {
                it.color = RED
                balanceAfterInsert(it)
            }
            return
        }

        if (parent.color == RED && (uncle == null || uncle.color == BLACK)) {
            // Case 4 - The parent is red but the uncle is black; also, the
            // current node is the right child of parent, and parent in turn
            // is the left child of its parent grandparent.
            if (grandParent != null) {
                if (node === parent.greater && parent === grandParent.lesser) {
                    // right-left
                    rotateLeft(parent)

                    node = node.lesser as RedBlackNode<T>
                    parent = node.parent as RedBlackNode<T>
                    grandParent = node.getGrandParent()
                    uncle = node.getUncle(grandParent)
                } else if (node === parent.lesser && parent === grandParent.greater) {
                    // left-right
                    rotateRight(parent)

                    node = node.greater as RedBlackNode<T>
                    parent = node.parent as RedBlackNode<T>
                    grandParent = node.getGrandParent()
                    uncle = node.getUncle(grandParent)
                }
            }
        }

        if (parent.color == RED && (uncle == null || uncle.color == BLACK)) {
            // Case 5 - The parent is red but the uncle is black, the
            // current node is the left child of parent, and parent is the
            // left child of its parent G.
            grandParent?.let { gp ->
                parent.color = BLACK
                gp.color = RED
                if (node === parent.lesser && parent === gp.lesser) {
                    // left-left
                    rotateRight(gp)
                } else if (node === parent.greater && parent === gp.greater) {
                    // right-right
                    rotateLeft(gp)
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun removeNode(nodeToRemoved: Node<T>?): Node<T>? {
        if (nodeToRemoved == null) return null

        var rbNodeToRemoved = nodeToRemoved as RedBlackNode<T>

        if (rbNodeToRemoved.isLeaf()) {
            // No children
            rbNodeToRemoved.id = null
            if (rbNodeToRemoved === root) {
                root = null
            } else {
                rbNodeToRemoved.id = null
                rbNodeToRemoved.color = BLACK
                rbNodeToRemoved.lesser = null
                rbNodeToRemoved.greater = null
            }

            size--
            return rbNodeToRemoved
        }

        // At least one child

        // Keep the id and assign it to the replacement node
        val id = rbNodeToRemoved.id
        var lesser = rbNodeToRemoved.lesser as RedBlackNode<T>
        var greater = rbNodeToRemoved.greater as RedBlackNode<T>
        if (lesser.id != null && greater.id != null) {
            // Two children
            var greatestInLesser = this.getGreatest(lesser) as? RedBlackNode<T>
            if (greatestInLesser == null || greatestInLesser.id == null)
                greatestInLesser = lesser

            // Replace node with greatest in his lesser tree, which leaves us with only one child
            replaceValueOnly(rbNodeToRemoved, greatestInLesser)
            rbNodeToRemoved = greatestInLesser
            lesser = rbNodeToRemoved.lesser as RedBlackNode<T>
            greater = rbNodeToRemoved.greater as RedBlackNode<T>
        }

        // Handle one child
        val child = if (lesser.id != null) lesser else greater
        if (rbNodeToRemoved.color == BLACK) {
            if (child.color == BLACK)
                rbNodeToRemoved.color = RED
            val result = balanceAfterDelete(rbNodeToRemoved)
            if (!result)
                return rbNodeToRemoved
        }

        // Replacing node with child
        replaceWithChild(rbNodeToRemoved, child)
        // Add the id to the child because it represents the node that was removed.
        child.id = id
        if (root === rbNodeToRemoved) {
            root?.parent = null
            (root as? RedBlackNode<T>)?.color = BLACK
            // If we replaced the root with a leaf, just null out root
            if (rbNodeToRemoved.isLeaf())
                root = null
        }
        rbNodeToRemoved = child

        size--
        return rbNodeToRemoved
    }

    /**
     * Replace value of nodeToReplaceWith with nodeToReplace.
     *
     * @param nodeToReplace
     *            will get value of nodeToReplaceWith.
     * @param nodeToReplaceWith
     *            will get value NULLed.
     */
    private fun replaceValueOnly(nodeToReplace: RedBlackNode<T>, nodeToReplaceWith: RedBlackNode<T>) {
        nodeToReplace.id = nodeToReplaceWith.id
        nodeToReplaceWith.id = null
    }

    /**
     * Replace entire contents of nodeToReplace with nodeToReplaceWith.
     *
     * @param nodeToReplace
     *            will get it's contents replace with nodeToReplaceWith
     *            contents.
     * @param nodeToReplaceWith
     *            will not be changed.
     */
    private fun replaceWithChild(nodeToReplace: RedBlackNode<T>, nodeToReplaceWith: RedBlackNode<T>) {
        nodeToReplace.id = nodeToReplaceWith.id
        nodeToReplace.color = nodeToReplaceWith.color

        nodeToReplace.lesser = nodeToReplaceWith.lesser
        nodeToReplace.lesser?.parent = nodeToReplace

        nodeToReplace.greater = nodeToReplaceWith.greater
        nodeToReplace.greater?.parent = nodeToReplace
    }

    /**
     * Post delete balancing algorithm.
     *
     * @param node
     *            to begin balancing at.
     * @return True if balanced or false if error.
     */
    private fun balanceAfterDelete(node: RedBlackNode<T>): Boolean {
        if (node.parent == null) {
            // Case 1 - node is the new root.
            return true
        }

        var parent = node.parent as RedBlackNode<T>
        var sibling = node.getSibling()
        if (sibling?.color == RED) {
            // Case 2 - sibling is red.
            parent.color = RED
            sibling.color = BLACK
            when {
                node === parent.lesser -> {
                    rotateLeft(parent)

                    // Rotation, need to update parent/sibling
                    parent = node.parent as RedBlackNode<T>
                    sibling = node.getSibling()
                }
                node === parent.greater -> {
                    rotateRight(parent)

                    // Rotation, need to update parent/sibling
                    parent = node.parent as RedBlackNode<T>
                    sibling = node.getSibling()
                }
                else -> {
                    throw RuntimeException("Yikes! I'm not related to my parent. $node")
                }
            }
        }

        if (sibling != null && parent.color == BLACK
            && sibling.color == BLACK
            && (sibling.lesser as? RedBlackNode<T>)?.color == BLACK
            && (sibling.greater as? RedBlackNode<T>)?.color == BLACK
        ) {
            // Case 3 - parent, sibling, and sibling's children are black.
            sibling.color = RED
            return balanceAfterDelete(parent)
        }

        if (sibling != null && parent.color == RED
            && sibling.color == BLACK
            && (sibling.lesser as? RedBlackNode<T>)?.color == BLACK
            && (sibling.greater as? RedBlackNode<T>)?.color == BLACK
        ) {
            // Case 4 - sibling and sibling's children are black, but parent is red.
            sibling.color = RED
            parent.color = BLACK
            return true
        }

        if (sibling != null && sibling.color == BLACK) {
            // Case 5 - sibling is black, sibling's left child is red,
            // sibling's right child is black, and node is the left child of
            // its parent.
            if (node === parent.lesser
                && (sibling.lesser as? RedBlackNode<T>)?.color == RED
                && (sibling.greater as? RedBlackNode<T>)?.color == BLACK
            ) {
                sibling.color = RED
                (sibling.lesser as RedBlackNode<T>).color = RED

                rotateRight(sibling)

                // Rotation, need to update parent/sibling
                parent = node.parent as RedBlackNode<T>
                sibling = node.getSibling()
            } else if (node === parent.greater
                       && (sibling.lesser as? RedBlackNode<T>)?.color == BLACK
                       && (sibling.greater as? RedBlackNode<T>)?.color == RED
            ) {
                sibling.color = RED
                (sibling.greater as RedBlackNode<T>).color = RED

                rotateLeft(sibling)

                // Rotation, need to update parent/sibling
                parent = node.parent as RedBlackNode<T>
                sibling = node.getSibling()
            }
        }

        // Case 6 - sibling is black, sibling's right child is red, and node
        // is the left child of its parent.
        if (sibling != null) {
            sibling.color = parent.color
            parent.color = BLACK
            if (node === parent.lesser) {
                (sibling.greater as? RedBlackNode<T>)?.color = BLACK
                rotateLeft(node.parent!!)
            } else if (node === parent.greater) {
                (sibling.lesser as? RedBlackNode<T>)?.color = BLACK
                rotateRight(node.parent!!)
            } else {
                throw RuntimeException("Yikes! I'm not related to my parent. $node")
            }
        }

        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun validate(): Boolean {
        val rootNode = root as? RedBlackNode<T> ?: return true

        if (rootNode.color == RED) {
            // Root node should be black
            return false
        }

        return this.validateNode(rootNode)
    }

    /**
     * {@inheritDoc}
     */
    override fun validateNode(node: Node<T>): Boolean {
        val rbNode = node as RedBlackNode<T>
        val lesser = rbNode.lesser as? RedBlackNode<T> ?: return true
        val greater = rbNode.greater as? RedBlackNode<T> ?: return true

        if (rbNode.isLeaf() && rbNode.color == RED) {
            // Leafs should not be red
            return false
        }

        if (rbNode.color == RED) {
            // You should not have two red nodes in a row
            if (lesser.color == RED) return false
            if (greater.color == RED) return false
        }

        if (!lesser.isLeaf()) {
            // Check BST property
            val lesserId = lesser.id!!
            val rbNodeId = rbNode.id!!
            val lesserCheck = lesserId.compareTo(rbNodeId) <= 0
            if (!lesserCheck)
                return false
            // Check red-black property
            if (!this.validateNode(lesser))
                return false
        }

        if (!greater.isLeaf()) {
            // Check BST property
            val greaterId = greater.id!!
            val rbNodeId = rbNode.id!!
            val greaterCheck = greaterId.compareTo(rbNodeId) > 0
            if (!greaterCheck)
                return false
            // Check red-black property
            if (!this.validateNode(greater))
                return false
        }

        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun toCollection(): Collection<T> {
        return JavaCompatibleRedBlackTree(this)
    }

    /**
     * {@inheritDoc}
     */
    override fun toString(): String {
        return RedBlackTreePrinter.getString(this)
    }

    class RedBlackNode<T : Comparable<T>>(
        parent: Node<T>? = null,
        id: T? = null,
        var color: Boolean = BLACK
    ) : Node<T>(parent, id) {

        fun getGrandParent(): RedBlackNode<T>? {
            if (parent == null || parent?.parent == null) return null
            return parent?.parent as? RedBlackNode<T>
        }

        fun getUncle(grandParent: RedBlackNode<T>?): RedBlackNode<T>? {
            val gp = grandParent ?: return null
            return if (gp.lesser != null && gp.lesser === parent) {
                gp.greater as? RedBlackNode<T>
            } else if (gp.greater != null && gp.greater === parent) {
                gp.lesser as? RedBlackNode<T>
            } else {
                null
            }
        }

        fun getUncle(): RedBlackNode<T>? {
            val grandParent = getGrandParent()
            return getUncle(grandParent)
        }

        fun getSibling(): RedBlackNode<T>? {
            val p = parent ?: return null
            return when {
                p.lesser === this -> p.greater as? RedBlackNode<T>
                p.greater === this -> p.lesser as? RedBlackNode<T>
                else -> throw RuntimeException("Yikes! I'm not related to my parent. $this")
            }
        }

        fun isLeaf(): Boolean {
            return lesser == null && greater == null
        }

        /**
         * {@inheritDoc}
         */
        override fun toString(): String {
            return "id=$id color=${if (color == RED) "RED" else "BLACK"} isLeaf=${isLeaf()} parent=${parent?.id ?: "NULL"} lesser=${lesser?.id ?: "NULL"} greater=${greater?.id ?: "NULL"}"
        }
    }

    protected object RedBlackTreePrinter {

        fun <T : Comparable<T>> getString(tree: RedBlackTree<T>): String {
            val root = tree.root as? RedBlackNode<T> ?: return "Tree has no nodes."
            return getString(root, "", true)
        }

        fun <T : Comparable<T>> getString(node: RedBlackNode<T>?): String {
            if (node == null)
                return "Sub-tree has no nodes."
            return getString(node, "", true)
        }

        private fun <T : Comparable<T>> getString(node: RedBlackNode<T>, prefix: String, isTail: Boolean): String {
            val builder = StringBuilder()

            builder.append("$prefix${if (isTail) "└── " else "├── "}(${if (node.color == RED) "RED" else "BLACK"}) ${node.id} [parent=${node.parent?.id ?: "NULL"} grand-parent=${node.parent?.parent?.id ?: "NULL"}]\n")
            
            val children = mutableListOf<Node<T>>()
            node.lesser?.let { children.add(it) }
            node.greater?.let { children.add(it) }

            for (i in 0 until children.size - 1) {
                builder.append(getString(children[i] as RedBlackNode<T>, prefix + if (isTail) "    " else "│   ", false))
            }
            if (children.isNotEmpty()) {
                builder.append(getString(children.last() as RedBlackNode<T>, prefix + if (isTail) "    " else "│   ", true))
            }

            return builder.toString()
        }
    }

    class JavaCompatibleRedBlackTree<T : Comparable<T>> : AbstractMutableCollection<T> {

        private val tree: RedBlackTree<T>

        constructor() {
            this.tree = RedBlackTree()
        }

        constructor(tree: RedBlackTree<T>) {
            this.tree = tree
        }

        /**
         * {@inheritDoc}
         */
        override fun add(element: T): Boolean {
            return tree.add(element)
        }

        /**
         * {@inheritDoc}
         */
        override fun remove(element: T): Boolean {
            return tree.remove(element) != null
        }

        /**
         * {@inheritDoc}
         */
        override fun contains(element: T): Boolean {
            return tree.contains(element)
        }

        /**
         * {@inheritDoc}
         */
        override val size: Int
            get() = tree.size()

        /**
         * {@inheritDoc}
         */
        override fun iterator(): MutableIterator<T> {
            return RedBlackTreeIterator(this.tree)
        }

        private class RedBlackTreeIterator<C : Comparable<C>>(
            private val tree: RedBlackTree<C>
        ) : MutableIterator<C> {

            private var last: Node<C>? = null
            private val toVisit: Deque<Node<C>> = ArrayDeque()

            init {
                tree.root?.let {
                    toVisit.add(it)
                }
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
                
                while (toVisit.isNotEmpty()) {
                    // Go thru the current nodes
                    val n = toVisit.pop()

                    // Add non-null children
                    n.lesser?.let { if (it.id != null) toVisit.add(it) }
                    n.greater?.let { if (it.id != null) toVisit.add(it) }

                    last = n
                    return n.id!!
                }
                throw NoSuchElementException()
            }

            /**
             * {@inheritDoc}
             */
            override fun remove() {
                tree.removeNode(last)
            }
        }
    }
}
