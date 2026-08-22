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
Once the application starts on port `8080`:

| Resource | URL | Credentials / Notes |
| :--- | :--- | :--- |
| 🌐 **Interactive Dashboard** | [http://localhost:8080](http://localhost:8080) | Live seating visualizer & action panel |
| 📑 **Swagger UI (OpenAPI 3)** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Interactive API exploration & testing |
| 📄 **OpenAPI Docs (JSON)** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | Raw OpenAPI 3 JSON definition |
| 🗄️ **H2 Database Console** | [http://localhost:8080/h2-console](http://localhost:8080/h2-console) | **JDBC URL:** `jdbc:h2:mem:ticketforge_db`<br>**User:** `sa`<br>**Password:** *(leave blank)* |
| 💓 **Actuator Health** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | System health check (`"status": "UP"`) |
| 📈 **Prometheus Metrics** | [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus) | Custom ticketing metrics & JVM stats |

---

## 🌍 Multi-Environment Configuration

TicketForge supports 3 environment profiles configured via `SPRING_PROFILES_ACTIVE`:

| Environment | Profile | Database | Auth Target | Connection Strategy |
| :--- | :--- | :--- | :--- | :--- |
| **Local Dev** | `dev` *(default)* | In-Memory H2 | Local Mock / Supabase Dev | Embedded In-Memory |
| **Staging** | `preprod` | Supabase Staging PG | Supabase Staging Auth | Direct Connection (Port `5432`) |
| **Production** | `prod` | Supabase Production PG | Supabase Production Auth | PgBouncer Pooler (Port `6543`) |

### Environment Variables

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active configuration profile | `dev` (or `preprod`, `prod`) |
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
  - Multi-profile configuration (`application-dev/preprod/prod.yml`)
  - Flyway V1 schema migration (`V1__init_ticketing_schema.sql`)
  - Supabase OAuth2 Resource Server & JWT Converter
  - OpenAPI 3 / Swagger UI & Actuator endpoints
- [ ] **Phase 2: JPA Data, Schema & Repositories** *(Next)*
  - JPA Entity models (`Seat`, `Reservation`, `WaitlistEntry`, `User`)
  - Optimistic locking (`@Version`) & Pessimistic locking queries
  - Spring Data JPA repositories with custom derived methods
- [ ] **Phase 3: Business Logic & In-Memory DSAs**
  - Generified `GenericRedBlackTree<K, V>` & Indexed `GenericMinHeap<T>`
  - `TicketForgeService` transactional operations & cascading re-allocations
  - Time-to-Live (TTL) auto-expiry background scheduler
  - Request idempotency caching
- [ ] **Phase 4: REST Controllers, Live SSE Stream & Docs**
  - REST API controllers with Jakarta Bean Validation
  - `EventStreamController` with `SseEmitter` real-time push
  - Centralized RFC 7807 `GlobalExceptionHandler`
- [ ] **Phase 5: Automated Testing Suite**
  - DSA algorithm unit tests
  - Mockito service layer unit tests
  - MockMvc API integration tests
  - Multi-threaded concurrency race condition stress tests
- [ ] **Phase 6: Modern UI & Cloud Containerization**
  - Glassmorphism Single-Page Dashboard (`index.html`, `styles.css`, `app.js`)
  - Multi-stage `Dockerfile` & Docker Compose
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
    │   │   ├── config/                      <-- OpenAPI & Async thread pool config
    │   │   │   ├── OpenApiConfig.java
    │   │   │   └── AsyncConfig.java
    │   │   └── security/                    <-- Supabase OAuth2 / Security config
    │   │       ├── SecurityConfig.java
    │   │       ├── JwtAuthenticationConverter.java
    │   │       └── TicketForgeUserPrincipal.java
    │   └── resources/
    │       ├── application.yml              <-- Shared settings & Actuator/OpenAPI
    │       ├── application-dev.yml          <-- Local H2 database profile
    │       ├── application-preprod.yml      <-- Supabase Staging profile
    │       ├── application-prod.yml         <-- Supabase Production profile (PgBouncer)
    │       └── db/migration/                <-- Flyway version-controlled SQL migrations
    │           └── V1__init_ticketing_schema.sql
    └── test/
        └── java/com/ticketforge/
            └── TicketForgeApplicationTests.java
```

---

## 📄 License

This project is licensed under the terms of the [MIT License](LICENSE).
