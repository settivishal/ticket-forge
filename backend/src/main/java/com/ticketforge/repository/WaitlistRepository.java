package com.ticketforge.repository;

import com.ticketforge.model.WaitlistEntry;
import com.ticketforge.model.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    Optional<WaitlistEntry> findByUserId(String userId);

    long countByStatus(WaitlistStatus status);

    List<WaitlistEntry> findAllByStatusOrderByPriorityDescTimestampAsc(WaitlistStatus status);

    void deleteByUserId(String userId);

    boolean existsByUserId(String userId);

    boolean existsByUserIdAndStatus(String userId, WaitlistStatus status);
}
