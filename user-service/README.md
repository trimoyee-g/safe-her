# User Service — safeher Platform

Manages user profiles for the safeher crowdsourced safety ratings platform. Port **8082**.

## Responsibilities

- Create / read / update / soft-delete user profiles
- Serve public profiles to unauthenticated callers
- Keyword search across usernames, display names, and cities
- Publish domain events to Kafka (`user.created`, `user.updated`, `user.deleted`)
- Expose internal Feign-compatible endpoints for other services

## Technology

| Concern | Choice |
|---|---|
| Framework | Spring Boot 3.2, Spring Cloud 2023 |
| Database | PostgreSQL 16 + Flyway migrations |
| Auth | JWT (validated from Auth Service secret) |
| Messaging | Apache Kafka |
| Discovery | Netflix Eureka |
| Fault tolerance | Resilience4j (circuit breaker, retry, rate limiter) |
| Mapping | MapStruct |
| Java | 21 |

## Running locally

```bash
# 1 – Start infrastructure
docker-compose up -d

# 2 – Run the service
./mvnw spring-boot:run
```

Service starts on **http://localhost:8082**  
Eureka dashboard: **http://localhost:8761**

## API reference

### Public (no auth)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/users/public/{id}` | Public profile (no email/phone/coords) |
| GET | `/api/v1/users/search?q=&page=&size=` | Keyword search |

### Authenticated (Bearer JWT)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/users` | Create profile (called by Auth Service) |
| GET | `/api/v1/users/me` | Own full profile |
| GET | `/api/v1/users/{id}` | Full profile by ID |
| GET | `/api/v1/users/by-username/{username}` | Full profile by username |
| PUT | `/api/v1/users/{id}` | Update profile (owner or admin) |
| PATCH | `/api/v1/users/{id}/avatar` | Update avatar URL |
| DELETE | `/api/v1/users/{id}` | Soft-delete (owner or admin) |

### Admin only (`ROLE_ADMIN`)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/users?page=&size=` | List all active users |
| PATCH | `/api/v1/users/{id}/role?role=ADMIN` | Change role |
| PATCH | `/api/v1/users/{id}/deactivate` | Deactivate account |
| PATCH | `/api/v1/users/{id}/reactivate` | Reactivate account |

### Internal (Feign, gateway-blocked externally)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/internal/users/{id}/public` | Public profile for cross-service use |
| GET | `/api/v1/internal/users/by-auth/{authUserId}` | Existence check |
| POST | `/api/v1/internal/users/{id}/increment-reviews` | Called by Rating Service |
| POST | `/api/v1/internal/users/{id}/increment-helpful-votes` | Called by Rating Service |

## Kafka topics published

| Topic | Payload | Consumer(s) |
|---|---|---|
| `user.created` | `UserCreatedEvent` | Rating Service, Notification Service |
| `user.updated` | `UserUpdatedEvent` | Place Service (denormalized review author name) |
| `user.deleted` | `UserDeletedEvent` | Rating Service (anonymise reviews), Auth Service |

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `DB_USER` | `safeher` | PostgreSQL username |
| `DB_PASSWORD` | `safeher` | PostgreSQL password |
| `JWT_SECRET` | (dev value) | Must match Auth Service |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka brokers |
| `EUREKA_URI` | `http://localhost:8761/eureka` | Eureka server |
