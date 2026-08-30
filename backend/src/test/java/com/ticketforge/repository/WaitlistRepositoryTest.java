package com.ticketforge.repository;

import com.ticketforge.model.WaitlistEntry;
import com.ticketforge.model.WaitlistStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("dev")
class WaitlistRepositoryTest {

    @Autowired
    private WaitlistRepository waitlistRepository;

    @BeforeEach
    void setUp() {
        waitlistRepository.deleteAll();
    }

    @Test
    @DisplayName("Should retrieve waitlist entries ordered by Priority DESC, Timestamp ASC")
    void testWaitlistPriorityOrdering() {
        long baseTime = 1000000L;

        // User A: Priority 1, Timestamp 100
        waitlistRepository.save(WaitlistEntry.builder()
                .userId("user_A").priority(1).timestamp(baseTime + 100).status(WaitlistStatus.WAITING).build());

        // User B: Priority 3, Timestamp 300 (Higher priority than A, even though joined later)
        waitlistRepository.save(WaitlistEntry.builder()
                .userId("user_B").priority(3).timestamp(baseTime + 300).status(WaitlistStatus.WAITING).build());

        // User C: Priority 3, Timestamp 200 (Same priority as B, but joined earlier)
        waitlistRepository.save(WaitlistEntry.builder()
                .userId("user_C").priority(3).timestamp(baseTime + 200).status(WaitlistStatus.WAITING).build());

        // User D: Priority 2, Timestamp 50
        waitlistRepository.save(WaitlistEntry.builder()
                .userId("user_D").priority(2).timestamp(baseTime + 50).status(WaitlistStatus.WAITING).build());

        List<WaitlistEntry> ordered = waitlistRepository.findAllByStatusOrderByPriorityDescTimestampAsc(WaitlistStatus.WAITING);

        assertThat(ordered).hasSize(4);
        assertThat(ordered.get(0).getUserId()).isEqualTo("user_C"); // Priority 3, Time 200
        assertThat(ordered.get(1).getUserId()).isEqualTo("user_B"); // Priority 3, Time 300
        assertThat(ordered.get(2).getUserId()).isEqualTo("user_D"); // Priority 2, Time 50
        assertThat(ordered.get(3).getUserId()).isEqualTo("user_A"); // Priority 1, Time 100
    }

    @Test
    @DisplayName("Should enforce unique user in waitlist constraint")
    void testUniqueUserInWaitlist() {
        waitlistRepository.saveAndFlush(WaitlistEntry.builder()
                .userId("usr_dup").priority(1).timestamp(100L).status(WaitlistStatus.WAITING).build());

        assertThatThrownBy(() -> waitlistRepository.saveAndFlush(WaitlistEntry.builder()
                .userId("usr_dup").priority(2).timestamp(200L).status(WaitlistStatus.WAITING).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should delete waitlist entry by userId")
    void testDeleteByUserId() {
        waitlistRepository.saveAndFlush(WaitlistEntry.builder()
                .userId("usr_delete").priority(1).timestamp(100L).status(WaitlistStatus.WAITING).build());

        assertThat(waitlistRepository.findByUserId("usr_delete")).isPresent();

        waitlistRepository.deleteByUserId("usr_delete");
        waitlistRepository.flush();

        assertThat(waitlistRepository.findByUserId("usr_delete")).isEmpty();
    }
}
