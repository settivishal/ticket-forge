package com.ticketforge.dsa;

import com.ticketforge.model.WaitlistEntry;
import com.ticketforge.model.WaitlistStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenericMinHeapTest {

    private GenericMinHeap<Integer> integerHeap;

    @BeforeEach
    void setUp() {
        integerHeap = new GenericMinHeap<>();
    }

    @Test
    @DisplayName("Should insert elements and extract min in ascending order")
    void testInsertAndExtractMin() {
        int[] values = {45, 12, 89, 3, 34, 67, 90, 23};
        for (int v : values) {
            integerHeap.insert(v);
        }

        assertThat(integerHeap.size()).isEqualTo(8);
        assertThat(integerHeap.peek()).isEqualTo(3);

        assertThat(integerHeap.extractMin()).isEqualTo(3);
        assertThat(integerHeap.extractMin()).isEqualTo(12);
        assertThat(integerHeap.extractMin()).isEqualTo(23);
        assertThat(integerHeap.extractMin()).isEqualTo(34);
        assertThat(integerHeap.extractMin()).isEqualTo(45);
        assertThat(integerHeap.extractMin()).isEqualTo(67);
        assertThat(integerHeap.extractMin()).isEqualTo(89);
        assertThat(integerHeap.extractMin()).isEqualTo(90);
        assertThat(integerHeap.extractMin()).isNull();
        assertThat(integerHeap.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should remove arbitrary element by key in O(log N) and rebalance heap")
    void testRemoveById() {
        integerHeap.insert(10);
        integerHeap.insert(20);
        integerHeap.insert(5);
        integerHeap.insert(15);
        integerHeap.insert(30);

        assertThat(integerHeap.removeById(5)).isTrue();
        assertThat(integerHeap.peek()).isEqualTo(10);
        assertThat(integerHeap.size()).isEqualTo(4);

        assertThat(integerHeap.removeById(999)).isFalse();
        assertThat(integerHeap.size()).isEqualTo(4);

        assertThat(integerHeap.removeById(30)).isTrue();
        assertThat(integerHeap.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should correctly prioritize WaitlistEntry objects (Priority DESC, Timestamp ASC)")
    void testWaitlistEntryPriority() {
        GenericMinHeap<WaitlistEntry> waitlistHeap = new GenericMinHeap<>(WaitlistEntry::getUserId);

        long baseTime = 100000L;

        WaitlistEntry userA = WaitlistEntry.builder()
                .userId("user_A").priority(1).timestamp(baseTime + 100).status(WaitlistStatus.WAITING).build();
        WaitlistEntry userB = WaitlistEntry.builder()
                .userId("user_B").priority(3).timestamp(baseTime + 300).status(WaitlistStatus.WAITING).build();
        WaitlistEntry userC = WaitlistEntry.builder()
                .userId("user_C").priority(3).timestamp(baseTime + 200).status(WaitlistStatus.WAITING).build();
        WaitlistEntry userD = WaitlistEntry.builder()
                .userId("user_D").priority(2).timestamp(baseTime + 50).status(WaitlistStatus.WAITING).build();

        waitlistHeap.insert(userA);
        waitlistHeap.insert(userB);
        waitlistHeap.insert(userC);
        waitlistHeap.insert(userD);

        assertThat(waitlistHeap.size()).isEqualTo(4);

        // Verification of extract min order (Higher priority first, earlier timestamp on tie)
        assertThat(waitlistHeap.extractMin().getUserId()).isEqualTo("user_C"); // Priority 3, Time 200
        assertThat(waitlistHeap.extractMin().getUserId()).isEqualTo("user_B"); // Priority 3, Time 300
        assertThat(waitlistHeap.extractMin().getUserId()).isEqualTo("user_D"); // Priority 2, Time 50
        assertThat(waitlistHeap.extractMin().getUserId()).isEqualTo("user_A"); // Priority 1, Time 100
        assertThat(waitlistHeap.extractMin()).isNull();
    }

    @Test
    @DisplayName("Should update existing waitlist entry priority in O(log N) and rebalance")
    void testUpdatePriority() {
        GenericMinHeap<WaitlistEntry> waitlistHeap = new GenericMinHeap<>(WaitlistEntry::getUserId);
        long baseTime = 100000L;

        WaitlistEntry userA = WaitlistEntry.builder()
                .userId("user_A").priority(1).timestamp(baseTime + 100).status(WaitlistStatus.WAITING).build();
        WaitlistEntry userB = WaitlistEntry.builder()
                .userId("user_B").priority(2).timestamp(baseTime + 200).status(WaitlistStatus.WAITING).build();

        waitlistHeap.insert(userA);
        waitlistHeap.insert(userB);

        // Currently userB (priority 2) is at the root
        assertThat(waitlistHeap.peek().getUserId()).isEqualTo("user_B");

        // Upgrade userA priority from 1 to 5 (VIP)
        WaitlistEntry upgradedA = WaitlistEntry.builder()
                .userId("user_A").priority(5).timestamp(baseTime + 100).status(WaitlistStatus.WAITING).build();
        boolean updated = waitlistHeap.update(upgradedA);
        assertThat(updated).isTrue();

        // Now userA must be promoted to the root!
        assertThat(waitlistHeap.peek().getUserId()).isEqualTo("user_A");
        assertThat(waitlistHeap.extractMin().getUserId()).isEqualTo("user_A");
        assertThat(waitlistHeap.extractMin().getUserId()).isEqualTo("user_B");
    }

    @Test
    @DisplayName("Should return sorted list snapshot without mutating heap")
    void testToSortedList() {
        integerHeap.insert(50);
        integerHeap.insert(10);
        integerHeap.insert(30);

        List<Integer> sorted = integerHeap.toSortedList();
        assertThat(sorted).containsExactly(10, 30, 50);

        // Original heap remains intact
        assertThat(integerHeap.size()).isEqualTo(3);
        assertThat(integerHeap.peek()).isEqualTo(10);
    }
}
