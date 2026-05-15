# API Gateway — SafeHer Platform

Single entry point for all client traffic. Runs on port **8080**.

## Responsibilities

| Concern | Implementation |
|---|---|
| JWT validation | `JwtAuthenticationFilter` — validates signature + expiry before any downstream call |
| Identity forwarding | Injects `X-Auth-UserId`, `X-Auth-Username`, `X-Auth-Role`, `X-Auth-Email` headers |
| Internal endpoint blocking | `InternalRouteBlockingFilter` + route-level 403 — `/api/v1/internal/**` never reaches internet |
| Rate limiting | Redis `RequestRateLimiter` — per-IP for public routes, per-userId for authenticated routes |
| Circuit breaking | Resilience4j per downstream service with structured JSON fallback |
| Request tracing | `GlobalLoggingFilter` injects `X-Trace-Id` on every request |
| CORS | Global config + `CorsWebFilter` for preflight |

## Port map

| Service | Internal port | Gateway path prefix |
|---|---|---|
| API Gateway | 8080 | — |
| Auth Service | 8081 | `/api/v1/auth/**` |
| User Service | 8082 | `/api/v1/users/**` |
| Place Service | 8083 | `/api/v1/places/**` |
| Rating Service | 8084 | `/api/v1/ratings/**` |

## Route access matrix

| Route pattern | Auth required | Rate limit key |
|---|---|---|
| `GET /api/v1/places/**` | No | IP |
| `GET /api/v1/users/public/**` | No | IP |
| `GET /api/v1/users/search` | No | IP |
| `GET /api/v1/ratings/**` | No | IP |
| `POST /api/v1/auth/**` | No | IP (strict: 10 req/s) |
| `POST/PUT/DELETE /api/v1/places/**` | **Yes** | userId |
| `POST/PUT/DELETE /api/v1/users/**` | **Yes** | userId |
| `POST/PUT/DELETE /api/v1/ratings/**` | **Yes** | userId |
| `/api/v1/internal/**` | — | **BLOCKED (403)** |

## Running locally

```bash
# Redis must be running (used by rate limiter)
docker run -d -p 6379:6379 redis:7-alpine

./mvnw spring-boot:run
```

Gateway: **http://localhost:8080**

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | (dev value) | Must match Auth Service |
| `REDIS_HOST` | `localhost` | Redis host for rate limiting |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | _(empty)_ | Redis password if auth enabled |
| `EUREKA_URI` | `http://admin:admin@localhost:8761/eureka` | Eureka server |

## Downstream headers set by gateway

Downstream services can trust these headers **only** because the gateway validates the JWT first:

```
X-Auth-UserId    – UUID of the authenticated user (from JWT claim "userId")
X-Auth-Username  – username
X-Auth-Role      – USER | ADMIN | MODERATOR
X-Auth-Email     – email address
X-Trace-Id       – 16-char request trace ID for distributed logging
```
