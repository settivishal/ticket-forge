package com.ticketforge.service;

import com.ticketforge.dto.ReservationResponse;
import com.ticketforge.dto.SeatResponse;
import com.ticketforge.dto.SystemStatusResponse;
import com.ticketforge.dto.WaitlistResponse;

import java.util.List;

public interface TicketForgeService {

    /**
     * Initializes the venue with the given number of seats.
     * Clears all prior seat states, reservations, and waitlist entries.
     */
    void initializeSeats(int seatCount);

    /**
     * Attempts to reserve a seat for a user.
     * If seats are available, assigns the lowest seat number and confirms the reservation.
     * If all seats are occupied, adds the user to the priority waitlist.
     *
     * @param userId   User ID
     * @param priority User priority level (higher value = higher priority)
     * @return ReservationResponse if a seat was allocated, or null if placed on waitlist
     */
    ReservationResponse reserveSeat(String userId, int priority);

    /**
     * Holds a seat with a Time-to-Live (TTL) expiration window.
     */
    ReservationResponse holdSeat(String userId, int priority, int ttlSeconds);

    /**
     * Cancels a user's reservation for the specified seat.
     * Automatically cascades seat re-allocation to the highest-priority waitlist customer,
     * or returns the seat to the available inventory if the waitlist is empty.
     */
    void cancelReservation(int seatNumber, String userId);

    /**
     * Removes a user from the priority waitlist.
     *
     * @return true if user was found and removed, false otherwise
     */
    boolean exitWaitlist(String userId);

    /**
     * Dynamically updates the priority level of a waiting customer in O(log N) time.
     *
     * @return true if updated, false if user is not in waitlist
     */
    boolean updatePriority(String userId, int newPriority);

    /**
     * Expands venue capacity by adding more seats.
     * Newly created seats immediately fulfill waiting customers in priority order.
     */
    void addSeats(int count);

    /**
     * Batch releases reservations for users in the range [fromUserId, toUserId].
     * Promotes waiting users to fill the newly freed seats.
     *
     * @return List of released seat numbers
     */
    List<Integer> releaseSeats(String fromUserId, String toUserId);

    /**
     * Returns a real-time aggregate summary of total, available, held, reserved seats and waitlist size.
     */
    SystemStatusResponse getSystemStatus();

    /**
     * Returns all seats with current statuses and assigned user IDs.
     */
    List<SeatResponse> getAllSeats();

    /**
     * Returns all active confirmed and held reservations.
     */
    List<ReservationResponse> getAllReservations();

    /**
     * Returns the active waitlist queue ordered by priority (highest first) and timestamp.
     */
    List<WaitlistResponse> getWaitlist();

    /**
     * Retrieves status details for a specific seat number.
     */
    SeatResponse getSeatByNumber(int seatNumber);

    /**
     * Retrieves the reservation held by a specific user.
     */
    ReservationResponse getReservationByUserId(String userId);

    /**
     * Hydrates the in-memory DSA caches (RedBlackTree & MinHeap) from persistent PostgreSQL storage.
     */
    void syncFromDatabase();
}
