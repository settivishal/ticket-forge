package com.ticketforge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "waitlist_entries", indexes = {
    @Index(name = "idx_waitlist_priority_time", columnList = "priority DESC, timestamp ASC"),
    @Index(name = "idx_waitlist_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WaitlistEntry implements Comparable<WaitlistEntry> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 64)
    private String userId;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 1;

    @Column(nullable = false)
    private Long timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private WaitlistStatus status = WaitlistStatus.WAITING;

    @Override
    public int compareTo(WaitlistEntry other) {
        if (other == null) {
            return -1;
        }
        // Higher priority value first (e.g. 3 before 1)
        int priorityComparison = other.priority.compareTo(this.priority);
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        // Earlier timestamp first (FIFO tie-breaking)
        return Long.compare(this.timestamp, other.timestamp);
    }
}
