# TicketForge: Architecture, Potential Analysis & Modernization Blueprint

**Project Name:** TicketForge  
**GitHub Repository:** `ticket-forge`  
**Document Version:** 2.1.0  
**Target Platform:** Java 21 LTS | Spring Boot 3.3.x | Spring Data JPA | JUnit 5 | Maven | Docker  
**Domain:** High-Concurrency Event Ticketing & Priority-Based Resource Allocation Engine  

---

## Table of Contents
1. [Executive Summary & Potential Assessment](#1-executive-summary--potential-assessment)
2. [Current Architecture vs. Target Enterprise Architecture](#2-current-architecture-vs-target-enterprise-architecture)
3. [Deep-Dive: Core Java & Enterprise Concepts](#3-deep-dive-core-java--enterprise-concepts)
4. [Domain Model & Spring Data JPA Architecture](#4-domain-model--spring-data-jpa-architecture)
5. [Concurrency, Thread Safety & Race Conditions](#5-concurrency-thread-safety--race-conditions)
6. [RESTful API Specification](#6-restful-api-specification)
7. [Comprehensive Testing Strategy (JUnit 5, Mockito & Concurrency)](#7-comprehensive-testing-strategy-junit-5-mockito--concurrency)
8. [Simple, Efficient & Modern UI Design](#8-simple-efficient--modern-ui-design)
9. [Advanced Enterprise Enhancements](#9-advanced-enterprise-enhancements)
   * 9.1 Real-Time UI Synchronization (Server-Sent Events / SSE)
   * 9.2 Time-Bound Seat Holding & Auto-Expiry (TTL Mechanism)
   * 9.3 API Idempotency & Duplicate Request Protection
   * 9.4 Interactive API Documentation (OpenAPI 3 / Swagger UI)
   * 9.5 Observability & Metrics (Spring Boot Actuator & Micrometer)
   * 9.6 Role-Based Access Control (Admin vs. Customer RBAC)
   * 9.7 Containerization (Docker & Docker Compose)
10. [Enterprise Maven Configuration (`pom.xml`)](#10-enterprise-maven-configuration-pomxml)
11. [Proposed Modern Directory Structure](#11-proposed-modern-directory-structure)
12. [Step-by-Step Implementation Roadmap](#12-step-by-step-implementation-roadmap)

---

## 1. Executive Summary & Potential Assessment

### 1.1 Current State
**TicketForge** originates from an algorithm-driven seat reservation and priority waitlist engine. It was developed to solve real-time resource allocation and queue re-balancing using custom, low-level data structures:
* **Custom Red-Black Tree (`RedBlackTree.java`)**: A balanced binary search tree maintaining user-to-seat mappings with guaranteed $O(\log N)$ search, insertion, and deletion complexity.
* **Custom Indexed Min-Heap (`MinHeap.java`)**: A binary min-heap integrated with a hash table index (`userIndexMap`) enabling $O(\log N)$ extraction, $O(1)$ search, and $O(\log N)$ arbitrary priority updates or node removals.
* **Procedural Batch Controller (`GatorTicketMaster.java`)**: A CLI file-reader running sequential text commands (`Initialize`, `Reserve`, `Cancel`, `ExitWaitlist`, `UpdatePriority`, `AddSeats`, `PrintReservations`, `ReleaseSeats`, `Quit`).

### 1.2 Limitations of the Current Codebase
1. **Volatile In-Memory State**: Data disappears when the process terminates; output is limited to text logs.
2. **Lack of Concurrency Controls**: Not thread-safe; cannot handle simultaneous user bookings or flash-sale traffic spikes.
3. **No Modern Build System**: Uses an OS-dependent `makefile` rather than a standard Maven/Gradle lifecycle.
4. **No Automated Testing**: Relies on manual output text comparison rather than unit, integration, and load testing.
5. **Raw Object Types & Lack of Generics**: Low type-safety and frequent runtime casts in `MinHeap.java`.

### 1.3 Strategic Potential as an Enterprise Platform
**TicketForge** models the core mechanics of industry giants like **Ticketmaster, StubHub, and Eventbrite**:
* Managing high-demand ticket sales under strict seat-uniqueness constraints.
* Dynamic waitlist prioritization (VIP status, timestamp tie-breaking).
* Cascading automated re-allocation when reservations are cancelled or new inventory is released.

Converting this codebase into a **Spring Boot 3.x enterprise platform** creates an end-to-end showcase combining advanced algorithms with production-grade backend engineering.

---

## 2. Current Architecture vs. Target Enterprise Architecture

```
CURRENT ARCHITECTURE (Procedural Batch CLI)
[ input.txt ] ──> [ File Parser ] ──> [ Ticket Engine ] ──> [ output.txt ]
                                             │         │
                                             ▼         ▼
                                     [ RedBlackTree ] [ MinHeap ]
                                       (In-Memory)     (In-Memory)
```

```
TARGET ENTERPRISE ARCHITECTURE (Spring Boot 3 + JPA + Web + In-Memory Engine)

 ┌────────────────────────────────────────────────────────────────────────┐
 │                   Modern Responsive UI / REST Clients                  │
 └───────────────────────────────────┬────────────────────────────────────┘
                                     │ HTTP REST / Server-Sent Events / WS
                                     ▼
 ┌────────────────────────────────────────────────────────────────────────┐
 │                    Spring MVC / REST API Layer                         │
 │     [SeatController]   [ReservationController]   [WaitlistController]  │
 └───────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
 ┌────────────────────────────────────────────────────────────────────────┐
 │                    Service & Business Logic Layer                      │
 │   ┌────────────────────────────────────────────────────────────────┐   │
 │   │                   TicketForge Core Service                     │   │
 │   └───────────────┬───────────────────────────────┬────────────────┘   │
 │                   │                               │                    │
 │                   ▼                               ▼                    │
 │   ┌───────────────────────────────┐ ┌──────────────────────────────┐   │
 │   │  High-Throughput In-Memory    │ │   Concurrency & Lock Manager │   │
 │   │  Generic DSA Matching Engine  │ │  (Pessimistic / Optimistic)  │   │
 │   └───────────────────────────────┘ └──────────────────────────────┘   │
 └───────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
 ┌────────────────────────────────────────────────────────────────────────┐
 │               Persistence Layer (Spring Data JPA & Hibernate)          │
 │     [SeatRepository]   [ReservationRepository]   [WaitlistRepository]  │
 └───────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
 ┌────────────────────────────────────────────────────────────────────────┐
 │                 Relational Database (H2 / PostgreSQL)                  │
 │   [ seats ]          [ reservations ]          [ waitlist_entries ]    │
 └────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Deep-Dive: Core Java & Enterprise Concepts

Transforming this project allows you to demonstrate mastering the core concepts of modern enterprise Java development:

| Topic Area | Concept | Practical Application in TicketForge |
| :--- | :--- | :--- |
| **Concurrency** | Virtual Threads (Java 21), `ReentrantLock`, `@Version` (Optimistic Lock) | Handling thousands of concurrent reservation requests without race conditions or deadlocks. |
| **Data Structures** | Custom Generic Trees & Heaps vs. Standard Collections | Generifying `MinHeap<T>` and `RedBlackTree<K, V>` and evaluating them against `java.util.concurrent` collections. |
| **Persistence** | Spring Data JPA, Hibernate, ACID Transactions | Managing seat inventory state changes with `@Transactional`, composite indexes, and relational mapping. |
| **Build Automation** | Apache Maven Lifecycle & Plugins | Automating dependency management, code coverage (JaCoCo), packaging, and Spring Boot executable JAR generation. |
| **Testing** | JUnit 5, Mockito, AssertJ, Testcontainers | Multi-tier test suite: unit tests for algorithms, integration tests for REST APIs, and multi-threaded stress tests. |
| **Design Patterns** | Strategy, Observer, Repository, DTO & Factory Patterns | Dynamic seat allocation strategies, event listeners for waitlist promotions, and decoupled architecture. |
| **Modern Java** | Java Records, Sealed Classes, Pattern Matching, Stream API | Immutable DTO records, domain events, functional filtering, and clean error handling. |

---

## 4. Domain Model & Spring Data JPA Architecture

### 4.1 Relational Schema & Entity Diagram

```
 +------------------+             +--------------------+             +------------------+
 |      EVENT       | 1         * |        SEAT        | 1         1 |   RESERVATION    |
 +------------------+-------------+--------------------+-------------+------------------+
 | id: BIGINT (PK)  |             | id: BIGINT (PK)    |             | id: BIGINT (PK)  |
 | name: VARCHAR    |             | seat_number: INT   |             | user_id: BIGINT  |
 | total_seats: INT |             | tier: VARCHAR      |             | seat_id: BIGINT  |
 | event_date: TS   |             | status: VARCHAR    |             | reserved_at: TS  |
 +------------------+             | event_id: BIGINT   |             | expires_at: TS   |
         | 1                      +--------------------+             | version: INT     |
         |                                                           +------------------+
         | *                                                                   |
 +--------------------+                                                        | *
 |   WAITLIST_ENTRY   |                                              +------------------+
 +--------------------+                                              |       USER       |
 | id: BIGINT (PK)    |                                              +------------------+
 | user_id: BIGINT    |----------------------------------------------| id: BIGINT (PK)  |
 | event_id: BIGINT   |                                              | name: VARCHAR    |
 | priority: INT      |                                              | email: VARCHAR   |
 | timestamp: BIGINT  |                                              | priority_tier:INT|
 | status: VARCHAR    |                                              +------------------+
 +--------------------+
```

### 4.2 JPA Entity Definitions

#### Seat Entity
```java
package com.ticketforge.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats", indexes = {
    @Index(name = "idx_seat_status", columnList = "status"),
    @Index(name = "idx_seat_number", columnList = "seatNumber")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @Enumerated(EnumType.STRING)
    private SeatTier tier;

    public enum SeatStatus {
        AVAILABLE, HELD, RESERVED
    }

    public enum SeatTier {
        STANDARD, VIP, COURTSIDE
    }
}
```

#### Reservation Entity (with Optimistic Locking)
```java
package com.ticketforge.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_reservation_user", columnList = "userId"),
    @Index(name = "idx_reservation_expiry", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false, unique = true)
    private Seat seat;

    @Column(nullable = false)
    private LocalDateTime reservedAt;

    private LocalDateTime expiresAt; // For time-bound holding / TTL

    @Version
    private Integer version; // Prevents race conditions during concurrent updates
}
```

#### WaitlistEntry Entity
```java
package com.ticketforge.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "waitlist_entries", indexes = {
    @Index(name = "idx_waitlist_priority_time", columnList = "priority DESC, timestamp ASC"),
    @Index(name = "idx_waitlist_user", columnList = "userId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistEntry implements Comparable<WaitlistEntry> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer userId;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private Long timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaitlistStatus status;

    public enum WaitlistStatus {
        WAITING, PROMOTED, CANCELLED
    }

    @Override
    public int compareTo(WaitlistEntry other) {
        if (!this.priority.equals(other.priority)) {
            return other.priority.compareTo(this.priority); // Higher priority first
        }
        return Long.compare(this.timestamp, other.timestamp); // Earlier timestamp first
    }
}
```

---

## 5. Concurrency, Thread Safety & Race Conditions

In real-world ticketing scenarios, multiple threads compete for the same inventory.

### 5.1 Race Condition Scenarios Handled
1. **Simultaneous Single-Seat Booking**: Two users attempt to book the last available seat at the exact same millisecond.
2. **Cancellation Re-allocation Race**: While user $A$ is cancelling seat $S$, user $B$ tries to reserve it directly, bypassing the priority waitlist.
3. **Batch Range Release Collision**: Admin releases seats for user IDs $[10, 50]$ while several new users are entering the waitlist.

### 5.2 Concurrency Solutions Implemented
* **Pessimistic Write Lock (`@Lock(LockModeType.PESSIMISTIC_WRITE)`)**:
  Used on seat availability queries during reservation transactions to serialize row-level access.
* **Optimistic Versioning (`@Version`)**:
  Protects reservation rows so that stale updates fail immediately with `OptimisticLockException` and trigger graceful retries.
* **Thread-Safe In-Memory DSAs**:
  Wrapping internal tree/heap modifications in a fine-grained `ReentrantReadWriteLock` ensures high-throughput concurrent reads with serialized writes.

---

## 6. RESTful API Specification

| Endpoint | Method | Purpose | Sample Request Body | Sample Response Body | HTTP Status |
| :--- | :---: | :--- | :--- | :--- | :---: |
| `/api/v1/seats/initialize` | `POST` | Initialize seat inventory | `{"seatCount": 100}` | `{"message": "100 seats initialized", "totalSeats": 100}` | `201 Created` |
| `/api/v1/seats/availability` | `GET` | Get available count & waitlist size | _None_ | `{"availableSeats": 18, "waitlistCount": 5, "totalSeats": 100}` | `200 OK` |
| `/api/v1/reservations` | `POST` | Reserve a seat or join waitlist | `{"userId": 101, "priority": 3}` | `{"status": "RESERVED", "seatNumber": 12, "userId": 101}` | `200 OK` / `202 Accepted` |
| `/api/v1/reservations/{seatNumber}`| `DELETE` | Cancel reservation (auto-promotes waitlist) | `?userId=101` | `{"message": "Reservation cancelled", "promotedUserId": 205}` | `200 OK` |
| `/api/v1/reservations` | `GET` | List all active reservations | _None_ | `[{"seatNumber": 1, "userId": 101}, {"seatNumber": 2, "userId": 102}]` | `200 OK` |
| `/api/v1/reservations/release-range`| `POST` | Batch release range `[u1, u2]` | `{"fromUserId": 10, "toUserId": 25}` | `{"releasedSeatsCount": 12, "promotedWaitlistCount": 5}` | `200 OK` |
| `/api/v1/waitlist/{userId}` | `PATCH` | Update user priority in waitlist | `{"newPriority": 5}` | `{"userId": 302, "newPriority": 5, "updated": true}` | `200 OK` |
| `/api/v1/waitlist/{userId}` | `DELETE` | Leave waitlist | _None_ | `{"message": "User removed from waitlist"}` | `204 No Content` |
| `/api/v1/seats/expand` | `POST` | Add more seats to total capacity | `{"additionalCount": 20}` | `{"newTotal": 120, "seatsAdded": 20}` | `200 OK` |
| `/api/v1/events/stream` | `GET` | Server-Sent Events (SSE) live updates stream | _None_ | `data: {"type": "SEAT_RESERVED", "seatNumber": 5, "userId": 101}` | `200 OK (text/event-stream)` |

---

## 7. Comprehensive Testing Strategy (JUnit 5, Mockito & Concurrency)

A robust 3-tier testing pyramid:

```
                  / \
                 /   \
                / E2E \       <-- RestAssured / MockMvc End-to-End API Flow Tests
               /───────\
              / Integr. \     <-- @DataJpaTest & Multi-Threaded Concurrency Tests
             /───────────\
            /  Unit Tests \   <-- MinHeap, RedBlackTree & Service Mockito Tests
           /───────────────\
```

### 7.1 Sample Unit Test: Priority Waitlist Sorting
```java
@Test
@DisplayName("Waitlist entries should prioritize higher priority value, then earlier timestamp")
void testWaitlistEntryOrdering() throws InterruptedException {
    WaitlistEntry regularUserEarly = new WaitlistEntry(1L, 101, 1, System.nanoTime(), WaitlistStatus.WAITING);
    Thread.sleep(2);
    WaitlistEntry regularUserLate = new WaitlistEntry(2L, 102, 1, System.nanoTime(), WaitlistStatus.WAITING);
    WaitlistEntry vipUser = new WaitlistEntry(3L, 103, 3, System.nanoTime(), WaitlistStatus.WAITING);

    GenericMinHeap<WaitlistEntry> heap = new GenericMinHeap<>();
    heap.insert(regularUserLate);
    heap.insert(regularUserEarly);
    heap.insert(vipUser);

    assertThat(heap.extractMin().getUserId()).isEqualTo(103); // Highest priority (3)
    assertThat(heap.extractMin().getUserId()).isEqualTo(101); // Priority 1, earlier timestamp
    assertThat(heap.extractMin().getUserId()).isEqualTo(102); // Priority 1, later timestamp
}
```

### 7.2 Multi-Threaded Concurrency Stress Test
```java
@Test
@DisplayName("50 Concurrent requests for 5 remaining seats must yield exactly 5 reservations and 45 waitlist entries")
void testConcurrentSeatBookingNoDoubleBooking() throws InterruptedException {
    int threadCount = 50;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);

    ticketForgeService.initialize(5); // Only 5 seats

    for (int i = 1; i <= threadCount; i++) {
        final int userId = i;
        executor.submit(() -> {
            try {
                startLatch.await();
                ticketForgeService.reserve(userId, 1);
            } catch (Exception e) {
                // record exceptions
            } finally {
                endLatch.countDown();
            }
        });
    }

    startLatch.countDown(); // Fire all threads simultaneously
    endLatch.await(5, TimeUnit.SECONDS);

    assertThat(seatRepository.countByStatus(SeatStatus.RESERVED)).isEqualTo(5);
    assertThat(waitlistRepository.countByStatus(WaitlistStatus.WAITING)).isEqualTo(45);
}
```

---

## 8. Simple, Efficient & Modern UI Design

A lightweight, modern dashboard interface built with **HTML5, CSS3 Glassmorphism, and Vanilla JavaScript**:

```
+-------------------------------------------------------------------------------------------------------+
|  ⚡ TICKETFORGE           [Live System: Active]               ⚡ Spring Boot 3.3 | Java 21 LTS        |
+-------------------------------------------------------------------------------------------------------+
|  [ 🏟️ Total Seats: 100 ]   [ 🟩 Available: 18 ]   [ 🟥 Reserved: 82 ]   [ ⏳ Waitlist Queue: 12 ]       |
+-------------------------------------------------------------------------------------------------------+
|                                                                                                       |
|  STADIUM SEATING MAP (Interactive Grid with Live Push)                                                |
|  +----+ +----+ +----+ +----+ +----+ +----+ +----+ +----+ +----+ +----+                                |
|  | 01 | | 02 | | 03 | | 04 | | 05 | | 06 | | 07 | | 08 | | 09 | | 10 |   [Seat Status Legend]         |
|  |RES | |RES | |AVL | |AVL | |VIP | |RES | |AVL | |RES | |RES | |AVL |   🟩 Available (Click to book) |
|  +----+ +----+ +----+ +----+ +----+ +----+ +----+ +----+ +----+ +----+   🟥 Reserved (Click to view)  |
|  | 11 | | 12 | | 13 | | 14 | | 15 | | 16 | | 17 | | 18 | | 19 | | 20 |   🟨 Held (TTL Expiry)         |
|  +----+ +----+ +----+ +----+ +----+ +----+ +----+ +----+ +----+ +----+   ⭐ VIP Reserved              |
|                                                                                                       |
+---------------------------------------------------+---------------------------------------------------+
|  🎮 ACTION PANEL                                  |  ⏳ PRIORITY WAITLIST MONITOR (Min-Heap View)     |
|  ------------------------------------------------ |  ------------------------------------------------ |
|  Reserve Seat:                                    |  Pos  User ID  Priority  Joined At      Action    |
|  User ID: [ 105 ]  Priority: [ 3 - VIP  ▼ ]       |  #1   User 402 Priority 5 12:01:04 PM   [Update]  |
|  [ 🎟️ Submit Reservation ]                        |  #2   User 108 Priority 3 12:00:15 PM   [Update]  |
|                                                   |  #3   User 215 Priority 1 12:02:30 PM   [Update]  |
|  Cancel / Release Options:                        |                                                   |
|  [ Cancel Single Seat ]  [ Batch Release Range ]  |                                                   |
+---------------------------------------------------+---------------------------------------------------+
|  📜 LIVE AUDIT LOG STREAM (Real-time events via Server-Sent Events / SSE)                             |
|  [12:04:15] [PROMOTION] User 108 promoted from waitlist -> Allocated Seat 03.                         |
|  [12:04:14] [CANCELLED] User 12 cancelled reservation for Seat 03.                                    |
|  [12:03:50] [RESERVED]  User 88 reserved Seat 25.                                                     |
+-------------------------------------------------------------------------------------------------------+
```

---

## 9. Advanced Enterprise Enhancements

### 9.1 Real-Time UI Synchronization (Server-Sent Events / SSE)
In high-concurrency ticketing, multiple users view the seating grid at the same time. When Seat 15 is reserved by User 42, all other users' browsers should update without manual refreshing.
* **Spring Controller:** Expose an `SseEmitter` endpoint (`/api/v1/events/stream`).
* **Event Dispatcher:** When reservations, cancellations, or waitlist promotions occur, publish an application event that broadcasts JSON payloads to active SSE subscribers.

---

### 9.2 Time-Bound Seat Holding & Auto-Expiry (TTL Mechanism)
Real systems place seats into a temporary `HELD` state for 5–10 minutes to allow the user to enter billing info.
* **State Transition Lifecycle:**
  `AVAILABLE` $\rightarrow$ `HELD` (TTL 5 mins) $\rightarrow$ `RESERVED` (on payment) OR `AVAILABLE` (on expiry).
* **Spring Scheduled Sweeper:**
  ```java
  @Component
  @RequiredArgsConstructor
  public class ReservationExpiryScheduler {
      private final TicketForgeService ticketForgeService;

      @Scheduled(fixedRate = 10000) // Runs every 10 seconds
      public void releaseExpiredHolds() {
          ticketForgeService.processExpiredHolds();
      }
  }
  ```

---

### 9.3 API Idempotency & Duplicate Request Protection
Flash sales lead to users double-clicking submit buttons.
* **Implementation:** Accept an optional or required `Idempotency-Key: <UUID>` header in `POST /api/v1/reservations`.
* **Behavior:** If a request with an existing idempotency key arrives within 60 seconds, return the cached result instead of executing a second reservation.

---

### 9.4 Interactive API Documentation (OpenAPI 3 / Swagger UI)
* Integrated via `springdoc-openapi-starter-webmvc-ui`.
* Accessible out-of-the-box at: `http://localhost:8080/swagger-ui.html`.

---

### 9.5 Observability & Metrics (Spring Boot Actuator & Micrometer)
Expose system health and custom ticketing business metrics:
* Custom Prometheus gauges and counters:
  * `ticketforge.seats.available` (Gauge)
  * `ticketforge.waitlist.depth` (Gauge)
  * `ticketforge.reservations.total` (Counter)
  * `ticketforge.cancellations.total` (Counter)
* Health endpoint: `/actuator/health`.

---

### 9.6 Role-Based Access Control (Admin vs. Customer RBAC)
Separate customer-facing actions from sensitive administrative controls:
* **Customer Permissions:** `GET /availability`, `POST /reservations`, `DELETE /reservations/{seatId}`, `DELETE /waitlist/{userId}`.
* **Admin Permissions:** `POST /seats/initialize`, `POST /seats/expand`, `POST /reservations/release-range`.

---

### 9.7 Containerization (Docker & Docker Compose)

#### Multi-Stage `Dockerfile`
```dockerfile
# Stage 1: Build the application with Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Java Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
VOLUME /tmp
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "app.jar"]
```

#### `docker-compose.yml`
```yaml
version: '3.8'

services:
  app:
    build: .
    container_name: ticketforge_app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/ticketforge_db
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
    depends_on:
      - db

  db:
    image: postgres:16-alpine
    container_name: ticketforge_db
    environment:
      POSTGRES_DB: ticketforge_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

---

## 10. Enterprise Maven Configuration (`pom.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.2</version>
        <relativePath/>
    </parent>

    <groupId>com.ticketforge</groupId>
    <artifactId>ticket-forge</artifactId>
    <version>1.0.0</version>
    <name>TicketForge</name>
    <description>Enterprise High-Concurrency Ticket Master and Waitlist Engine</description>

    <properties>
        <java.version>21</java.version>
        <springdoc.version>2.5.0</springdoc.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web: REST APIs & Embedded Tomcat -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA & Hibernate ORM -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Jakarta Bean Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Spring Boot Actuator for Production Metrics & Health -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- OpenAPI 3 / Swagger UI Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- In-Memory H2 Database for local development & automated testing -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- PostgreSQL Driver for production deployment -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Project Lombok (reduces boilerplate getters, setters, builders) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing Suite: JUnit 5, Mockito, AssertJ, Spring Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Executable JAR Packaging Plugin -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- Compiler Plugin with Java 21 features enabled -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 11. Proposed Modern Directory Structure

```
ticket-forge/
├── pom.xml                                  <-- Maven build configuration
├── Dockerfile                               <-- Multi-stage Docker containerization
├── docker-compose.yml                       <-- App + PostgreSQL orchestration
├── README.md                                <-- Project documentation & run guide
├── TICKET_FORGE_BLUEPRINT.md
└── src/
    ├── main/
    │   ├── java/com/ticketforge/
    │   │   ├── TicketForgeApplication.java
    │   │   ├── config/                      <-- OpenApi & SSE Configuration
    │   │   │   ├── OpenApiConfig.java
    │   │   │   └── AsyncConfig.java
    │   │   ├── controller/                  <-- REST & SSE Controller Endpoints
    │   │   │   ├── SeatController.java
    │   │   │   ├── ReservationController.java
    │   │   │   ├── WaitlistController.java
    │   │   │   └── EventStreamController.java
    │   │   ├── dto/                         <-- Request & Response DTO Records
    │   │   │   ├── InitializeSeatsRequest.java
    │   │   │   ├── ReservationRequest.java
    │   │   │   ├── UpdatePriorityRequest.java
    │   │   │   ├── ReleaseSeatsRequest.java
    │   │   │   ├── SeatResponse.java
    │   │   │   └── SystemStatusResponse.java
    │   │   ├── model/                       <-- JPA Entity Models
    │   │   │   ├── Seat.java
    │   │   │   ├── Reservation.java
    │   │   │   └── WaitlistEntry.java
    │   │   ├── repository/                  <-- Spring Data JPA Repositories
    │   │   │   ├── SeatRepository.java
    │   │   │   ├── ReservationRepository.java
    │   │   │   └── WaitlistRepository.java
    │   │   ├── service/                     <-- Core Business Logic & Transactions
    │   │   │   ├── TicketForgeService.java
    │   │   │   ├── EventPublisherService.java
    │   │   │   └── impl/TicketForgeServiceImpl.java
    │   │   ├── scheduler/                   <-- Scheduled Tasks (TTL Expiry)
    │   │   │   └── ReservationExpiryScheduler.java
    │   │   ├── dsa/                         <-- Generified In-Memory DSAs
    │   │   │   ├── GenericRedBlackTree.java
    │   │   │   └── GenericMinHeap.java
    │   │   └── exception/                   <-- Centralized Exception Handling
    │   │       ├── GlobalExceptionHandler.java
    │   │       ├── SeatNotFoundException.java
    │   │       └── UserNotFoundException.java
    │   └── resources/
    │       ├── application.yml              <-- Spring Boot Configuration
    │       └── static/                      <-- Modern Interactive Web UI
    │           ├── index.html
    │           ├── styles.css
    │           └── app.js
    └── test/
        └── java/com/ticketforge/
            ├── dsa/                         <-- Algorithm Unit Tests
            │   ├── GenericMinHeapTest.java
            │   └── GenericRedBlackTreeTest.java
            ├── service/                     <-- Business Logic Unit Tests (Mockito)
            │   └── TicketForgeServiceTest.java
            ├── controller/                  <-- REST API Integration Tests (MockMvc)
            │   └── ReservationControllerTest.java
            └── concurrency/                 <-- Multi-threaded Race Condition Tests
                └── ConcurrentBookingTest.java
```

---

## 12. Step-by-Step Implementation Roadmap

### Phase Deliverables Matrix

| Phase | Core Focus | Included Advanced Enhancements | Key Deliverables |
| :--- | :--- | :--- | :--- |
| **Phase 1** | **Build & Config Foundation** | Actuator, OpenAPI 3, Java 21, Lombok | `pom.xml`, `application.yml`, Base Package Hierarchy, Health Endpoint |
| **Phase 2** | **JPA Data & Domain Layer** | Optimistic Locking (`@Version`), DB Indexes, Status Enums | `Seat`, `Reservation`, `WaitlistEntry` Entities & Spring Data Repositories |
| **Phase 3** | **Business Logic & DSAs** | Generic Min-Heap, Generic RB-Tree, TTL Scheduler, Idempotency | `TicketForgeService`, `ReservationExpiryScheduler`, In-Memory Engine |
| **Phase 4** | **APIs, Events & Docs** | Server-Sent Events (SSE), Swagger UI, Validation, RBAC Filters | REST Controllers, `EventStreamController`, `GlobalExceptionHandler` |
| **Phase 5** | **Automated Testing Suite** | Concurrency Stress Tests, Mockito Unit Tests, MockMvc Tests | Algorithm Tests, Service Layer Tests, Race Condition Verification |
| **Phase 6** | **UI & Containerization** | Real-Time SSE Visualizer, Glassmorphism Dashboard, Docker | `index.html`, `styles.css`, `app.js`, `Dockerfile`, `docker-compose.yml` |

---

### Detailed Phase Breakdown

#### Phase 1: Project Initialization & Enterprise Maven Configuration
* Configure `pom.xml` with Java 21 LTS, Spring Boot 3.3.x, Spring Data JPA, Web, Validation, Actuator, and Springdoc OpenAPI under `groupId: com.ticketforge`, `artifactId: ticket-forge`.
* Establish standard Maven directory layout (`src/main/java/com/ticketforge`, `src/main/resources`, `src/test/java/com/ticketforge`).
* Set up `application.yml` with H2 console, JPA/Hibernate configuration, Actuator metrics endpoints, and logging levels.

#### Phase 2: Domain Modeling & JPA Persistence Layer
* Create JPA entities with complete constraints: `Seat`, `Reservation`, `WaitlistEntry`, and `User`.
* Add optimistic locking versioning (`@Version`) on `Reservation` to prevent concurrent modification collisions.
* Define composite database indexes on `(priority DESC, timestamp ASC)` and `status` for sub-millisecond lookups.
* Build Spring Data Repositories with custom derived methods, `@Lock(PESSIMISTIC_WRITE)` queries, and pagination.

#### Phase 3: Service Layer, Generified Algorithms & Background Schedulers
* Generify existing algorithmic data structures into type-safe `GenericRedBlackTree<K, V>` and `GenericMinHeap<T>`.
* Implement `TicketForgeService` with full transactional integrity (`@Transactional`) for:
  * `initialize(seatCount)`
  * `reserve(userId, priority)`
  * `cancel(seatId, userId)` (with automated cascading waitlist promotion)
  * `releaseSeats(fromUserId, toUserId)`
  * `updatePriority(userId, newPriority)`
  * `addSeats(count)`
* Implement `ReservationExpiryScheduler` (`@Scheduled`) to automatically release `HELD` seats when 5-minute TTL timers expire.
* Implement request idempotency handling to prevent double booking on rapid button clicks.

#### Phase 4: REST Controllers, SSE Live Stream & Swagger UI
* Design immutable Java `record` DTOs with validation rules (`@NotNull`, `@Min`, `@Positive`).
* Create REST Controllers (`SeatController`, `ReservationController`, `WaitlistController`).
* Implement `EventStreamController` using `SseEmitter` to broadcast real-time booking and queue events to client browsers.
* Build centralized `@RestControllerAdvice` (`GlobalExceptionHandler`) to map domain exceptions into standardized RFC 7807 problem details JSON.
* Configure OpenAPI 3 metadata and verify the interactive Swagger UI playground at `/swagger-ui.html`.

#### Phase 5: Comprehensive Automated Testing Suite
* **Algorithm Unit Tests:** Validate `GenericMinHeap` and `GenericRedBlackTree` edge cases (rotations, priority re-balancing, deletions).
* **Service Mockito Tests:** Test business logic in isolation with mocked repositories (promotions, range releases).
* **MockMvc API Integration Tests:** Validate HTTP status codes, request validation errors, and response payloads.
* **Multi-Threaded Concurrency Tests:** Simulate 50+ simultaneous threads booking the final remaining seats using `CountDownLatch` and `ExecutorService` to verify zero double-bookings.

#### Phase 6: Interactive Seating Visualizer UI & Containerization
* Build a responsive, glassmorphism-styled Single Page Dashboard (`index.html`, `styles.css`, `app.js`) in `src/main/resources/static/`.
* Connect client-side JavaScript to the `/api/v1/events/stream` SSE feed for zero-latency live visual seat changes.
* Build multi-stage `Dockerfile` (Maven Build + Eclipse Temurin 21 JRE runtime).
* Build `docker-compose.yml` to orchestrate the application with PostgreSQL for production readiness.
