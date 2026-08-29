package com.ticketforge.dsa;

import com.ticketforge.model.WaitlistEntry;
import com.ticketforge.model.WaitlistStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Advanced Edge-Case and Priority Mutation Tests for Indexed GenericMinHeap.
 */
class GenericMinHeapAdvancedTest {

    private GenericMinHeap<WaitlistEntry> waitlistHeap;

    @BeforeEach
    void setUp() {
        waitlistHeap = new GenericMinHeap<>(WaitlistEntry::getUserId);
    }

    @Test
    @DisplayName("FIFO Tie-Breaking: Entries with identical priority maintain strict FIFO order")
    void testIdenticalPriorityFifoOrdering() {
        WaitlistEntry e1 = WaitlistEntry.builder().userId("usr_1").priority(2).timestamp(1000L).status(WaitlistStatus.WAITING).build();
        WaitlistEntry e2 = WaitlistEntry.builder().userId("usr_2").priority(2).timestamp(2000L).status(WaitlistStatus.WAITING).build();
        WaitlistEntry e3 = WaitlistEntry.builder().userId("usr_3").priority(2).timestamp(3000L).status(WaitlistStatus.WAITING).build();

        waitlistHeap.insert(e2);
        waitlistHeap.insert(e3);
        waitlistHeap.insert(e1);

        assertThat(waitlistHeap.extractMin().getUserId()).isEqualTo("usr_1");
        assertThat(waitlistHeap.extractMin().getUserId()).isEqualTo("usr_2");
        assertThat(waitlistHeap.extractMin().getUserId()).isEqualTo("usr_3");
    }

    @Test
    @DisplayName("Dynamic Priority Mutation: Promoting lower priority user instantly bubbles up to top")
    void testDynamicPriorityPromotion() {
        WaitlistEntry standard = WaitlistEntry.builder().userId("usr_std").priority(1).timestamp(1000L).status(WaitlistStatus.WAITING).build();
        WaitlistEntry premium = WaitlistEntry.builder().userId("usr_prem").priority(2).timestamp(2000L).status(WaitlistStatus.WAITING).build();

        waitlistHeap.insert(standard);
        waitlistHeap.insert(premium);

        // Premium is currently top (P=2 is prioritized over P=1)
        assertThat(waitlistHeap.peek().getUserId()).isEqualTo("usr_prem");

        // Promote standard user from Tier 1 to Tier 3 (VIP)
        WaitlistEntry vip = WaitlistEntry.builder().userId("usr_std").priority(3).timestamp(1000L).status(WaitlistStatus.WAITING).build();
        waitlistHeap.insert(vip); // update existing key

        // Standard user (now VIP) must be top of the heap
        assertThat(waitlistHeap.peek().getUserId()).isEqualTo("usr_std");
        assertThat(waitlistHeap.extractMin().getUserId()).isEqualTo("usr_std");
        assertThat(waitlistHeap.extractMin().getUserId()).isEqualTo("usr_prem");
    }

    @Test
    @DisplayName("Corner Cases: ExtractMin on empty heap returns null and toList returns empty")
    void testEmptyHeapOperations() {
        assertThat(waitlistHeap.isEmpty()).isTrue();
        assertThat(waitlistHeap.peek()).isNull();
        assertThat(waitlistHeap.extractMin()).isNull();
        assertThat(waitlistHeap.toList()).isEmpty();
        assertThat(waitlistHeap.size()).isEqualTo(0);

        // Removing non-existent entry returns false
        assertThat(waitlistHeap.removeById("ghost")).isFalse();
    }

    @Test
    @DisplayName("Batch clear and reconstruction")
    void testClearAndReconstruct() {
        for (int i = 1; i <= 20; i++) {
            waitlistHeap.insert(WaitlistEntry.builder()
                    .userId("usr_" + i)
                    .priority((i % 3) + 1)
                    .timestamp(System.currentTimeMillis() + i)
                    .status(WaitlistStatus.WAITING)
                    .build());
        }

        assertThat(waitlistHeap.size()).isEqualTo(20);
        waitlistHeap.clear();
        assertThat(waitlistHeap.isEmpty()).isTrue();
        assertThat(waitlistHeap.size()).isEqualTo(0);

        // Re-insert after clear
        waitlistHeap.insert(WaitlistEntry.builder()
                .userId("usr_fresh")
                .priority(3)
                .timestamp(System.currentTimeMillis())
                .status(WaitlistStatus.WAITING)
                .build());
        assertThat(waitlistHeap.size()).isEqualTo(1);
        assertThat(waitlistHeap.peek().getUserId()).isEqualTo("usr_fresh");
    }
}
