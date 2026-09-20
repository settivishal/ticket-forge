# 🗺️ TicketForge Implementation Roadmap & Progress Tracker

> Detailed phase-by-phase implementation progress, deliverable tracking, and project architecture hierarchy.

---

## 📊 Phase Progress Summary

| Phase | Milestone | Scope / Deliverables | Status | Tests |
| :--- | :--- | :--- | :---: | :---: |
| **Phase 1** | Build & Config Foundation | Java 21, Spring Boot 3.3.2, multi-profile config, Flyway V1, Supabase JWT, Swagger UI | ✅ Completed | 5 |
| **Phase 2** | JPA Data & Domain Layer | JPA Entities (`Seat`, `Reservation`, `WaitlistEntry`, `User`), Optimistic/Pessimistic Locks, DTOs | ✅ Completed | 14 |
| **Phase 3** | Business Logic & DSAs | Generic Red-Black Tree, Indexed Min-Heap ($O(1)$ updates), TTL hold scheduler, concurrency tests | ✅ Completed | 43 |
| **Phase 4** | Caching & Redis Integration | Redis Cache-Aside (`@Cacheable`), Redisson Distributed Locks (`RLock`), Rate Limiter, Pub/Sub | ✅ Completed | 54 |
| **Phase 5** | GraphQL Engine | Schema-first `schema.graphqls`, Queries, Mutations, WebSocket Subscriptions, DataLoader | ✅ Completed | 63 |
| **Phase 6** | REST APIs, SSE & Security | REST controllers, Live SSE (`SseEmitter`), RFC 7807 Global Exception Handler, RBAC filter chain | ✅ Completed | 95 |
| **Phase 7** | Automated Testing Suite | Multi-tier test suite: DSA invariant tests, Mockito service tests, E2E multi-protocol, 100-thread stress tests | ✅ Completed | 123 |
| **Phase 8** | Modern UI & Cloud Deploy | Glassmorphism Single-Page Dashboard, Live 2D Seat Map, SSE Event Feed, Docker Compose stack | ✅ Completed | 126 |
| **Phase 9** | Security & Booking Flow Hardening | Authenticated GraphQL, token-derived identity, per-operation RBAC, credential-verified sign-in, hold confirmation, seat selection | ✅ Completed | 141 |

---

## 🗺️ Detailed Phase Breakdown

### ✅ Phase 1: Build & Config Foundation
- Maven `pom.xml`, Java 21 LTS, Spring Boot 3.3.2, `./mvnw` wrapper
- Multi-profile configuration (`application-dev.yml`, `application-staging.yml`, `application-prod.yml`)
- Flyway V1 schema migration (`V1__init_ticketing_schema.sql`)
- Supabase OAuth2 Resource Server & JWT Authentication Converter
- OpenAPI 3 / Swagger UI (`/swagger-ui.html`) and Actuator endpoints

### ✅ Phase 2: JPA Data, Schema & Repositories
- JPA Entity models (`Seat`, `Reservation`, `WaitlistEntry`, `User`)
- Optimistic locking (`@Version`) & Pessimistic locking queries (`@Lock(PESSIMISTIC_WRITE)`)
- Jakarta Bean Validation Request/Response Record DTOs
- Spring Data JPA Repositories with custom derived and JPQL queries

### ✅ Phase 3: Business Logic & In-Memory DSAs
- Generified `GenericRedBlackTree<K, V>` for $O(\log N)$ seat allocation and range queries
- Indexed `GenericMinHeap<T>` for priority waitlist ($P=3$ VIP down to $P=1$ Standard) with $O(1)$ priority lookups
- `TicketForgeService` transactional operations & automatic waitlist promotions
- Time-to-Live (TTL) auto-expiry background scheduler (`TicketHoldTtlScheduler`)
- 100-thread multi-threaded concurrency race condition stress tests

### ✅ Phase 4: Distributed Caching & Redis Integration
- `spring-boot-starter-data-redis` & Redisson client integration with non-blocking local in-memory fallback
- Redis cache-aside layer for seat availability & venue layout (`@Cacheable`, `@CacheEvict`)
- Redisson distributed locks (`RLock`) for multi-instance cluster-safe concurrency
- Redis token-bucket / sliding window rate limiting to block ticketing bots / scalpers
- Redis Pub/Sub for cross-instance real-time event broadcasting (`ticketforge:events`)

### ✅ Phase 5: GraphQL Engine & Schema Architecture
- `spring-boot-starter-graphql` schema-first architecture (`schema.graphqls`)
- Query resolvers (flexible seat maps, waitlist positions, system metrics)
- Mutation resolvers (book seat, cancel reservation, update priority)
- Subscription resolvers over WebSocket (`/graphql`) for live seat state changes
- Batch mapping / `DataLoader` implementation to eliminate GraphQL N+1 problem
- GraphiQL interactive development playground (`/graphiql`)

