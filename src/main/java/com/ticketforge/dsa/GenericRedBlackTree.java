package com.ticketforge.dsa;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Generic self-balancing Red-Black Tree implementation.
 * <p>
 * Guarantees O(log N) worst-case time complexity for search, insert, and delete operations.
 * Supports thread-safe concurrent reads and exclusive writes via ReentrantReadWriteLock.
 *
 * @param <K> The key type, which must implement {@link Comparable}
 * @param <V> The value type associated with the key
 */
public class GenericRedBlackTree<K extends Comparable<K>, V> {

    public static final boolean RED = true;
    public static final boolean BLACK = false;

    public static class Node<K, V> {
        private K key;
        private V value;
        private boolean color;
        private Node<K, V> left;
        private Node<K, V> right;
        private Node<K, V> parent;

        public Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.color = RED;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }

        public boolean isColor() {
            return color;
        }

        public Node<K, V> getLeft() {
            return left;
        }

        public Node<K, V> getRight() {
            return right;
        }

        public Node<K, V> getParent() {
            return parent;
        }

        @Override
        public String toString() {
            return "Node{" + "key=" + key + ", value=" + value + ", color=" + (color == RED ? "RED" : "BLACK") + '}';
        }
    }

    private Node<K, V> root;
    private int size = 0;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public GenericRedBlackTree() {
        this.root = null;
    }

    /**
     * Inserts or updates a key-value pair in the Red-Black Tree.
     * If the key already exists, updates the value and returns the old value.
     *
     * @param key   The key to insert
     * @param value The value to associate with the key
     * @return The previous value associated with key, or null if key was newly inserted
     */
    public V insert(K key, V value) {
        Objects.requireNonNull(key, "Key cannot be null");
        rwLock.writeLock().lock();
        try {
            Node<K, V> current = root;
            Node<K, V> parent = null;

            while (current != null) {
                parent = current;
                int cmp = key.compareTo(current.key);
                if (cmp == 0) {
                    V oldValue = current.value;
                    current.value = value;
                    return oldValue;
                } else if (cmp < 0) {
                    current = current.left;
                } else {
                    current = current.right;
                }
            }

            Node<K, V> newNode = new Node<>(key, value);
            newNode.parent = parent;

            if (parent == null) {
                root = newNode;
            } else if (key.compareTo(parent.key) < 0) {
                parent.left = newNode;
            } else {
                parent.right = newNode;
            }

            size++;
            rebalanceTreeAfterInsert(newNode);
            return null;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Left rotation to maintain binary tree invariants during balance adjustments.
     */
    private void rotateLeft(Node<K, V> node) {
        Node<K, V> rightChild = node.right;
        node.right = rightChild.left;

        if (rightChild.left != null) {
            rightChild.left.parent = node;
        }

        rightChild.parent = node.parent;

        if (node.parent == null) {
            root = rightChild;
        } else if (node == node.parent.left) {
            node.parent.left = rightChild;
        } else {
            node.parent.right = rightChild;
        }

        rightChild.left = node;
        node.parent = rightChild;
    }

    /**
     * Right rotation to maintain binary tree invariants during balance adjustments.
     */
    private void rotateRight(Node<K, V> node) {
        Node<K, V> leftChild = node.left;
        node.left = leftChild.right;

        if (leftChild.right != null) {
            leftChild.right.parent = node;
        }

        leftChild.parent = node.parent;

        if (node.parent == null) {
            root = leftChild;
        } else if (node == node.parent.right) {
            node.parent.right = leftChild;
        } else {
            node.parent.left = leftChild;
        }

        leftChild.right = node;
        node.parent = leftChild;
    }

    /**
     * Fixes Red-Black tree properties after node insertion.
     */
    private void rebalanceTreeAfterInsert(Node<K, V> node) {
        while (node != root && node.parent != null && node.parent.color == RED) {
            if (node.parent.parent == null) {
                break;
            }

            if (node.parent == node.parent.parent.left) {
                Node<K, V> uncle = node.parent.parent.right;

                if (uncle != null && uncle.color == RED) {
                    node.parent.color = BLACK;
                    uncle.color = BLACK;
                    node.parent.parent.color = RED;
                    node = node.parent.parent;
                } else {
                    if (node == node.parent.right) {
                        node = node.parent;
                        rotateLeft(node);
                    }
                    node.parent.color = BLACK;
                    node.parent.parent.color = RED;
                    rotateRight(node.parent.parent);
                }
            } else {
                Node<K, V> uncle = node.parent.parent.left;

                if (uncle != null && uncle.color == RED) {
                    node.parent.color = BLACK;
                    uncle.color = BLACK;
                    node.parent.parent.color = RED;
                    node = node.parent.parent;
                } else {
                    if (node == node.parent.left) {
                        node = node.parent;
                        rotateRight(node);
                    }
                    node.parent.color = BLACK;
                    node.parent.parent.color = RED;
                    rotateLeft(node.parent.parent);
                }
            }
        }
        if (root != null) {
            root.color = BLACK;
        }
    }

    /**
     * Searches for a node matching the specified key.
     *
     * @param key The key to search for
     * @return The Node if found, or null if key does not exist
     */
    public Node<K, V> findNode(K key) {
        if (key == null) return null;
        rwLock.readLock().lock();
        try {
            Node<K, V> current = root;
            while (current != null) {
                int cmp = key.compareTo(current.key);
                if (cmp == 0) {
                    return current;
                }
                current = cmp < 0 ? current.left : current.right;
            }
            return null;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Gets the value associated with the given key.
     *
     * @param key The key to look up
     * @return The value, or null if key not present
     */
    public V get(K key) {
        Node<K, V> node = findNode(key);
        return node != null ? node.value : null;
    }

    /**
     * Checks if the key exists in the tree.
     */
    public boolean containsKey(K key) {
        return findNode(key) != null;
    }

    /**
     * Deletes the node with the specified key from the tree.
     *
     * @param key The key of the node to remove
     * @return The value of the removed node, or null if not found
     */
    public V delete(K key) {
        if (key == null) return null;
        rwLock.writeLock().lock();
        try {
            Node<K, V> node = findNodeInternal(key);
            if (node == null) {
                return null;
            }

            V removedValue = node.value;
            deleteNodeInternal(node);
            size--;
            return removedValue;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private Node<K, V> findNodeInternal(K key) {
        Node<K, V> current = root;
        while (current != null) {
            int cmp = key.compareTo(current.key);
            if (cmp == 0) {
                return current;
            }
            current = cmp < 0 ? current.left : current.right;
        }
        return null;
    }

    private void deleteNodeInternal(Node<K, V> node) {
        Node<K, V> replacementChild;
        Node<K, V> nodeToRemove;

        if (node.left == null || node.right == null) {
            nodeToRemove = node;
        } else {
            nodeToRemove = successorNode(node);
        }

        if (nodeToRemove.left != null) {
            replacementChild = nodeToRemove.left;
        } else {
            replacementChild = nodeToRemove.right;
        }

        if (replacementChild != null) {
            replacementChild.parent = nodeToRemove.parent;
        }

        if (nodeToRemove.parent == null) {
            root = replacementChild;
        } else if (nodeToRemove == nodeToRemove.parent.left) {
            nodeToRemove.parent.left = replacementChild;
        } else {
            nodeToRemove.parent.right = replacementChild;
        }

        if (nodeToRemove != node) {
            node.key = nodeToRemove.key;
            node.value = nodeToRemove.value;
        }

        if (nodeToRemove.color == BLACK) {
            rebalanceTreeAfterDelete(replacementChild, nodeToRemove.parent);
        }
    }

    /**
     * Finds the in-order successor node in the tree.
     */
    private Node<K, V> successorNode(Node<K, V> node) {
        if (node.right != null) {
            Node<K, V> curr = node.right;
            while (curr.left != null) {
                curr = curr.left;
            }
            return curr;
        }

        Node<K, V> parent = node.parent;
        while (parent != null && node == parent.right) {
            node = parent;
            parent = parent.parent;
        }
        return parent;
    }

    /**
     * Rebalances the tree after node deletion.
     */
    private void rebalanceTreeAfterDelete(Node<K, V> node, Node<K, V> parent) {
        while (node != root && (node == null || node.color == BLACK)) {
            if (parent == null) break;

            if (node == parent.left) {
                Node<K, V> sibling = parent.right;

                if (sibling != null && sibling.color == RED) {
                    sibling.color = BLACK;
                    parent.color = RED;
                    rotateLeft(parent);
                    sibling = parent.right;
                }

                if (sibling == null || ((sibling.left == null || sibling.left.color == BLACK) &&
                        (sibling.right == null || sibling.right.color == BLACK))) {
                    if (sibling != null) {
                        sibling.color = RED;
                    }
                    node = parent;
                    parent = node.parent;
                } else {
                    if (sibling.right == null || sibling.right.color == BLACK) {
                        if (sibling.left != null) {
                            sibling.left.color = BLACK;
                        }
                        sibling.color = RED;
                        rotateRight(sibling);
                        sibling = parent.right;
                    }

                    if (sibling != null) {
                        sibling.color = parent.color;
                        if (sibling.right != null) {
                            sibling.right.color = BLACK;
                        }
                    }
                    parent.color = BLACK;
                    rotateLeft(parent);
                    node = root;
                }
            } else {
                Node<K, V> sibling = parent.left;

                if (sibling != null && sibling.color == RED) {
                    sibling.color = BLACK;
                    parent.color = RED;
                    rotateRight(parent);
                    sibling = parent.left;
                }

                if (sibling == null || ((sibling.right == null || sibling.right.color == BLACK) &&
                        (sibling.left == null || sibling.left.color == BLACK))) {
                    if (sibling != null) {
                        sibling.color = RED;
                    }
                    node = parent;
                    parent = node.parent;
                } else {
                    if (sibling.left == null || sibling.left.color == BLACK) {
                        if (sibling.right != null) {
                            sibling.right.color = BLACK;
                        }
                        sibling.color = RED;
                        rotateLeft(sibling);
                        sibling = parent.left;
                    }

                    if (sibling != null) {
                        sibling.color = parent.color;
                        if (sibling.left != null) {
                            sibling.left.color = BLACK;
                        }
                    }
                    parent.color = BLACK;
                    rotateRight(parent);
                    node = root;
                }
            }
        }
        if (node != null) {
            node.color = BLACK;
        }
    }

    /**
     * Performs an in-order traversal of the tree, returning elements sorted by key.
     *
     * @return Ordered list of nodes
     */
    public List<Node<K, V>> inorderTraversal() {
        rwLock.readLock().lock();
        try {
            List<Node<K, V>> result = new ArrayList<>(size);
            inorderTraversalHelper(root, result);
            return result;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private void inorderTraversalHelper(Node<K, V> node, List<Node<K, V>> result) {
        if (node != null) {
            inorderTraversalHelper(node.left, result);
            result.add(node);
            inorderTraversalHelper(node.right, result);
        }
    }

    /**
     * Finds all nodes whose keys fall within the range [fromKey, toKey] inclusive.
     * Runs in O(log N + K) time complexity where K is the number of elements in range.
     *
     * @param fromKey Lower bound key (inclusive)
     * @param toKey   Upper bound key (inclusive)
     * @return Sorted list of nodes in the specified key range
     */
    public List<Node<K, V>> findRange(K fromKey, K toKey) {
        Objects.requireNonNull(fromKey, "fromKey cannot be null");
        Objects.requireNonNull(toKey, "toKey cannot be null");
        rwLock.readLock().lock();
        try {
            List<Node<K, V>> result = new ArrayList<>();
            findRangeHelper(root, fromKey, toKey, result);
            return result;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private void findRangeHelper(Node<K, V> node, K fromKey, K toKey, List<Node<K, V>> result) {
        if (node == null) return;

        if (node.key.compareTo(fromKey) > 0) {
            findRangeHelper(node.left, fromKey, toKey, result);
        }

        if (node.key.compareTo(fromKey) >= 0 && node.key.compareTo(toKey) <= 0) {
            result.add(node);
        }

        if (node.key.compareTo(toKey) < 0) {
            findRangeHelper(node.right, fromKey, toKey, result);
        }
    }

    public int size() {
        rwLock.readLock().lock();
        try {
            return size;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void clear() {
        rwLock.writeLock().lock();
        try {
            root = null;
            size = 0;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public Node<K, V> getRoot() {
        return root;
    }
}
