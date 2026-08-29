# ⚡ TicketForge

> **High-Concurrency Event Ticketing & Priority-Based Resource Allocation Engine**

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Auth-Supabase%20OAuth2-3ECF8E.svg?style=flat-square&logo=supabase)](https://supabase.com)
[![Database](https://img.shields.io/badge/Database-PostgreSQL%2016%20%2F%20H2-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Migrations](https://img.shields.io/badge/Migrations-Flyway-CC0200.svg?style=flat-square&logo=flyway)](https://flywaydb.org/)
[![Documentation](https://img.shields.io/badge/OpenAPI-3.0%20%2F%20Swagger-85EA2D.svg?style=flat-square&logo=swagger)](http://localhost:8080/swagger-ui.html)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

---

## 📖 Overview

**TicketForge** is a high-throughput event ticketing and waitlist re-allocation platform designed to handle flash-sale traffic spikes under strict seat-uniqueness constraints. It combines **custom algorithmic data structures ($O(\log N)$ in-memory Min-Heap & Red-Black Tree)** with **enterprise Spring Boot 3.3, Supabase Auth, Flyway schema migrations, and real-time Server-Sent Events (SSE)**.

### ✨ Key Features
* 🛡️ **Zero Race-Condition Booking**: Multi-layered concurrency defense utilizing row-level pessimistic write locks, `@Version` optimistic locking, and thread-safe internal structures.
* ⏳ **Priority Waitlist Engine**: Custom Indexed Min-Heap prioritizing VIP tiers ($P=3$) over standard tiers ($P=1$) with timestamp FIFO tie-breaking and $O(1)$ priority updates.
* 🔐 **Supabase Identity & RBAC**: Stateless OAuth2 Resource Server verifying JWTs via Supabase JWKS with automated role and claim mapping (`ROLE_ADMIN` vs `ROLE_CUSTOMER`).
* 🔄 **Real-Time Live Push (SSE)**: Streams instant seat status updates and automated waitlist promotions directly to connected clients without polling.
* ⏱️ **Time-Bound Seat Holding (TTL)**: Background scheduler automatically releasing `HELD` seats back into the available pool upon payment timeout.
* 🗄️ **Zero-Drift Multi-Environment Migrations**: Version-controlled Flyway SQL scripts guaranteeing identical schemas across **Dev**, **Preprod**, and **Prod**.
* 📊 **Production Observability**: Built-in Spring Boot Actuator and Prometheus metrics gauges (`ticketforge.seats.available`, `ticketforge.waitlist.depth`).

---

## 🏛️ System Architecture

```
 ┌────────────────────────────────────────────────────────────────────────┐
 │           Glassmorphism Web Dashboard (Vanilla JS + SSE Live Stream)   │
 └───────────────────────────────────┬────────────────────────────────────┘
                                     │ HTTP REST / SSE Stream
                                     ▼
 ┌────────────────────────────────────────────────────────────────────────┐
 │               Spring Security / OAuth2 Resource Server                 │
 │     Stateless JWT Verification via Supabase JWKS Endpoint (RS256)      │
 └───────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
 ┌────────────────────────────────────────────────────────────────────────┐
 │                    Spring MVC / REST API Layer                         │
 │     [SeatController]   [ReservationController]   [WaitlistController]  │
 └───────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
 ┌────────────────────────────────────────────────────────────────────────┐
 │                    Service & Business Logic Layer                      │
 │   ┌───────────────────────────────┐ ┌──────────────────────────────┐   │
 │   │  High-Throughput In-Memory    │ │   Concurrency & Lock Manager │   │
 │   │  Generic DSA Matching Engine  │ │  (Pessimistic / Optimistic)  │   │
 │   └───────────────────────────────┘ └──────────────────────────────┘   │
 └───────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
 ┌────────────────────────────────────────────────────────────────────────┐
 │               Persistence Layer (Spring Data JPA & Flyway)             │
 │     [SeatRepository]   [ReservationRepository]   [WaitlistRepository]  │
 └───────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
 ┌────────────────────────────────────────────────────────────────────────┐
 │           Relational Database (H2 Local / Supabase PostgreSQL 16)      │
 │    Managed PgBouncer Connection Pooler (Port 6543 Transaction Mode)   │
 └────────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start (Local Development)

### 📋 Prerequisites
* **Java 21 LTS** or higher installed (`java -version`)
* **Git**

*(Maven is bundled via `./mvnw` — no separate installation required)*

### 1. Clone the Repository
```bash
git clone https://github.com/settivishal/ticket-forge.git
cd ticket-forge
```

### 2. Run the Application (Dev Profile)
By default, the application starts with the `dev` profile using an in-memory H2 database:
```bash
./mvnw spring-boot:run
```

### 3. Access Interactive Endpoints
Once the application starts on port `8080` (or visit the live Staging cloud deployment at [https://ticketforge-staging.onrender.com](https://ticketforge-staging.onrender.com)):

| Resource | Local URL | Live Cloud Staging (Render) |
| :--- | :--- | :--- |
| 🌐 **Interactive Dashboard** | [http://localhost:8080](http://localhost:8080) | [https://ticketforge-staging.onrender.com](https://ticketforge-staging.onrender.com) |
| 📑 **Swagger UI (OpenAPI 3)** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | [https://ticketforge-staging.onrender.com/swagger-ui.html](https://ticketforge-staging.onrender.com/swagger-ui.html) |
| 📄 **OpenAPI Docs (JSON)** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | [https://ticketforge-staging.onrender.com/v3/api-docs](https://ticketforge-staging.onrender.com/v3/api-docs) |
| 🗄️ **H2 Database Console** | [http://localhost:8080/h2-console](http://localhost:8080/h2-console) | *(Local dev only)* |
| 💓 **Actuator Health** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | [https://ticketforge-staging.onrender.com/actuator/health](https://ticketforge-staging.onrender.com/actuator/health) |
| 📈 **Prometheus Metrics** | [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus) | [https://ticketforge-staging.onrender.com/actuator/prometheus](https://ticketforge-staging.onrender.com/actuator/prometheus) |


---

## 🌍 Multi-Environment Configuration

TicketForge supports 3 environment profiles configured via `SPRING_PROFILES_ACTIVE`:

| Environment | Profile | Branch | Database | Auth Target | Connection Strategy |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Local Feature** | `dev` *(default)* | `feature/<name>` | In-Memory H2 | Local Mock / Supabase Dev | Embedded In-Memory |
| **Local Integration** | `dev` *(default)* | `dev` | In-Memory H2 / Docker PG | Local Mock / Supabase Dev | Local Embedded / Docker |
| **Cloud Staging** | `staging` | `staging` | Supabase Staging PG | Supabase Staging Auth | Direct Connection (Port `5432`) |
| **Production** | `prod` | `main` | Supabase Production PG | Supabase Production Auth | PgBouncer Pooler (Port `6543`) |

### 🌿 Git Branching & Promotion Strategy
1. **`feature/<name>`** (e.g. `feature/phase-2-jpa`): Individual feature/task branches. Branch off `dev`.
2. **`dev`**: Local integration & aggregation branch. Collects and integrates feature branches locally with fast H2/Docker testing.
3. **`staging`**: Cloud Staging deployment branch. Pull Requests from `dev` $\rightarrow$ `staging` trigger automated Staging deployment & QA tests on Supabase Staging.
4. **`main`**: Production release branch. Pull Requests from `staging` $\rightarrow$ `main` trigger automated zero-downtime Production deployment on Supabase Prod.



### Environment Variables

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active configuration profile | `dev` (or `staging`, `prod`) |
| `PORT` | Server HTTP port | `8080` |
| `SUPABASE_PROJECT_ID` | Supabase Project Reference ID | `your-project-id` |
| `SUPABASE_DB_URL` | JDBC Connection URL to PostgreSQL | `jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0` |
| `SUPABASE_DB_USER` | Supabase database user | `postgres.your-project-id` |
| `SUPABASE_DB_PASSWORD` | Supabase database password | *Secret* |

---

## 👥 Roles & Authorization (RBAC)

Identity and access are managed via **Supabase Auth JWTs**:

```
                      ┌────────────────────────┐
                      │    Supabase Auth IdP   │
                      └───────────┬────────────┘
                                  │
                  ┌───────────────┴───────────────┐
                  ▼                               ▼
       ┌────────────────────┐          ┌────────────────────┐
       │     ROLE_ADMIN     │          │   ROLE_CUSTOMER    │
       └──────────┬─────────┘          └──────────┬─────────┘
                  │                               │
        Venue & Ops Management            Ticketing & Waitlist
        - Initialize capacity             - Reserve seat
        - Expand inventory                - View live seat map (SSE)
        - Batch release ranges            - Cancel own reservation
        - System health & Actuator        - Leave waitlist
                                                  │
                                       ┌──────────┴──────────┐
                                       ▼ Priority Tiers (Heap)
                                       ⭐ Tier 3: VIP (P=3)
                                       ⭐ Tier 2: Premium (P=2)
                                       ⭐ Tier 1: Standard (P=1)
```

---

## 🔌 RESTful API Summary

| Endpoint | Method | Role | Purpose |
| :--- | :---: | :---: | :--- |
| `/api/v1/seats/availability` | `GET` | Public / Customer | Get available seat count and waitlist depth |
| `/api/v1/events/stream` | `GET` | Public / Customer | Server-Sent Events (SSE) live booking updates |
| `/api/v1/reservations` | `POST` | Customer | Reserve a seat or enter priority waitlist |
| `/api/v1/reservations/{seatNumber}` | `DELETE` | Customer (Own) / Admin | Cancel reservation & auto-promote waitlist |
| `/api/v1/reservations` | `GET` | Customer / Admin | List all active reservations |
| `/api/v1/waitlist/{userId}` | `PATCH` | Customer (Own) / Admin | Update priority level in waitlist |
| `/api/v1/waitlist/{userId}` | `DELETE` | Customer (Own) / Admin | Leave waitlist queue |
| `/api/v1/seats/initialize` | `POST` | **Admin Only** | Initialize venue seat inventory |
| `/api/v1/seats/expand` | `POST` | **Admin Only** | Add more seats to inventory |
| `/api/v1/reservations/release-range` | `POST` | **Admin Only** | Batch cancel user ID range $[u_1, u_2]$ |

---

## 🧪 Testing Suite

Run all automated unit tests, integration tests, and concurrency stress tests:
```bash
./mvnw clean test
```

### Concurrency Stress Test Sample
Simulates 50 simultaneous threads competing for 5 remaining seats to ensure zero double-bookings:
```java
@Test
@DisplayName("50 Concurrent requests for 5 remaining seats must yield exactly 5 reservations and 45 waitlist entries")
void testConcurrentSeatBookingNoDoubleBooking() throws InterruptedException {
    ticketForgeService.initialize(5);
    // ... concurrent ExecutorService fire with CountDownLatch
    assertThat(seatRepository.countByStatus(SeatStatus.RESERVED)).isEqualTo(5);
    assertThat(waitlistRepository.countByStatus(WaitlistStatus.WAITING)).isEqualTo(45);
}
```

---

## 🗺️ Implementation Roadmap

- [x] **Phase 1: Build & Config Foundation** *(Completed)*
  - Maven `pom.xml`, Java 21 LTS, Spring Boot 3.3.2, `./mvnw` wrapper
  - Multi-profile configuration (`application-dev/staging/prod.yml`)
  - Flyway V1 schema migration (`V1__init_ticketing_schema.sql`)
  - Supabase OAuth2 Resource Server & JWT Converter
  - OpenAPI 3 / Swagger UI & Actuator endpoints
  - Cloud Staging deployment live on Render Free Tier
- [x] **Phase 2: JPA Data, Schema & Repositories** *(Completed)*
  - JPA Entity models (`Seat`, `Reservation`, `WaitlistEntry`, `User`)
  - Optimistic locking (`@Version`) & Pessimistic locking queries (`@Lock(PESSIMISTIC_WRITE)`)
  - Jakarta Bean Validation Request/Response Record DTOs
  - 14 `@DataJpaTest` automated integration tests
- [x] **Phase 3: Business Logic & In-Memory DSAs** *(Completed)*
  - Generified `GenericRedBlackTree<K, V>` & Indexed `GenericMinHeap<T>`
  - `TicketForgeService` transactional operations & cascading re-allocations
  - Time-to-Live (TTL) auto-expiry background scheduler (`TicketHoldTtlScheduler`)
  - 43 automated tests (including 100-thread multi-threaded concurrency race condition stress tests)
- [x] **Phase 4: Distributed Caching & Redis Integration** *(Completed)*
  - `spring-boot-starter-data-redis` & Redisson client integration
  - Redis cache-aside layer for seat availability & venue layout (`@Cacheable`, `@CacheEvict`)
  - Redisson distributed locks (`RLock`) with zero-latency local fallback
  - Redis token-bucket rate limiting to block ticketing bots / scalpers
  - Redis Pub/Sub for cross-instance real-time event broadcasting
- [x] **Phase 5: GraphQL Engine & Schema Architecture** *(Completed)*
  - `spring-boot-starter-graphql` schema-first architecture (`schema.graphqls`)
  - Query resolvers (flexible seat maps, waitlist positions, system metrics)
  - Mutation resolvers (book seat, cancel reservation, update priority)
  - Subscription resolvers over WebSocket (`/graphql`) for live seat state changes
  - Batch mapping / `DataLoader` implementation to eliminate GraphQL N+1 problem
  - GraphiQL interactive development playground (`/graphiql`)
- [x] **Phase 6: REST Controllers, Live SSE Stream & Security** *(Completed)*
  - REST API controllers with Jakarta Bean Validation (`SeatController`, `ReservationController`, `WaitlistController`)
  - `EventStreamController` & `SseEmitterManager` with real-time SSE push & heartbeat pings
  - Stateless Supabase OAuth2 / JWT security filter chain with RBAC routes
  - Centralized RFC 7807 `GlobalExceptionHandler` with standardized `ProblemDetail` responses
  - 95 automated unit, concurrency, security, and MockMvc integration tests
- [ ] **Phase 7: Comprehensive Automated Testing Suite** *(Next)*
  - DSA algorithm unit tests (Generic Red-Black Tree, Indexed Min-Heap)
  - Mockito service layer unit tests
  - MockMvc REST & GraphQlTester integration tests
  - Redis caching & Redisson distributed locking tests
  - Multi-threaded 100-thread concurrency & race condition stress tests
- [ ] **Phase 8: Modern UI & Cloud Containerization**
  - Glassmorphism Single-Page Dashboard (REST + GraphQL + SSE/WebSocket)
  - Multi-stage `Dockerfile` & Docker Compose (App + PostgreSQL + Redis)
  - Production deployment to Fly.io / Render

---

## 📂 Project Structure

```
ticket-forge/
├── .mvn/wrapper/                            <-- Maven Wrapper binaries & configs
├── mvnw & mvnw.cmd                          <-- Executable Maven Wrapper scripts
├── pom.xml                                  <-- Enterprise Maven POM (Java 21, Spring Boot 3.3)
├── README.md                                <-- Project documentation & guide
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

---

## 📄 License

This project is licensed under the terms of the [MIT License](LICENSE).