### ✅ Phase 6: REST Controllers, Live SSE Stream & Security
- REST API controllers with Jakarta Bean Validation (`SeatController`, `ReservationController`, `WaitlistController`)
- `EventStreamController` & `SseEmitterManager` with real-time SSE push & 15-second heartbeat pings
- Stateless Supabase OAuth2 / JWT security filter chain with RBAC routes
- Centralized RFC 7807 `GlobalExceptionHandler` with standardized `ProblemDetail` responses
- Zero-dependency `DevAuthenticationFilter` for local mock testing (`Bearer dev-admin`, `Bearer dev-customer`)

### ✅ Phase 7: Comprehensive Automated Testing Suite
- DSA algorithm invariant tests (`GenericRedBlackTreeInvariantsTest`, `GenericMinHeapAdvancedTest`)
- Mockito service layer unit tests (`TicketForgeServiceMockitoTest`)
- End-to-end multi-protocol integration tests (`TicketForgeE2EIntegrationTest`)
- Redis cache-aside eviction and Token Bucket rate limiter tests (`RedisCacheAndRateLimitingIntegrationTest`)
- Multi-threaded 100-thread concurrency & race condition stress tests (`TicketForgeHeavyConcurrencyStressTest`)

### ✅ Phase 8: Modern UI & Cloud Containerization
- High-aesthetic Glassmorphism Single-Page Dashboard (`index.html`, `styles.css`, `app.js`)
- Interactive 2D arena seat map with real-time tier indicators, state badges, and click-to-book modal
- Real-time Server-Sent Events (SSE) live domain event terminal feed with auto-reconnection
- Interactive booking forms, TTL hold sliders, 10-burst flash sale simulator, and waitlist manager
- Multi-stage `Dockerfile` (Eclipse Temurin 21 JRE, non-root user) & `docker-compose.yml` (App + PostgreSQL 16 + Redis 7)
- 126 passing automated unit, mock, security, and integration tests

### ✅ Phase 9: Security & Booking Flow Hardening
- `/graphql` moved behind authentication; it previously sat in the `permitAll()` matcher list, exposing every mutation (including admin-only ones) anonymously
- Acting user and priority tier derived from the authenticated principal via `SecurityUtils`; `userId` removed from request bodies, paths and query parameters
- Per-operation `@PreAuthorize` on every GraphQL resolver, since one URL matcher cannot express per-mutation rules
- Reading or mutating another user's reservation or waitlist entry restricted to `ROLE_ADMIN`; customers use `/reservations/me` and `DELETE /waitlist`
- Sign-in verifies credentials against Supabase; the previous mock branch accepted any password and inferred admin from the string "admin". Auth config now fails closed when the backend is unreachable
- `confirmHold` adds the missing `HELD → RESERVED` transition, so a held seat can become a booking instead of only expiring
- Optional `seatNumber` on reserve/hold makes the seat map functional, returning `409` when the chosen seat is taken
- Removed the admin burst simulator, which worked only by impersonating fabricated user ids

---

## 🌿 Git Branching & Promotion Strategy

1. **`feature/<name>`**: Individual feature/task branches. Branch off `main`, open a PR back to `main`. CI runs the full backend test suite on every push and PR.
2. **`main`**: Release branch. Merging to `main` auto-deploys the backend to Render and the frontend to Vercel, both against the single Supabase project.

---

## 📂 Project Architecture Tree

