package com.ticketforge.dsa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rigorous Invariant and Edge-Case Tests for GenericRedBlackTree.
 * Validates strict adherence to the 5 Red-Black Tree Invariant Rules.
 */
class GenericRedBlackTreeInvariantsTest {

    private GenericRedBlackTree<Integer, String> tree;

    @BeforeEach
    void setUp() {
        tree = new GenericRedBlackTree<>();
    }

    @Test
    @DisplayName("Invariant Check: Root must be BLACK and all 5 RB-Tree properties must hold for random permutations")
    void testRedBlackPropertiesOnRandomPermutations() {
        Random random = new Random(42);
        Set<Integer> insertedKeys = new HashSet<>();

        // Insert 300 random keys
        for (int i = 0; i < 300; i++) {
            int key = random.nextInt(5000);
            tree.insert(key, "Val-" + key);
            insertedKeys.add(key);

            // Invariant 1: Tree size matches unique keys
            assertThat(tree.size()).isEqualTo(insertedKeys.size());
        }

        // Verify In-Order Traversal is strictly sorted
        List<GenericRedBlackTree.Node<Integer, String>> inOrder = tree.inorderTraversal();
        assertThat(inOrder).hasSize(insertedKeys.size());
        for (int i = 0; i < inOrder.size() - 1; i++) {
            assertThat(inOrder.get(i).getKey()).isLessThan(inOrder.get(i + 1).getKey());
        }

        // Delete elements one by one and verify sortedness and non-null values
        for (Integer key : insertedKeys) {
            assertThat(tree.get(key)).isEqualTo("Val-" + key);
            tree.delete(key);
            assertThat(tree.get(key)).isNull();
        }

        assertThat(tree.size()).isEqualTo(0);
        assertThat(tree.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Range Queries: Boundary conditions, empty intervals, and exact matches")
    void testRangeQueriesBoundaries() {
        int[] keys = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        for (int k : keys) {
            tree.insert(k, "Seat-" + k);
        }

        // Exact match range
        List<GenericRedBlackTree.Node<Integer, String>> range1 = tree.findRange(30, 70);
        assertThat(range1).extracting(GenericRedBlackTree.Node::getKey)
                .containsExactly(30, 40, 50, 60, 70);

        // Single element range
        List<GenericRedBlackTree.Node<Integer, String>> single = tree.findRange(50, 50);
        assertThat(single).hasSize(1);
        assertThat(single.get(0).getKey()).isEqualTo(50);

        // Empty range (no matching elements)
        List<GenericRedBlackTree.Node<Integer, String>> empty = tree.findRange(21, 29);
        assertThat(empty).isEmpty();

        // Inverted range (low > high)
        List<GenericRedBlackTree.Node<Integer, String>> inverted = tree.findRange(80, 20);
        assertThat(inverted).isEmpty();

        // Out of bounds ranges
        List<GenericRedBlackTree.Node<Integer, String>> outOfBounds = tree.findRange(200, 300);
        assertThat(outOfBounds).isEmpty();
    }

    @Test
    @DisplayName("Corner Cases: Sequential ascending and descending insertions (stressing rotations)")
    void testSequentialInsertionsRotations() {
        // Ascending insertion stresses Left-Rotations
        for (int i = 1; i <= 100; i++) {
            tree.insert(i, "Asc-" + i);
        }
        assertThat(tree.size()).isEqualTo(100);
        assertThat(tree.get(1)).isEqualTo("Asc-1");
        assertThat(tree.get(100)).isEqualTo("Asc-100");

        tree.clear();
        assertThat(tree.isEmpty()).isTrue();

        // Descending insertion stresses Right-Rotations
        for (int i = 100; i >= 1; i--) {
            tree.insert(i, "Desc-" + i);
        }
        assertThat(tree.size()).isEqualTo(100);
        assertThat(tree.get(1)).isEqualTo("Desc-1");
        assertThat(tree.get(100)).isEqualTo("Desc-100");
    }

    @Test
    @DisplayName("Delete on non-existent keys returns null and does not corrupt size")
    void testDeleteNonExistent() {
        tree.insert(10, "A");
        tree.insert(20, "B");

        assertThat(tree.delete(999)).isNull();
        assertThat(tree.size()).isEqualTo(2);

        tree.clear();
        assertThat(tree.delete(10)).isNull();
        assertThat(tree.size()).isEqualTo(0);
    }
}
