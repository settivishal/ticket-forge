package com.ticketforge.service;

import com.ticketforge.config.RedisConfig;
import com.ticketforge.dsa.GenericMinHeap;
import com.ticketforge.dsa.GenericRedBlackTree;
import com.ticketforge.dto.ReservationResponse;
import com.ticketforge.dto.SeatResponse;
import com.ticketforge.dto.SystemStatusResponse;
import com.ticketforge.dto.WaitlistResponse;
import com.ticketforge.exception.InvalidRequestException;
import com.ticketforge.exception.ReservationNotFoundException;
import com.ticketforge.exception.SeatNotFoundException;
import com.ticketforge.exception.UserAlreadyInWaitlistException;
import com.ticketforge.exception.UserAlreadyReservedException;
import com.ticketforge.model.Reservation;
import com.ticketforge.model.Seat;
import com.ticketforge.model.SeatStatus;
import com.ticketforge.model.SeatTier;
import com.ticketforge.model.WaitlistEntry;
import com.ticketforge.model.WaitlistStatus;
import com.ticketforge.repository.ReservationRepository;
import com.ticketforge.repository.SeatRepository;
import com.ticketforge.repository.UserRepository;
import com.ticketforge.repository.WaitlistRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketForgeServiceImpl implements TicketForgeService {

    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final WaitlistRepository waitlistRepository;
    private final UserRepository userRepository;

    // In-memory DSA Caching & Matching Engine
    private final GenericRedBlackTree<String, Integer> reservationsTree = new GenericRedBlackTree<>();
    private final GenericMinHeap<Integer> availableSeatsHeap = new GenericMinHeap<>();
    private final GenericMinHeap<WaitlistEntry> waitlistHeap = new GenericMinHeap<>(WaitlistEntry::getUserId);

    @PostConstruct
    @Override
    @Transactional(readOnly = true)
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_SYSTEM_STATUS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEATS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEAT, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_WAITLIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_RESERVATIONS, allEntries = true)
    })
    public void syncFromDatabase() {
        log.info("Hydrating in-memory Red-Black Tree and Min-Heap structures from database...");

        // Clear in-memory DSAs
        reservationsTree.clear();
        availableSeatsHeap.clear();
        waitlistHeap.clear();

        // 1. Hydrate available seats heap
        List<Seat> availableSeats = seatRepository.findByStatusOrderBySeatNumberAsc(SeatStatus.AVAILABLE);
        for (Seat seat : availableSeats) {
            availableSeatsHeap.insert(seat.getSeatNumber());
        }

        // 2. Hydrate active reservations tree
        List<Reservation> reservations = reservationRepository.findAll();
        for (Reservation res : reservations) {
            reservationsTree.insert(res.getUserId(), res.getSeat().getSeatNumber());
        }

        // 3. Hydrate waitlist priority heap
        List<WaitlistEntry> waitingEntries = waitlistRepository.findAllByStatusOrderByPriorityDescTimestampAsc(WaitlistStatus.WAITING);
        for (WaitlistEntry entry : waitingEntries) {
            waitlistHeap.insert(entry);
        }

        log.info("In-memory cache hydration complete: Available Seats={}, Reservations={}, Waitlist={}",
                availableSeatsHeap.size(), reservationsTree.size(), waitlistHeap.size());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_SYSTEM_STATUS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEATS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEAT, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_WAITLIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_RESERVATIONS, allEntries = true)
    })
    public void initializeSeats(int seatCount) {
        if (seatCount <= 0) {
            throw new InvalidRequestException("Seat count must be greater than 0. Provided: " + seatCount);
        }

        log.info("Initializing venue with {} seats (resetting existing inventory)...", seatCount);

        // Delete existing reservations, waitlist entries, and seats
        reservationRepository.deleteAllInBatch();
        waitlistRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();

        // Reset in-memory DSAs
        reservationsTree.clear();
        availableSeatsHeap.clear();
        waitlistHeap.clear();

        List<Seat> seatsToSave = new ArrayList<>(seatCount);
        for (int i = 1; i <= seatCount; i++) {
            SeatTier tier = determineSeatTier(i, seatCount);
            Seat seat = Seat.builder()
                    .seatNumber(i)
                    .status(SeatStatus.AVAILABLE)
                    .tier(tier)
                    .build();
            seatsToSave.add(seat);
            availableSeatsHeap.insert(i);
        }

        seatRepository.saveAll(seatsToSave);
        log.info("Successfully initialized and saved {} seats to database and available seats heap", seatCount);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_SYSTEM_STATUS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEATS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEAT, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_WAITLIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_RESERVATIONS, allEntries = true)
    })
    public ReservationResponse reserveSeat(String userId, int priority) {
        return processSeatReservation(userId, priority, null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_SYSTEM_STATUS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEATS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEAT, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_WAITLIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_RESERVATIONS, allEntries = true)
    })
    public ReservationResponse holdSeat(String userId, int priority, int ttlSeconds) {
        if (ttlSeconds <= 0) {
            throw new InvalidRequestException("TTL seconds must be greater than 0");
        }
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        return processSeatReservation(userId, priority, expiresAt);
    }

    private ReservationResponse processSeatReservation(String userId, int priority, Instant expiresAt) {
        validateUserNotAlreadyBookedOrWaiting(userId);

        if (priority < 1 || priority > 5) {
            throw new InvalidRequestException("Priority must be between 1 and 5. Provided: " + priority);
        }

        // Check if seats are available in memory
        if (!availableSeatsHeap.isEmpty()) {
            Integer seatNumber = availableSeatsHeap.extractMin();

            Seat seat = seatRepository.findBySeatNumber(seatNumber)
                    .orElseThrow(() -> new SeatNotFoundException("Seat number " + seatNumber + " not found in database"));

            SeatStatus targetStatus = (expiresAt != null) ? SeatStatus.HELD : SeatStatus.RESERVED;
            seat.setStatus(targetStatus);
            seatRepository.save(seat);

            Reservation reservation = Reservation.builder()
                    .userId(userId)
                    .seat(seat)
                    .reservedAt(Instant.now())
                    .expiresAt(expiresAt)
                    .build();

            Reservation savedRes = reservationRepository.save(reservation);
            reservationsTree.insert(userId, seatNumber);

            log.info("User {} successfully allocated seat {} (Status={})", userId, seatNumber, targetStatus);
            return mapToReservationResponse(savedRes);
        } else {
            // Venue sold out: add to priority waitlist
            WaitlistEntry entry = WaitlistEntry.builder()
                    .userId(userId)
                    .priority(priority)
                    .timestamp(System.nanoTime())
                    .status(WaitlistStatus.WAITING)
                    .build();

            WaitlistEntry savedEntry = waitlistRepository.save(entry);
            waitlistHeap.insert(savedEntry);

            log.info("Venue sold out. User {} added to waitlist with priority {}", userId, priority);
            return null; // Signals placement on waitlist
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_SYSTEM_STATUS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEATS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEAT, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_WAITLIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_RESERVATIONS, allEntries = true)
    })
    public void cancelReservation(int seatNumber, String userId) {
        GenericRedBlackTree.Node<String, Integer> node = reservationsTree.findNode(userId);

        if (node == null) {
            throw new ReservationNotFoundException("User " + userId + " has no active reservation to cancel");
        }

        if (!Objects.equals(node.getValue(), seatNumber)) {
            throw new ReservationNotFoundException("User " + userId + " does not hold seat " + seatNumber);
        }

        // Delete from database & in-memory tree
        reservationRepository.deleteByUserId(userId);
        reservationRepository.flush();
        reservationsTree.delete(userId);

        Seat seat = seatRepository.findBySeatNumber(seatNumber)
                .orElseThrow(() -> new SeatNotFoundException("Seat " + seatNumber + " not found"));

        log.info("User {} canceled reservation for seat {}", userId, seatNumber);

        // Cascading re-allocation: If users are waiting, promote highest priority user
        cascadeSeatAssignment(seat);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_SYSTEM_STATUS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_WAITLIST, allEntries = true)
    })
    public boolean exitWaitlist(String userId) {
        if (!waitlistHeap.containsId(userId)) {
            log.warn("User {} is not in the waitlist", userId);
            return false;
        }

        waitlistHeap.removeById(userId);
        waitlistRepository.deleteByUserId(userId);
        log.info("User {} removed from the waitlist", userId);
        return true;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_WAITLIST, allEntries = true)
    })
    public boolean updatePriority(String userId, int newPriority) {
        if (newPriority < 1 || newPriority > 5) {
            throw new InvalidRequestException("Priority must be between 1 and 5. Provided: " + newPriority);
        }

        WaitlistEntry currentEntry = waitlistHeap.get(userId);
        if (currentEntry == null) {
            log.warn("Cannot update priority: User {} not found in waitlist", userId);
            return false;
        }

        currentEntry.setPriority(newPriority);
        waitlistHeap.update(currentEntry);

        waitlistRepository.findByUserId(userId).ifPresent(dbEntry -> {
            dbEntry.setPriority(newPriority);
            waitlistRepository.save(dbEntry);
        });

        log.info("User {} priority successfully updated to {}", userId, newPriority);
        return true;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_SYSTEM_STATUS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEATS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEAT, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_WAITLIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_RESERVATIONS, allEntries = true)
    })
    public void addSeats(int count) {
        if (count <= 0) {
            throw new InvalidRequestException("Seat addition count must be greater than 0");
        }

        int maxSeatNumber = seatRepository.findMaxSeatNumber().orElse(0);
        int startSeat = maxSeatNumber + 1;
        int endSeat = maxSeatNumber + count;

        log.info("Expanding venue capacity by {} seats (range: {} to {})", count, startSeat, endSeat);

        List<Seat> newSeats = new ArrayList<>(count);
        for (int seatNum = startSeat; seatNum <= endSeat; seatNum++) {
            SeatTier tier = determineSeatTier(seatNum, endSeat);
            Seat seat = Seat.builder()
                    .seatNumber(seatNum)
                    .status(SeatStatus.AVAILABLE)
                    .tier(tier)
                    .build();
            newSeats.add(seat);
        }

        List<Seat> savedSeats = seatRepository.saveAll(newSeats);

        // Assign each new seat to waitlist or available seats heap
        for (Seat seat : savedSeats) {
            cascadeSeatAssignment(seat);
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_SYSTEM_STATUS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEATS, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_SEAT, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_WAITLIST, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_RESERVATIONS, allEntries = true)
    })
    public List<Integer> releaseSeats(String fromUserId, String toUserId) {
        if (fromUserId == null || toUserId == null || fromUserId.compareTo(toUserId) > 0) {
            throw new InvalidRequestException("Invalid user range: fromUserId must be lexicographically <= toUserId");
        }

        log.info("Batch releasing reservations in user range [{}, {}]", fromUserId, toUserId);

        // Find all reservations in the user range via Red-Black Tree in O(log N + K)
        List<GenericRedBlackTree.Node<String, Integer>> nodesInRange = reservationsTree.findRange(fromUserId, toUserId);
        List<Integer> releasedSeatNumbers = new ArrayList<>();

        for (GenericRedBlackTree.Node<String, Integer> node : nodesInRange) {
            String uId = node.getKey();
            Integer seatNum = node.getValue();
            releasedSeatNumbers.add(seatNum);

            reservationRepository.deleteByUserId(uId);
            reservationsTree.delete(uId);

            // Also ensure user is removed from waitlist if present
            exitWaitlist(uId);
        }

        reservationRepository.flush();

        // Reallocate each freed seat to waiting users or return to available heap
        for (Integer seatNumber : releasedSeatNumbers) {
            seatRepository.findBySeatNumber(seatNumber).ifPresent(this::cascadeSeatAssignment);
        }

        log.info("Released {} seats for range [{}, {}]", releasedSeatNumbers.size(), fromUserId, toUserId);
        return releasedSeatNumbers;
    }

    /**
     * Helper method to assign a seat to the highest-priority waitlist customer or return it to available pool.
     */
    private void cascadeSeatAssignment(Seat seat) {
        if (!waitlistHeap.isEmpty()) {
            WaitlistEntry waitingUser = waitlistHeap.extractMin();
            waitlistRepository.deleteByUserId(waitingUser.getUserId());

            seat.setStatus(SeatStatus.RESERVED);
            seatRepository.save(seat);

            Reservation newRes = Reservation.builder()
                    .userId(waitingUser.getUserId())
                    .seat(seat)
                    .reservedAt(Instant.now())
                    .build();

            reservationRepository.save(newRes);
            reservationsTree.insert(waitingUser.getUserId(), seat.getSeatNumber());

            log.info("Waitlist Auto-Promotion: User {} promoted and allocated seat {}",
                    waitingUser.getUserId(), seat.getSeatNumber());
        } else {
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
            availableSeatsHeap.insert(seat.getSeatNumber());
            log.info("Seat {} returned to available inventory", seat.getSeatNumber());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_SYSTEM_STATUS, key = "'status'")
    public SystemStatusResponse getSystemStatus() {
        long totalSeats = seatRepository.count();
        long availableSeats = seatRepository.countByStatus(SeatStatus.AVAILABLE);
        long heldSeats = seatRepository.countByStatus(SeatStatus.HELD);
        long reservedSeats = seatRepository.countByStatus(SeatStatus.RESERVED);
        long waitlistCount = waitlistHeap.size();

        return new SystemStatusResponse(totalSeats, availableSeats, heldSeats, reservedSeats, waitlistCount);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_SEATS, key = "'all'")
    public List<SeatResponse> getAllSeats() {
        List<Seat> seats = seatRepository.findAllByOrderBySeatNumberAsc();
        List<Reservation> reservations = reservationRepository.findAll();

        Map<Integer, String> seatToUserMap = new HashMap<>();
        for (Reservation res : reservations) {
            seatToUserMap.put(res.getSeat().getSeatNumber(), res.getUserId());
        }

        List<SeatResponse> responses = new ArrayList<>(seats.size());
        for (Seat seat : seats) {
            String occupantUserId = seatToUserMap.get(seat.getSeatNumber());
            responses.add(new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getStatus(), seat.getTier(), occupantUserId));
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_RESERVATIONS, key = "'all'")
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAllByOrderBySeat_SeatNumberAsc().stream()
                .map(this::mapToReservationResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_WAITLIST, key = "'queue'")
    public List<WaitlistResponse> getWaitlist() {
        List<WaitlistEntry> sortedList = waitlistHeap.toSortedList();
        List<WaitlistResponse> responses = new ArrayList<>(sortedList.size());

        for (int i = 0; i < sortedList.size(); i++) {
            WaitlistEntry entry = sortedList.get(i);
            responses.add(new WaitlistResponse(
                    entry.getId(),
                    entry.getUserId(),
                    entry.getPriority(),
                    entry.getTimestamp(),
                    entry.getStatus(),
                    i + 1 // 1-based queue position
            ));
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_SEAT, key = "#seatNumber")
    public SeatResponse getSeatByNumber(int seatNumber) {
        Seat seat = seatRepository.findBySeatNumber(seatNumber)
                .orElseThrow(() -> new SeatNotFoundException("Seat number " + seatNumber + " not found"));

        String occupantUserId = reservationRepository.findBySeat_SeatNumber(seatNumber)
                .map(Reservation::getUserId)
                .orElse(null);

        return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getStatus(), seat.getTier(), occupantUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationByUserId(String userId) {
        return reservationRepository.findByUserId(userId)
                .map(this::mapToReservationResponse)
                .orElseThrow(() -> new ReservationNotFoundException("No active reservation found for user " + userId));
    }

    private void validateUserNotAlreadyBookedOrWaiting(String userId) {
        if (reservationsTree.containsKey(userId) || reservationRepository.existsByUserId(userId)) {
            throw new UserAlreadyReservedException("User " + userId + " already has an active reservation or hold");
        }
        if (waitlistHeap.containsId(userId) || waitlistRepository.existsByUserIdAndStatus(userId, WaitlistStatus.WAITING)) {
            throw new UserAlreadyInWaitlistException("User " + userId + " is already in the priority waitlist");
        }
    }

    private SeatTier determineSeatTier(int seatNumber, int totalSeats) {
        if (totalSeats <= 10) {
            return seatNumber <= 2 ? SeatTier.VIP : SeatTier.STANDARD;
        }
        double ratio = (double) seatNumber / totalSeats;
        if (ratio <= 0.10) {
            return SeatTier.COURTSIDE;
        } else if (ratio <= 0.30) {
            return SeatTier.VIP;
        } else {
            return SeatTier.STANDARD;
        }
    }

    private ReservationResponse mapToReservationResponse(Reservation res) {
        return new ReservationResponse(
                res.getId(),
                res.getUserId(),
                res.getSeat().getSeatNumber(),
                res.getSeat().getTier(),
                res.getReservedAt(),
                res.getExpiresAt()
        );
    }
}
