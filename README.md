# TicketForge

> Event ticketing backend with concurrency-safe seat allocation and a priority waitlist.

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Auth-Supabase%20OAuth2-3ECF8E.svg?style=flat-square&logo=supabase)](https://supabase.com)
[![Database](https://img.shields.io/badge/Database-PostgreSQL%20%2F%20H2-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Caching](https://img.shields.io/badge/Cache-Redis%20%2F%20Redisson-red.svg?style=flat-square&logo=redis)](https://redis.io)
[![GraphQL](https://img.shields.io/badge/API-GraphQL%20%2F%20REST%20%2F%20SSE-e10098.svg?style=flat-square&logo=graphql)](https://graphql.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

---

## Overview

TicketForge is an event ticketing backend built to handle flash-sale contention: many users competing for a fixed set of seats, with a priority waitlist for everyone who misses out. It is a Spring Boot 3.3 / Java 21 service with REST and GraphQL APIs, Redis-backed locking, caching and rate limiting, Supabase JWT authentication, and live updates over Server-Sent Events.

### Features
* **Concurrency-safe allocation**: Redisson distributed locks around each write transaction, row-level pessimistic locks on seats, `@Version` optimistic locking on reservations, and unique constraints in the schema. A 100-thread stress test asserts zero double bookings.
* **Priority waitlist**: an indexed min-heap ordered by priority tier (1-5), then join time (FIFO). The index gives O(1) membership lookup and O(log n) priority updates and removals.
* **Seat holds with TTL**: a seat can be held, then confirmed. A scheduler releases unconfirmed holds after their TTL and promotes the next waitlisted user.
* **Caching and rate limiting**: Redis cache-aside for read endpoints and a token-bucket rate limiter on writes, both falling back to in-memory when Redis is unavailable.
* **Live updates**: seat events are published over Redis pub/sub and pushed to clients via SSE (`/api/v1/events/stream`) and GraphQL subscriptions.
* **REST and GraphQL**: a REST API with RFC 7807 problem details and a schema-first GraphQL API, both documented (Swagger UI, GraphiQL).
* **Auth and RBAC**: stateless JWT verification against Supabase JWKS. The acting user always comes from the token, never the request body. Role and `priority_tier` are read only from admin-controlled `app_metadata`, never user-editable `user_metadata`. Admin operations are guarded with `@PreAuthorize` on both REST and GraphQL.
* **Migrations and observability**: Flyway migrations, Actuator health checks, and Prometheus metrics.

### Design tradeoffs
* **In-memory structures are per instance.** The seat heap, waitlist heap and user-to-seat Red-Black Tree are hydrated from Postgres at startup and kept in sync on every write. Distributed locks serialize writes across instances, but each instance holds its own copy of these structures, so the engine is designed to run as a single instance. Scaling out would mean making Postgres the only source of truth (for example `SELECT ... FOR UPDATE SKIP LOCKED` for allocation and an indexed query for the waitlist head).
* **Why a Red-Black Tree for user-to-seat lookups?** A hash map would give O(1) point lookups, but the tree keeps user IDs ordered, which supports the admin range release (`release-range`) in O(log n + k).
* **One venue.** The current model is a single seat inventory with one booking per user. Multi-event support (events, per-event inventory, orders) is the next major change.

---

## Tech Stack

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language & Platform** | Java 21 | Eclipse Temurin 21 with virtual threads enabled |
| **Framework** | Spring Boot 3.3.2 | Spring MVC, Spring Data JPA, Spring Security 6, Spring for GraphQL |
| **Persistence** | PostgreSQL / H2 | Supabase Postgres via its connection pooler; H2 for local dev and tests; Flyway migrations |
| **Data structures** | Custom Java | Red-Black Tree (ordered user-to-seat index) and indexed min-heap (seat pool, waitlist) |
| **Caching & Locking** | Redis & Redisson | Cache-aside, distributed locks (`RLock`), token-bucket rate limiter, pub/sub |
| **Real-time** | SSE & WebSockets | `SseEmitter` stream and GraphQL subscriptions |
| **Security** | Supabase Auth | JWT verification via JWKS with role mapping |
| **Documentation** | OpenAPI 3 | Springdoc Swagger UI and GraphiQL |

---

## Quick Start

### Prerequisites
* **Java 21 LTS** or higher installed (`java -version`)
* **Git**
* *(Optional)* **Docker & Docker Compose** (for running PostgreSQL & Redis locally)

### 1. Clone the Repository
```bash
git clone https://github.com/settivishal/ticket-forge.git
cd ticket-forge
```

### 2. Run the Application

#### Option A: Quick Start with In-Memory H2 (Dev Profile)
```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Option B: Full Stack with Docker Compose (App + PostgreSQL + Redis)
```bash
docker compose up --build
```

---

## Interactive Endpoints

Once the application starts on port `8080`:

| Resource | URL | Description |
| :--- | :--- | :--- |
| **Swagger UI (REST Docs)** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Interactive OpenAPI 3 explorer |
| **GraphiQL IDE (GraphQL)** | [http://localhost:8080/graphiql](http://localhost:8080/graphiql) | Interactive GraphQL query & subscription console |
| **OpenAPI Schema (JSON)** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | Machine-readable API schema |
| **H2 Database Console** | [http://localhost:8080/h2-console](http://localhost:8080/h2-console) | Local in-memory DB viewer (`dev` profile) |
| **Actuator Health** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | System health status check |
| **Prometheus Metrics** | [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus) | Micrometer metrics for Prometheus scraping |

---

## API Reference & Specifications

### RESTful API Endpoints (`/api/v1`)

> **Identity is never supplied by the client.** The acting user and their priority tier are
> taken from the authenticated JWT, so no endpoint accepts a `userId` for the caller.
> Endpoints that name a `{userId}` act on *another* user and are therefore admin-only.
> Seat and waitlist listings only show a user id to that user or to an admin, and live
> events (SSE, GraphQL subscriptions) never carry user ids.

| Method & Endpoint | Role | Request Parameters / Body | Response Payload | HTTP Status |
| :--- | :---: | :--- | :--- | :---: |
| **`GET /api/v1/seats/availability`** | Public | _None_ | `ApiResponse<SystemStatusResponse>` | `200 OK` |
| **`GET /api/v1/seats`** | Authenticated | _None_ | `ApiResponse<List<SeatResponse>>` | `200 OK` |
| **`GET /api/v1/seats/{seatNumber}`** | Authenticated | Path: `seatNumber: Integer` | `ApiResponse<SeatResponse>` | `200 OK` |
| **`POST /api/v1/seats/initialize`** | **Admin Only** | Body: `{"seatCount": 100}` | `ApiResponse<SystemStatusResponse>` | `200 OK` |
| **`POST /api/v1/seats/expand`** | **Admin Only** | Body: `{"additionalCount": 20}` | `ApiResponse<SystemStatusResponse>` | `200 OK` |
| **`POST /api/v1/reservations`** | Customer | Body: `{"seatNumber": 42}` _(optional)_ | `ApiResponse<ReservationResponse>` | `201 Created` / `202 Accepted` |
| **`POST /api/v1/reservations/confirm`** | Customer | _None_ | `ApiResponse<ReservationResponse>` | `200 OK` |
| **`DELETE /api/v1/reservations/{seatNumber}`** | Customer | Path: `seatNumber` | `ApiResponse<Void>` | `200 OK` |
| **`GET /api/v1/reservations/me`** | Customer | _None_ | `ApiResponse<ReservationResponse>` | `200 OK` |
| **`GET /api/v1/reservations`** | **Admin Only** | _None_ | `ApiResponse<List<ReservationResponse>>` | `200 OK` |
| **`GET /api/v1/reservations/user/{userId}`** | **Admin Only** | Path: `userId: String` | `ApiResponse<ReservationResponse>` | `200 OK` |
| **`POST /api/v1/reservations/release-range`** | **Admin Only** | Body: `{"fromUserId": "usr_10", "toUserId": "usr_50"}` | `ApiResponse<List<Integer>>` | `200 OK` |
| **`GET /api/v1/waitlist`** | Customer / Admin | _None_ | `ApiResponse<List<WaitlistResponse>>` | `200 OK` |
| **`DELETE /api/v1/waitlist`** | Customer | _None_ (removes the caller) | `ApiResponse<Void>` | `200 OK` |
| **`PATCH /api/v1/waitlist/{userId}`** | **Admin Only** | Path: `userId`, Body: `{"newPriority": 3}` | `ApiResponse<Void>` | `200 OK` |
| **`DELETE /api/v1/waitlist/{userId}`** | **Admin Only** | Path: `userId: String` | `ApiResponse<Void>` | `200 OK` |
| **`GET /api/v1/events/stream`** | Public | _None_ (`Accept: text/event-stream`) | Server-Sent Events (SSE) Stream | `200 OK` |

#### Example REST Request & Response Payloads

##### 1. Reserve Seat (`POST /api/v1/reservations`)
* **Request:** _(omit `seatNumber` to be allocated the best available seat)_
  ```json
  {
    "seatNumber": 14
  }
  ```
* **Success Response (`201 Created`):**
  ```json
  {
    "success": true,
    "message": "Seat 14 successfully reserved",
    "data": {
      "id": 14,
      "userId": "usr_402",
      "seatNumber": 14,
      "tier": "VIP",
      "reservedAt": "2026-08-28T22:30:00Z",
      "expiresAt": null
    },
    "timestamp": "2026-08-28T22:30:00.123Z"
  }
  ```
* **Full Capacity Response (`202 Accepted`):**
  ```json
  {
    "success": true,
    "message": "Venue at full capacity. User usr_402 added to priority waitlist",
    "data": null,
    "timestamp": "2026-08-28T22:30:00.123Z"
  }
  ```

##### 2. Hold then Confirm (`POST /api/v1/reservations/confirm`)

A hold reserves a seat temporarily; confirming it before the TTL elapses turns it into a
booking. An unconfirmed hold is released back to the pool by the TTL scheduler.

* **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "message": "Seat 14 confirmed",
    "data": {
      "id": 14,
      "userId": "usr_402",
      "seatNumber": 14,
      "tier": "VIP",
      "reservedAt": "2026-08-28T22:30:00Z",
      "expiresAt": null
    }
  }
  ```

##### 3. Venue Availability (`GET /api/v1/seats/availability`)
* **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "message": "System status retrieved successfully",
    "data": {
      "totalSeats": 100,
      "availableSeats": 18,
      "heldSeats": 2,
      "reservedSeats": 80,
      "waitlistCount": 12
    },
    "timestamp": "2026-08-28T22:30:00.123Z"
  }
  ```

##### 4. Standard RFC 7807 Error Response (e.g. `409 Conflict`)
```json
{
  "type": "https://ticketforge.com/errors/user-already-reserved",
  "title": "User Already Has Reservation",
  "status": 409,
  "detail": "User usr_402 already has an active reservation or hold",
  "instance": "/api/v1/reservations",
  "timestamp": "2026-08-28T22:30:00.123Z"
}
```

---

### GraphQL API Reference (`/graphql`)

Interactive development console accessible at: **[http://localhost:8080/graphiql](http://localhost:8080/graphiql)**

```graphql
# 1. Fetch Venue Seat Layout & System Metrics
query GetVenueStatus {
  systemStatus {
    totalSeats
    availableSeats
    reservedSeats
    waitlistCount
  }
  seats(status: AVAILABLE) {
    seatNumber
    tier
    status
  }
}

# 2. Reserve a Seat (or Join Waitlist). Omit seatNumber for best available.
#    The acting user comes from the bearer token, never from arguments.
mutation BookTicket {
  reserveSeat(seatNumber: 42) {
    seatNumber
    userId
    tier
    reservedAt
  }
}

# 2b. Hold a seat, then confirm it before the TTL expires
mutation HoldTicket {
  holdSeat(ttlSeconds: 120, seatNumber: 42) {
    seatNumber
    expiresAt
  }
}

mutation ConfirmTicket {
  confirmHold {
    seatNumber
    expiresAt
  }
}

# 3. Live WebSocket Subscription for Real-Time Event Stream
subscription WatchSeatEvents {
  seatEvents {
    eventType
    seatNumber
    userId
    message
    timestamp
  }
}
```

---

## Environment Configuration

| Profile | Command / Variable | Database | Caching & Locking | Auth Provider |
| :--- | :--- | :--- | :--- | :--- |
| **`dev`** | `SPRING_PROFILES_ACTIVE=dev` | In-Memory H2 | Local In-Memory Fallback | Local Mock / Supabase Dev |
| **`staging`** | `SPRING_PROFILES_ACTIVE=staging` | Supabase Postgres (pooler, port 6543) | Redis / Redisson | Supabase Auth |
| **`prod`** *(default)* | `SPRING_PROFILES_ACTIVE=prod` | Supabase Postgres (pooler, port 6543) | Redis / Redisson (TLS) | Supabase Auth |

The Render deployment (`render.yaml`) currently runs the `staging` profile.

### Environment Variables
* `SPRING_PROFILES_ACTIVE`: Active profile (`dev`, `staging`, `prod`)
* `PORT`: Server port (default: `8080`)
* `SUPABASE_PROJECT_ID`: Supabase Project Reference ID
* `SUPABASE_DB_URL`: JDBC URL to PostgreSQL
* `SUPABASE_DB_USER`: Database username
* `SUPABASE_DB_PASSWORD`: Database password
* `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL`: Redis connection
* `REDISSON_ENABLED`: use Redisson for distributed locks and rate limiting (default `true`)
* `RATE_LIMIT_RPS`: write requests per second per user (default `5`)
* `CORS_ALLOWED_ORIGINS`: comma-separated origins allowed to call the API cross-origin (default `http://localhost:3000`)

---

## Testing Suite

Execute all automated unit tests, MockMvc REST tests, GraphQlTester tests, and concurrency stress tests:
```bash
cd backend && ./mvnw clean test
```

---

## License

This project is licensed under the terms of the [MIT License](LICENSE).
