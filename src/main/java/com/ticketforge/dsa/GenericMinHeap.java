package com.ticketforge.dsa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Generic indexed binary Min-Heap data structure.
 * <p>
 * Maintains heap-order invariants where the smallest element (by natural {@link Comparable} order)
 * is always at the root (index 0).
 * <p>
 * An internal hash-indexed map enables O(1) element lookup and O(log N) arbitrary element removal
 * and dynamic priority updates.
 *
 * @param <T> Element type, which must implement {@link Comparable}
 */
public class GenericMinHeap<T extends Comparable<T>> {

    private final ArrayList<T> heap;
    private final Map<Object, Integer> indexMap;
    private final Function<T, Object> keyExtractor;
    private final ReentrantReadWriteLock rwLock;

    /**
     * Creates a Min-Heap where elements themselves serve as their unique identifiers.
     */
    public GenericMinHeap() {
        this(item -> (Object) item);
    }

    /**
     * Creates a Min-Heap with a custom key extractor function for identifying elements.
     *
     * @param keyExtractor Function to extract unique identity from element (e.g. WaitlistEntry::getUserId)
     */
    public GenericMinHeap(Function<T, Object> keyExtractor) {
        this.heap = new ArrayList<>();
        this.indexMap = new HashMap<>();
        this.keyExtractor = Objects.requireNonNull(keyExtractor, "Key extractor cannot be null");
        this.rwLock = new ReentrantReadWriteLock();
    }

    /**
     * Inserts an item into the heap.
     *
     * @param item The element to insert
     */
    public void insert(T item) {
        Objects.requireNonNull(item, "Cannot insert null into heap");
        rwLock.writeLock().lock();
        try {
            Object key = keyExtractor.apply(item);
            if (indexMap.containsKey(key)) {
                // If element already exists, update its value and heapify
                update(item);
                return;
            }

            heap.add(item);
            int current = heap.size() - 1;
            indexMap.put(key, current);
            promoteElement(current);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Extracts and removes the minimum element from the root of the heap.
     *
     * @return The minimum element, or null if the heap is empty
     */
    public T extractMin() {
        rwLock.writeLock().lock();
        try {
            if (heap.isEmpty()) {
                return null;
            }

            T min = heap.get(0);
            indexMap.remove(keyExtractor.apply(min));

            int lastIdx = heap.size() - 1;
            if (lastIdx > 0) {
                T lastItem = heap.get(lastIdx);
                heap.set(0, lastItem);
                indexMap.put(keyExtractor.apply(lastItem), 0);
            }

            heap.remove(lastIdx);

            if (!heap.isEmpty()) {
                demoteElement(0);
            }

            return min;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Peeks at the minimum element without removing it.
     *
     * @return The minimum element, or null if empty
     */
    public T peek() {
        rwLock.readLock().lock();
        try {
            return heap.isEmpty() ? null : heap.get(0);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Removes an element by its unique identifier in O(log N) time.
     *
     * @param key Identifier of the element to remove
     * @return true if the element was found and removed, false otherwise
     */
    public boolean removeById(Object key) {
        if (key == null) return false;
        rwLock.writeLock().lock();
        try {
            Integer index = indexMap.get(key);
            if (index == null) {
                return false;
            }

            int lastIdx = heap.size() - 1;
            if (index == lastIdx) {
                heap.remove(lastIdx);
                indexMap.remove(key);
                return true;
            }

            swap(index, lastIdx);
            heap.remove(lastIdx);
            indexMap.remove(key);

            if (index < heap.size()) {
                int parent = (index - 1) / 2;
                if (index > 0 && heap.get(index).compareTo(heap.get(parent)) < 0) {
                    promoteElement(index);
                } else {
                    demoteElement(index);
                }
            }
            return true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Updates an existing element in the heap, maintaining the heap invariant.
     *
     * @param newItem Updated element
     * @return true if element was found and updated, false otherwise
     */
    public boolean update(T newItem) {
        Objects.requireNonNull(newItem, "Updated item cannot be null");
        rwLock.writeLock().lock();
        try {
            Object key = keyExtractor.apply(newItem);
            Integer index = indexMap.get(key);
            if (index == null) {
                return false;
            }

            heap.set(index, newItem);

            if (index > 0 && heap.get(index).compareTo(heap.get((index - 1) / 2)) < 0) {
                promoteElement(index);
            } else {
                demoteElement(index);
            }
            return true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Finds an element by its identifier in O(1) time.
     *
     * @param key Identifier to find
     * @return The element, or null if not present
     */
    public T get(Object key) {
        if (key == null) return null;
        rwLock.readLock().lock();
        try {
            Integer index = indexMap.get(key);
            return index != null ? heap.get(index) : null;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Checks if an element with the given identifier exists in the heap in O(1) time.
     */
    public boolean containsId(Object key) {
        if (key == null) return false;
        rwLock.readLock().lock();
        try {
            return indexMap.containsKey(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Bubbles up the element at the given index to restore the heap property.
     */
    private void promoteElement(int current) {
        while (current > 0) {
            int parent = (current - 1) / 2;
            if (heap.get(current).compareTo(heap.get(parent)) >= 0) {
                break;
            }
            swap(current, parent);
            current = parent;
        }
    }

    /**
     * Trickles down the element at the given index to restore the heap property.
     */
    private void demoteElement(int index) {
        int heapSize = heap.size();
        while (true) {
            int smallest = index;
            int leftChild = 2 * index + 1;
            int rightChild = 2 * index + 2;

            if (leftChild < heapSize && heap.get(leftChild).compareTo(heap.get(smallest)) < 0) {
                smallest = leftChild;
            }

            if (rightChild < heapSize && heap.get(rightChild).compareTo(heap.get(smallest)) < 0) {
                smallest = rightChild;
            }

            if (smallest == index) {
                break;
            }

            swap(index, smallest);
            index = smallest;
        }
    }

    /**
     * Swaps two elements in the heap array and synchronizes their positions in the index map.
     */
    private void swap(int i, int j) {
        T temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);

        indexMap.put(keyExtractor.apply(heap.get(i)), i);
        indexMap.put(keyExtractor.apply(heap.get(j)), j);
    }

    /**
     * Returns an unmodifiable snapshot of the elements currently in the heap.
     */
    public List<T> toList() {
        rwLock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(heap));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Returns a sorted copy of all elements in ascending priority order.
     */
    public List<T> toSortedList() {
        rwLock.readLock().lock();
        try {
            List<T> copy = new ArrayList<>(heap);
            Collections.sort(copy);
            return copy;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int size() {
        rwLock.readLock().lock();
        try {
            return heap.size();
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
            heap.clear();
            indexMap.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
