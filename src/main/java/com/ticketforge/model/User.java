package com.ticketforge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class User {

    @Id
    @Column(nullable = false, length = 64)
    private String id; // Supabase Auth UUID or custom User ID

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String role = "ROLE_CUSTOMER";

    @Column(name = "priority_tier", nullable = false)
    @Builder.Default
    private Integer priorityTier = 1;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
