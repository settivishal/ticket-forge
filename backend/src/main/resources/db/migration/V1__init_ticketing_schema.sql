-- ==========================================================
-- TicketForge Database Migration: V1 Initial Schema
-- Supports: PostgreSQL 16 & H2 (PostgreSQL compatibility mode)
-- ==========================================================

-- 1. Users Table (Stores user metadata and role hierarchy)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(32) NOT NULL DEFAULT 'ROLE_CUSTOMER',
    priority_tier INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Seats Table (Physical seat inventory)
CREATE TABLE IF NOT EXISTS seats (
    id BIGSERIAL PRIMARY KEY,
    seat_number INT NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    tier VARCHAR(32) NOT NULL DEFAULT 'STANDARD'
);

CREATE INDEX IF NOT EXISTS idx_seat_status ON seats(status);
CREATE INDEX IF NOT EXISTS idx_seat_number ON seats(seat_number);

-- 3. Reservations Table (Active bookings with Optimistic Locking Version)
CREATE TABLE IF NOT EXISTS reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    seat_id BIGINT NOT NULL UNIQUE REFERENCES seats(id) ON DELETE CASCADE,
    reserved_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_reservation_user ON reservations(user_id);
CREATE INDEX IF NOT EXISTS idx_reservation_expiry ON reservations(expires_at);

-- 4. Waitlist Entries Table (Priority Queue tracking with timestamp tie-breaking)
CREATE TABLE IF NOT EXISTS waitlist_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    priority INT NOT NULL DEFAULT 1,
    timestamp BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'WAITING'
);

CREATE INDEX IF NOT EXISTS idx_waitlist_priority_time ON waitlist_entries(priority DESC, timestamp ASC);
CREATE INDEX IF NOT EXISTS idx_waitlist_user ON waitlist_entries(user_id);
