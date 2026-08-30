package com.ticketforge.dsa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class GenericRedBlackTreeTest {

    private GenericRedBlackTree<Integer, String> tree;

    @BeforeEach
    void setUp() {
        tree = new GenericRedBlackTree<>();
    }

    @Test
    @DisplayName("Should insert elements and retrieve by key in O(log N)")
    void testInsertAndGet() {
        tree.insert(50, "Fifty");
        tree.insert(25, "Twenty-Five");
        tree.insert(75, "Seventy-Five");
        tree.insert(10, "Ten");

        assertThat(tree.size()).isEqualTo(4);
        assertThat(tree.get(50)).isEqualTo("Fifty");
        assertThat(tree.get(25)).isEqualTo("Twenty-Five");
        assertThat(tree.get(75)).isEqualTo("Seventy-Five");
        assertThat(tree.get(10)).isEqualTo("Ten");
        assertThat(tree.get(999)).isNull();
    }

    @Test
    @DisplayName("Should update existing key value and return old value")
    void testUpdateExistingKey() {
        tree.insert(10, "Initial");
        String old = tree.insert(10, "Updated");

        assertThat(old).isEqualTo("Initial");
        assertThat(tree.get(10)).isEqualTo("Updated");
        assertThat(tree.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should perform in-order traversal returning sorted keys")
    void testInorderTraversal() {
        int[] keys = {45, 12, 89, 3, 34, 67, 90, 23};
        for (int k : keys) {
            tree.insert(k, "Val-" + k);
        }

        List<GenericRedBlackTree.Node<Integer, String>> inOrder = tree.inorderTraversal();
        assertThat(inOrder).hasSize(8);

        for (int i = 0; i < inOrder.size() - 1; i++) {
            assertThat(inOrder.get(i).getKey()).isLessThan(inOrder.get(i + 1).getKey());
        }
    }

    @Test
    @DisplayName("Should find range [fromKey, toKey] inclusive in O(log N + K)")
    void testFindRange() {
        for (int i = 10; i <= 100; i += 10) {
            tree.insert(i, "Seat-" + i);
        }

        List<GenericRedBlackTree.Node<Integer, String>> range = tree.findRange(30, 70);
        assertThat(range).extracting(GenericRedBlackTree.Node::getKey)
                .containsExactly(30, 40, 50, 60, 70);

        List<GenericRedBlackTree.Node<Integer, String>> singleRange = tree.findRange(50, 50);
        assertThat(singleRange).extracting(GenericRedBlackTree.Node::getKey).containsExactly(50);

        List<GenericRedBlackTree.Node<Integer, String>> outOfRange = tree.findRange(200, 300);
        assertThat(outOfRange).isEmpty();
    }

    @Test
    @DisplayName("Should delete leaf, single-child, and two-children nodes while preserving invariants")
    void testDeletions() {
        int[] keys = {50, 20, 70, 10, 30, 60, 80, 5, 15, 25, 35};
        for (int k : keys) {
            tree.insert(k, "Val-" + k);
        }

        // Delete leaf node (5)
        String removed = tree.delete(5);
        assertThat(removed).isEqualTo("Val-5");
        assertThat(tree.containsKey(5)).isFalse();
        assertThat(tree.size()).isEqualTo(10);
        assertRedBlackInvariants(tree.getRoot());

        // Delete node with 2 children (20)
        removed = tree.delete(20);
        assertThat(removed).isEqualTo("Val-20");
        assertThat(tree.containsKey(20)).isFalse();
        assertThat(tree.size()).isEqualTo(9);
        assertRedBlackInvariants(tree.getRoot());

        // Delete root node (50)
        removed = tree.delete(50);
        assertThat(removed).isEqualTo("Val-50");
        assertThat(tree.containsKey(50)).isFalse();
        assertThat(tree.size()).isEqualTo(8);
        assertRedBlackInvariants(tree.getRoot());
    }

    @Test
    @DisplayName("Should maintain Red-Black invariants across large randomized insertions and deletions")
    void testLargeRandomizedDataset() {
        Random random = new Random(42);
        for (int i = 0; i < 500; i++) {
            int key = random.nextInt(10000);
            tree.insert(key, "Val-" + key);
            assertRedBlackInvariants(tree.getRoot());
        }

        List<GenericRedBlackTree.Node<Integer, String>> nodes = tree.inorderTraversal();
        for (int i = 0; i < Math.min(250, nodes.size()); i++) {
            int keyToRemove = nodes.get(i).getKey();
            tree.delete(keyToRemove);
            assertRedBlackInvariants(tree.getRoot());
        }
    }

    /**
     * Helper asserting Red-Black Tree properties:
     * 1. Root is BLACK
     * 2. No RED node has a RED child
     * 3. Equal black height on all paths from root to leaves
     */
    private <K extends Comparable<K>, V> void assertRedBlackInvariants(GenericRedBlackTree.Node<K, V> root) {
        if (root == null) return;

        // 1. Root property
        assertThat(root.isColor()).as("Root must be BLACK").isEqualTo(GenericRedBlackTree.BLACK);

        // 2. Red property & 3. Black height property
        checkNodeProperties(root);
    }

    private <K extends Comparable<K>, V> int checkNodeProperties(GenericRedBlackTree.Node<K, V> node) {
        if (node == null) {
            return 1; // Leaf null counts as black node (height 1)
        }

        // Red property: If node is RED, both children must be BLACK
        if (node.isColor() == GenericRedBlackTree.RED) {
            if (node.getLeft() != null) {
                assertThat(node.getLeft().isColor()).as("Left child of red node must be black").isEqualTo(GenericRedBlackTree.BLACK);
            }
            if (node.getRight() != null) {
                assertThat(node.getRight().isColor()).as("Right child of red node must be black").isEqualTo(GenericRedBlackTree.BLACK);
            }
        }

        int leftBlackHeight = checkNodeProperties(node.getLeft());
        int rightBlackHeight = checkNodeProperties(node.getRight());

        assertThat(leftBlackHeight).as("Black height must be equal on both subtrees")
                .isEqualTo(rightBlackHeight);

        return leftBlackHeight + (node.isColor() == GenericRedBlackTree.BLACK ? 1 : 0);
    }
}
