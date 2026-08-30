# ⚡ TicketForge

> **High-Concurrency Event Ticketing & Priority-Based Resource Allocation Engine**

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Auth-Supabase%20OAuth2-3ECF8E.svg?style=flat-square&logo=supabase)](https://supabase.com)
[![Database](https://img.shields.io/badge/Database-PostgreSQL%2016%20%2F%20H2-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Caching](https://img.shields.io/badge/Cache-Redis%20%2F%20Redisson-red.svg?style=flat-square&logo=redis)](https://redis.io)
[![GraphQL](https://img.shields.io/badge/API-GraphQL%20%2F%20REST%20%2F%20SSE-e10098.svg?style=flat-square&logo=graphql)](https://graphql.org)
[![Documentation](https://img.shields.io/badge/OpenAPI-3.0%20%2F%20Swagger-85EA2D.svg?style=flat-square&logo=swagger)](http://localhost:8080/swagger-ui.html)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

---

## 📖 Overview

**TicketForge** is a high-throughput event ticketing engine designed to handle flash-sale traffic spikes under strict seat-uniqueness constraints. It combines custom in-memory algorithmic data structures ($O(\log N)$ Min-Heap and Red-Black Tree) with Spring Boot 3.3, Redis distributed caching & locking, Spring for GraphQL, Supabase OAuth2/JWT authentication, and real-time Server-Sent Events (SSE).

### ✨ Key Features
* 🛡️ **Zero Race Conditions**: Multi-layered locking with Redisson distributed locks, row-level pessimistic write locks, and `@Version` optimistic locking.
* ⏳ **Priority Waitlist Engine**: Custom Indexed Min-Heap prioritizing VIP tiers ($P=3$) over standard tiers ($P=1$) with timestamp FIFO tie-breaking and $O(1)$ priority updates.
* ⚡ **Distributed Caching & Rate Limiting**: Redis Cache-Aside layer (`@Cacheable`) and token-bucket bot rate limiter with silent non-blocking in-memory fallback.
* 🔄 **Real-Time Push (SSE & WebSockets)**: Live seat map status updates via Server-Sent Events (`/api/v1/events/stream`) and GraphQL Subscriptions (`/graphql`).
* 📊 **GraphQL & REST Dual-Interface**: Full RESTful API with RFC 7807 problem details alongside a schema-first GraphQL engine with GraphiQL IDE.
* 🔐 **Supabase Identity & RBAC**: Stateless OAuth2 Resource Server verifying JWTs via Supabase JWKS with automated role and claim mapping (`ROLE_ADMIN` vs `ROLE_CUSTOMER`).
* ⏱️ **Time-Bound Seat Holding (TTL)**: Background scheduler automatically releasing `HELD` seats back into the available pool upon payment timeout.
* 🗄️ **Zero-Drift Migrations**: Flyway version-controlled migrations for PostgreSQL 16 and H2.

---

## 🛠️ Tech Stack

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language & Platform** | Java 21 LTS | Eclipse Temurin 21 with Virtual Threads capability |
| **Framework** | Spring Boot 3.3.2 | Spring MVC, Spring Data JPA, Spring Security 6, Spring for GraphQL |
| **Persistence** | PostgreSQL 16 / H2 | Relational storage with PgBouncer connection pooling and Flyway migrations |
| **In-Memory Engine** | Generic DSA Engine | Custom $O(\log N)$ Red-Black Tree & Indexed Min-Heap ($O(1)$ updates) |
| **Caching & Locking** | Redis & Redisson | Redis cache-aside, Redisson distributed locks (`RLock`), token bucket rate limiter |
| **Real-Time Streaming**| SSE & WebSockets | Server-Sent Events (`SseEmitter`) & GraphQL Subscriptions over WebSocket |
| **Security** | Supabase OAuth2 | Stateless JWT RS256 verification via JWKS with RBAC role converter |
| **Documentation** | OpenAPI 3 / Swagger | Springdoc OpenAPI UI and GraphiQL interactive development console |

---

## 🚀 Quick Start

### 📋 Prerequisites
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
./mvnw spring-boot:run
```

#### Option B: Full Stack with Docker Compose (App + PostgreSQL + Redis)
```bash
docker compose up --build
```

---

## 🌐 Interactive Endpoints

Once the application starts on port `8080`:

| Resource | URL | Description |
| :--- | :--- | :--- |
| 📑 **Swagger UI (REST Docs)** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Interactive OpenAPI 3 explorer |
| 🔮 **GraphiQL IDE (GraphQL)** | [http://localhost:8080/graphiql](http://localhost:8080/graphiql) | Interactive GraphQL query & subscription console |
| 📄 **OpenAPI Schema (JSON)** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | Machine-readable API schema |
| 🗄️ **H2 Database Console** | [http://localhost:8080/h2-console](http://localhost:8080/h2-console) | Local in-memory DB viewer (`dev` profile) |
| 💓 **Actuator Health** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | System health status check |
| 📈 **Prometheus Metrics** | [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus) | Micrometer metrics for Prometheus scraping |

---

## 🔌 API Reference & Specifications

### 📡 RESTful API Endpoints (`/api/v1`)

| Method & Endpoint | Role | Request Parameters / Body | Response Payload | HTTP Status |
| :--- | :---: | :--- | :--- | :---: |
| **`GET /api/v1/seats/availability`** | Public | _None_ | `ApiResponse<SystemStatusResponse>` | `200 OK` |
| **`GET /api/v1/seats`** | Authenticated | _None_ | `ApiResponse<List<SeatResponse>>` | `200 OK` |
| **`GET /api/v1/seats/{seatNumber}`** | Authenticated | Path: `seatNumber: Integer` | `ApiResponse<SeatResponse>` | `200 OK` |
| **`POST /api/v1/seats/initialize`** | **Admin Only** | Body: `{"seatCount": 100}` | `ApiResponse<SystemStatusResponse>` | `200 OK` |
| **`POST /api/v1/seats/expand`** | **Admin Only** | Body: `{"additionalCount": 20}` | `ApiResponse<SystemStatusResponse>` | `200 OK` |
| **`POST /api/v1/reservations`** | Customer | Body: `{"userId": "usr_101", "priority": 3}` | `ApiResponse<ReservationResponse>` | `201 Created` / `202 Accepted` |
| **`DELETE /api/v1/reservations/{seatNumber}`** | Customer / Admin | Path: `seatNumber`, Query: `?userId=usr_101` | `ApiResponse<Void>` | `200 OK` |
| **`GET /api/v1/reservations`** | Customer / Admin | _None_ | `ApiResponse<List<ReservationResponse>>` | `200 OK` |
| **`GET /api/v1/reservations/user/{userId}`** | Customer / Admin | Path: `userId: String` | `ApiResponse<ReservationResponse>` | `200 OK` |
| **`POST /api/v1/reservations/release-range`** | **Admin Only** | Body: `{"fromUserId": "usr_10", "toUserId": "usr_50"}` | `ApiResponse<List<Integer>>` | `200 OK` |
| **`GET /api/v1/waitlist`** | Customer / Admin | _None_ | `ApiResponse<List<WaitlistResponse>>` | `200 OK` |
| **`PATCH /api/v1/waitlist/{userId}`** | Customer / Admin | Path: `userId`, Body: `{"newPriority": 3}` | `ApiResponse<Void>` | `200 OK` |
| **`DELETE /api/v1/waitlist/{userId}`** | Customer / Admin | Path: `userId: String` | `ApiResponse<Void>` | `200 OK` |
| **`GET /api/v1/events/stream`** | Public | _None_ (`Accept: text/event-stream`) | Server-Sent Events (SSE) Stream | `200 OK` |

#### 📝 Example REST Request & Response Payloads

##### 1. Reserve Seat (`POST /api/v1/reservations`)
* **Request:**
  ```json
  {
    "userId": "usr_402",
    "priority": 3
  }
  ```
* **Success Response (`201 Created`):**
  ```json
  {
    "success": true,
    "message": "Seat 14 successfully reserved",
    "data": {
      "id": 14,
      "seatNumber": 14,
      "userId": "usr_402",
      "status": "RESERVED",
      "reservedAt": "2026-08-28T22:30:00Z"
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

##### 2. Venue Availability (`GET /api/v1/seats/availability`)
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

##### 3. Standard RFC 7807 Error Response (e.g. `409 Conflict`)
```json
{
  "type": "https://ticketforge.com/errors/user-already-reserved",
  "title": "User Already Has Reservation",
  "status": 409,
  "detail": "User 'usr_402' already holds active reservation for Seat 14",
  "instance": "/api/v1/reservations",
  "timestamp": "2026-08-28T22:30:00.123Z"
}
```

---

### 🔮 GraphQL API Reference (`/graphql`)

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

# 2. Reserve a Seat (or Join Waitlist)
mutation BookTicket {
  reserveSeat(userId: "usr_101", priority: 3) {
    seatNumber
    userId
    status
    reservedAt
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

## 🌍 Environment Configuration

| Profile | Command / Variable | Database | Caching & Locking | Auth Provider |
| :--- | :--- | :--- | :--- | :--- |
| **`dev`** *(default)* | `SPRING_PROFILES_ACTIVE=dev` | In-Memory H2 | Local In-Memory Fallback | Local Mock / Supabase Dev |
| **`staging`** | `SPRING_PROFILES_ACTIVE=staging` | Supabase Staging PG | Redis / Redisson | Supabase Staging Auth |
| **`prod`** | `SPRING_PROFILES_ACTIVE=prod` | Supabase Prod (PgBouncer: 6543) | Redis Cluster / Redisson | Supabase Prod Auth |

### Environment Variables
* `SPRING_PROFILES_ACTIVE`: Active profile (`dev`, `staging`, `prod`)
* `PORT`: Server port (default: `8080`)
* `SUPABASE_PROJECT_ID`: Supabase Project Reference ID
* `SUPABASE_DB_URL`: JDBC URL to PostgreSQL
* `SUPABASE_DB_USER`: Database username
* `SUPABASE_DB_PASSWORD`: Database password
* `REDIS_URL`: Redis connection URL (`redis://localhost:6379`)

---

## 🧪 Testing Suite

Execute all automated unit tests, MockMvc REST tests, GraphQlTester tests, and concurrency stress tests:
```bash
./mvnw clean test
```

---

## 📚 Project Documentation

* 🗺️ **[ROADMAP.md](ROADMAP.md)**: Implementation roadmap, phase deliverables, and file structure tree.
* 🏛️ **[TICKET_FORGE_REPORT.md](TICKET_FORGE_REPORT.md)**: Complete technical architecture blueprint and algorithm design.

---

## 📄 License

This project is licensed under the terms of the [MIT License](LICENSE).