```
ticket-forge/
├── .mvn/wrapper/                            <-- Maven Wrapper binaries & configs
├── mvnw & mvnw.cmd                          <-- Executable Maven Wrapper scripts
├── pom.xml                                  <-- Enterprise Maven POM (Java 21, Spring Boot 3.3)
├── README.md                                <-- Quick start & developer guide
├── ROADMAP.md                               <-- Implementation roadmap & phase tracking
├── TICKET_FORGE_REPORT.md                   <-- Technical Architecture Blueprint
└── src/
    ├── main/
    │   ├── java/com/ticketforge/
    │   │   ├── TicketForgeApplication.java  <-- Main Spring Boot entrypoint
    │   │   ├── concurrency/                 <-- Distributed & Local Locking
    │   │   │   └── DistributedLockManager.java
    │   │   ├── config/                      <-- Config (OpenAPI, Async, Redis, GraphQL)
    │   │   │   ├── AsyncConfig.java
    │   │   │   ├── GraphQlConfig.java
    │   │   │   ├── OpenApiConfig.java
    │   │   │   └── RedisConfig.java
    │   │   ├── controller/                  <-- Spring MVC REST & SSE Stream Controllers
    │   │   │   ├── EventStreamController.java
    │   │   │   ├── ReservationController.java
    │   │   │   ├── SeatController.java
    │   │   │   ├── SseEmitterManager.java
    │   │   │   └── WaitlistController.java
    │   │   ├── dsa/                         <-- Generic Algorithmic In-Memory Engine
    │   │   │   ├── GenericMinHeap.java
    │   │   │   └── GenericRedBlackTree.java
    │   │   ├── dto/                         <-- Record DTOs (Requests & Responses)
    │   │   │   ├── ApiResponse.java
    │   │   │   ├── ExpandSeatsRequest.java
    │   │   │   ├── InitializeSeatsRequest.java
    │   │   │   ├── ReleaseSeatsRequest.java
    │   │   │   ├── ReservationRequest.java
    │   │   │   ├── ReservationResponse.java
    │   │   │   ├── SeatResponse.java
    │   │   │   ├── SystemStatusResponse.java
    │   │   │   ├── UpdatePriorityRequest.java
    │   │   │   └── WaitlistResponse.java
    │   │   ├── event/                       <-- Domain Event Bus (Pub/Sub)
    │   │   │   ├── RedisEventPublisher.java
    │   │   │   ├── RedisEventSubscriber.java
    │   │   │   └── TicketForgeEvent.java
    │   │   ├── exception/                   <-- RFC 7807 Exception Handlers
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── InvalidRequestException.java
    │   │   │   ├── RateLimitExceededException.java
    │   │   │   ├── ReservationNotFoundException.java
    │   │   │   ├── SeatNotFoundException.java
    │   │   │   ├── TicketForgeException.java
    │   │   │   ├── UserAlreadyInWaitlistException.java
    │   │   │   └── UserAlreadyReservedException.java
    │   │   ├── graphql/                     <-- GraphQL Query, Mutation & Subscription Resolvers
    │   │   │   ├── GraphQlExceptionResolver.java
    │   │   │   ├── ReservationGraphQLController.java
    │   │   │   ├── SeatGraphQLController.java
    │   │   │   ├── SystemStatusGraphQLController.java
    │   │   │   ├── TicketSubscriptionController.java
    │   │   │   └── WaitlistGraphQLController.java
    │   │   ├── model/                       <-- JPA Domain Entities & Status Enums
    │   │   │   ├── Reservation.java
    │   │   │   ├── Seat.java
    │   │   │   ├── SeatStatus.java
    │   │   │   ├── SeatTier.java
    │   │   │   ├── User.java
    │   │   │   ├── WaitlistEntry.java
    │   │   │   └── WaitlistStatus.java
    │   │   ├── ratelimit/                   <-- Anti-Bot Rate Limiter
    │   │   │   └── RedisRateLimiterService.java
    │   │   ├── repository/                  <-- Spring Data JPA Repositories
    │   │   │   ├── ReservationRepository.java
    │   │   │   ├── SeatRepository.java
    │   │   │   ├── UserRepository.java
    │   │   │   └── WaitlistRepository.java
    │   │   ├── scheduler/                   <-- Background TTL Expiry Scheduler
    │   │   │   └── TicketHoldTtlScheduler.java
    │   │   ├── security/                    <-- Supabase OAuth2 / Security Config
    │   │   │   ├── JwtAuthenticationConverter.java
    │   │   │   ├── SecurityConfig.java
    │   │   │   └── TicketForgeUserPrincipal.java
    │   │   └── service/                     <-- Core Transactional Business Logic
    │   │       ├── TicketForgeService.java
    │   │       └── TicketForgeServiceImpl.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       ├── application-staging.yml
    │       ├── db/migration/
    │       │   └── V1__init_ticketing_schema.sql
    │       └── graphql/
    │           └── schema.graphqls
    └── test/
        └── java/com/ticketforge/
            ├── TicketForgeApplicationTests.java
            ├── concurrency/
            │   └── DistributedLockTest.java
            ├── controller/                  <-- MockMvc REST & SSE Controller Tests
            │   ├── EventStreamControllerTest.java
            │   ├── ReservationControllerTest.java
            │   ├── SeatControllerTest.java
            │   └── WaitlistControllerTest.java
            ├── dsa/                         <-- Custom DSA Unit Tests
            │   ├── GenericMinHeapTest.java
            │   └── GenericRedBlackTreeTest.java
            ├── event/                       <-- Redis Pub/Sub Tests
            │   └── RedisEventPubSubTest.java
            ├── exception/                   <-- RFC 7807 Exception Tests
            │   └── GlobalExceptionHandlerTest.java
            ├── graphql/                     <-- GraphQlTester Integration Tests
            │   └── TicketForgeGraphQlTest.java
            ├── ratelimit/                   <-- Rate Limiter Tests
            │   └── RedisRateLimiterTest.java
            ├── repository/                  <-- Data JPA Integration Tests
            │   ├── ReservationRepositoryTest.java
            │   ├── SeatRepositoryTest.java
            │   ├── UserRepositoryTest.java
            │   └── WaitlistRepositoryTest.java
            ├── scheduler/                   <-- Scheduler Tests
            │   └── TicketHoldTtlSchedulerTest.java
            ├── security/                    <-- Security Authorization Tests
            │   └── SecurityAuthorizationTest.java
            └── service/                     <-- Service & Concurrency Tests
                ├── RedisCacheIntegrationTest.java
                ├── TicketForgeServiceConcurrencyTest.java
                └── TicketForgeServiceTest.java
```
