<div align="center">

# SafeHer

**Crowdsourced safety ratings and real-time reviews of places — built for women.**

SafeHer lets users rate the safety of public places, read reviews from others, and get
AI-powered safety insights — helping people make informed decisions about where they go
and when.

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.1-green?style=flat-square&logo=spring)](https://spring.io/projects/spring-cloud)
[![React](https://img.shields.io/badge/React-18-blue?style=flat-square&logo=react)](https://react.dev)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [Tech Stack](#tech-stack)
- [Kafka Event Bus](#kafka-event-bus)
- [AI Agents](#ai-agents)
- [API Reference](#api-reference)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Project Structure](#project-structure)
- [Contributing](#contributing)

---

## Overview

SafeHer is a microservices-based platform where users can:

- **Browse** safety-rated places on an interactive map
- **Search** by keyword or location radius
- **Post reviews** with a 1–5 safety score, structured tags, and optional body text
- **Post anonymously** — identity stored for deduplication only, never shown publicly
- **Ask the AI** safety questions about specific places in natural language
- **Trust the data** — AI agents automatically moderate fake reviews and flag coordinated attacks

Unauthenticated users can read everything. Writing reviews, adding places, and using the chatbot require an account.

---

## Architecture

```
                         ┌─────────────────────────────────┐
                         │          React Frontend          │
                         │   (Vite · Tailwind · Leaflet)    │
                         └──────────────┬──────────────────┘
                                        │ HTTPS
                         ┌──────────────▼──────────────────┐
                         │         API Gateway :8080        │
                         │  JWT validation · Rate limiting  │
                         │  Circuit breaker · CORS · Routing│
                         └──┬──────┬──────┬──────┬──────┬──┘
                            │      │      │      │      │
               ┌────────────▼─┐ ┌──▼───┐ ┌▼───┐ ┌▼────┐ ┌▼──────┐
               │ Auth :8081   │ │ User │ │Place│ │Rating│ │  AI  │
               │ JWT · BCrypt │ │:8082 │ │:8083│ │:8084 │ │:8085 │
               └──────────────┘ └──────┘ └──┬──┘ └──┬──┘ └───┬───┘
                                             │       │         │
                         ┌───────────────────▼───────▼─────────▼───┐
                         │              Kafka Event Bus              │
                         │  rating.created · place.created · ...    │
                         └───────────────────────────────────────────┘
       ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
       │PostgreSQL│  │PostgreSQL│  │PostGIS   │  │ MongoDB  │  │  Ollama  │
       │  (auth)  │  │ (users)  │  │ (places) │  │(ratings) │  │  (LLM)   │
       └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘
                         ┌────────────────────────────────────┐
                         │          Elasticsearch 8           │
                         │   places index · ratings index     │
                         └────────────────────────────────────┘
                         ┌──────────┐       ┌──────────────────┐
                         │  Redis   │       │  Eureka :8761    │
                         │ (tokens  │       │  Config  :8888   │
                         │  + rate  │       │  (service disco  │
                         │  limits) │       │  + central conf) │
                         └──────────┘       └──────────────────┘
```

---

## Services

| Service | Port | Description | Database |
|---|---|---|---|
| **API Gateway** | 8080 | Single entry point — JWT validation, routing, rate limiting, circuit breaking | Redis |
| **Auth Service** | 8081 | Registration, login, JWT issuance, refresh token rotation, account lockout | PostgreSQL |
| **User Service** | 8082 | User profiles, avatar, location, roles | PostgreSQL |
| **Place Service** | 8083 | Place CRUD, PostGIS geo-search, keyword search, safety score materialisation | PostgreSQL + PostGIS |
| **Rating Service** | 8084 | Reviews (1–5), anonymous posting, helpful votes, score aggregation | MongoDB |
| **AI Service** | 8085 | 6 AI agents — moderation, summarisation, chatbot, anomaly detection | Redis |
| **Eureka Server** | 8761 | Service discovery | — |
| **Config Server** | 8888 | Centralised configuration (native filesystem backend) | — |

---

## Tech Stack

### Backend

| Concern | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.4 |
| Service mesh | Spring Cloud 2023.0.1 |
| Service discovery | Netflix Eureka |
| Centralised config | Spring Cloud Config Server |
| API Gateway | Spring Cloud Gateway (WebFlux) |
| Auth | Spring Security · JWT (JJWT 0.12) · BCrypt |
| Inter-service calls | OpenFeign |
| Fault tolerance | Resilience4j — Circuit Breaker, Retry, Rate Limiter |
| Async messaging | Apache Kafka 7.6 |
| ORM / migrations | Spring Data JPA · Hibernate Spatial · Flyway |
| Object mapping | MapStruct |
| Primary databases | PostgreSQL 16 (auth, users, places) · MongoDB 7 (ratings) |
| Spatial queries | PostGIS 3.4 — `ST_DWithin`, `ST_MakePoint` |
| Search | Elasticsearch 8.13 — multi-field, fuzzy, geo-point |
| Token blacklist + rate limiting | Redis 7 |
| AI / LLM | Ollama (local) — default model `llama3.2:3b` |
| Utilities | Lombok · Jackson |

### Frontend

| Concern | Technology |
|---|---|
| Framework | React 18 + TypeScript |
| Build tool | Vite |
| Styling | Tailwind CSS |
| Routing | React Router v6 |
| Server state | TanStack Query (React Query) |
| Global state | Zustand |
| HTTP client | Axios (with JWT interceptor + auto-refresh) |
| Map | Leaflet + React-Leaflet + OpenStreetMap |
| Forms | React Hook Form + Zod |

---

## Kafka Event Bus

All inter-service communication for non-request-path events goes through Kafka.

### Topics

| Topic | Producer | Consumers | Purpose |
|---|---|---|---|
| `auth.user.registered` | Auth | User | Creates user profile after signup |
| `auth.user.deleted` | Auth | User, Rating | Cascade deactivation |
| `auth.password.changed` | Auth | — | Notification hook |
| `user.created` | User | — | Profile created confirmation |
| `user.updated` | User | — | Profile updated |
| `user.deleted` | User | — | Soft delete broadcast |
| `place.created` | Place | AI | Triggers description generation |
| `place.updated` | Place | — | Place edited |
| `place.deleted` | Place | — | Soft delete broadcast |
| `place.score.updated` | Place | — | Safety score recalculated |
| `rating.created` | Rating | Place, AI | Updates materialised score · triggers moderation + summarisation + anomaly check |
| `rating.updated` | Rating | Place | Score change → recalculate |
| `rating.deleted` | Rating | Place | Removed rating → recalculate |
| `ai.review.flagged` | AI | Admin dashboard | Review flagged for abuse / fake / coordinated |
| `ai.place.summary.updated` | AI | Place, Frontend | New AI safety summary available |

---

## AI Agents

The AI Service runs 6 agents powered by **Ollama** (free, local, no API key):

| # | Agent | Trigger | What it does |
|---|---|---|---|
| 1 | **Review Moderation** | `rating.created` Kafka | Classifies reviews as CLEAN / SPAM / ABUSE / FAKE / COORDINATED. Auto-suppresses above 90% confidence, flags above 65% |
| 2 | **Safety Narrative Summarizer** | Every 10th new review | Reads up to 50 reviews and generates a 2–3 sentence plain-language safety summary, stored on the place |
| 3 | **Smart Review Assistant** | REST call from frontend | Returns a one-sentence writing prompt as the user fills in the review form, based on their score and selected tags |
| 4 | **Rating Anomaly Detector** | `rating.created` Kafka | Two-stage: Redis velocity window (15+ ratings/hr triggers investigation) → LLM analyses the batch for coordinated manipulation |
| 5 | **Safety Chatbot** | REST (multi-turn) | Conversational RAG — fetches live place + review data and answers natural language safety questions |
| 6 | **Place Description Generator** | `place.created` Kafka | Auto-drafts a factual 2-sentence description for new places that have none |

**Default model:** `llama3.2:3b` (2 GB RAM). Swap to `llama3.1:8b` or `mistral:7b` for better reasoning on larger hardware. `gemma2:9b` recommended for Hindi/Bengali multilingual support.

---

## API Reference

All requests go through the API Gateway at `http://localhost:8080`.

### Auth — `/api/v1/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/register` | No | Create account |
| POST | `/login` | No | Login with email or username |
| POST | `/refresh` | No | Rotate refresh token |
| POST | `/logout` | Yes | Logout current device |
| POST | `/logout-all` | Yes | Logout all devices |
| POST | `/change-password` | Yes | Change password |
| POST | `/forgot-password` | No | Request reset link |
| POST | `/reset-password` | No | Reset with token |
| DELETE | `/account` | Yes | Delete account |
| GET | `/validate` | Yes | Validate token |

### Users — `/api/v1/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/public/{id}` | No | Public profile |
| GET | `/search?q=` | No | Keyword search |
| GET | `/me` | Yes | Own full profile |
| GET | `/{id}` | Yes | Full profile by ID |
| PUT | `/{id}` | Yes | Update profile (owner or admin) |
| PATCH | `/{id}/avatar` | Yes | Update avatar URL |
| DELETE | `/{id}` | Yes | Soft-delete (owner or admin) |
| GET | `/` | Admin | List all users |
| PATCH | `/{id}/role` | Admin | Change role |
| PATCH | `/{id}/deactivate` | Admin | Deactivate account |

### Places — `/api/v1/places`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/{id}` | No | Full place detail + safety score |
| GET | `/` | No | Paginated list |
| GET | `/category/{category}` | No | By category |
| GET | `/top-rated` | No | Top 10 by safety score |
| GET | `/geo-search?latitude=&longitude=&radiusMeters=&category=` | No | PostGIS radius search |
| GET | `/search?query=&city=&category=` | No | Elasticsearch keyword search |
| POST | `/` | Yes | Create place |
| PUT | `/{id}` | Yes | Update place (owner or admin) |
| DELETE | `/{id}` | Yes | Soft-delete |
| GET | `/my-places` | Yes | My created places |
| POST | `/{id}/report` | Yes | Report a place |
| PATCH | `/{id}/verify` | Admin | Verify a place |
| POST | `/seed/google` | Admin | Seed from Google Places API |

### Ratings — `/api/v1/ratings`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/{id}` | No | Single review |
| GET | `/place/{placeId}?sortBy=NEWEST` | No | Reviews for a place |
| GET | `/place/{placeId}/summary` | No | Avg score + distribution |
| GET | `/user/{userId}` | No | Reviews by a user |
| GET | `/search?query=&placeId=&minScore=&maxScore=` | No | Keyword search |
| POST | `/` | Yes | Post review |
| PUT | `/{id}` | Yes | Edit own review |
| DELETE | `/{id}` | Yes | Delete own review (admin deletes any) |
| GET | `/my-reviews` | Yes | My reviews |
| POST | `/{id}/helpful` | Yes | Toggle helpful vote |
| POST | `/{id}/report` | Yes | Flag review |

### AI — `/api/v1/ai`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/chat` | Yes | Safety chatbot (multi-turn) |
| POST | `/review-assist` | Yes | Writing prompt while typing |
| POST | `/places/{id}/summary` | No | Generate safety summary |
| POST | `/moderate` | Admin | Manual moderation trigger |
| POST | `/places/{id}/description` | Admin | Generate place description |

---

## Getting Started

### Prerequisites

- Docker Desktop (with at least 6 GB RAM allocated)
- Git

### 1. Clone the repository

```bash
git clone https://github.com/your-org/safeher.git
cd safeher
```

### 2. Build all service images

```bash
docker-compose build
```

### 3. Start infrastructure first

```bash
# Start databases, Kafka, Redis, Elasticsearch
docker-compose up -d postgres-auth postgres-users postgres-places mongodb elasticsearch redis zookeeper kafka
```

### 4. Start Spring Cloud infrastructure

```bash
# Eureka must be healthy before Config Server
docker-compose up -d eureka-server
# Wait ~20s for Eureka to be ready, then:
docker-compose up -d config-server
```

### 5. Start microservices

```bash
docker-compose up -d auth-service user-service place-service rating-service
```

### 6. Start the gateway

```bash
docker-compose up -d api-gateway
```

### 7. Set up Ollama for AI features

```bash
docker-compose up -d ollama
# Pull the default model (one-time download, ~2 GB)
docker exec -it sp-ollama ollama pull llama3.2:3b
docker-compose up -d ai-service
```

### 8. Start the frontend

```bash
cd safeher-ui
npm install
npm run dev
```

Frontend: **http://localhost:3000**
API Gateway: **http://localhost:8080**
Eureka Dashboard: **http://localhost:8761** (admin / admin)

> **Tip:** Run `docker-compose up -d` to start everything at once once you've done the first-time Ollama model pull.

---

## Environment Variables

All services read from the Config Server. The key shared variables are:

| Variable | Default (dev) | Description |
|---|---|---|
| `JWT_SECRET` | `404E635266...` | Shared across all services — **change in production** |
| `DB_USER` | `safeher` | PostgreSQL username |
| `DB_PASSWORD` | `safeher` | PostgreSQL password |
| `KAFKA_BOOTSTRAP` | `kafka:9092` | Kafka broker address |
| `REDIS_HOST` | `redis` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `EUREKA_URI` | `http://admin:admin@eureka-server:8761/eureka` | Eureka endpoint |
| `OLLAMA_URL` | `http://ollama:11434` | Ollama base URL |
| `OLLAMA_MODEL` | `llama3.2:3b` | LLM model to use |
| `GOOGLE_PLACES_API_KEY` | _(empty)_ | Optional — only needed for place seeding |

For production, override these via your orchestrator (Kubernetes secrets, AWS Parameter Store, etc).

---

## Project Structure

```
safeher/
├── docker-compose.yml          # Full platform — all infra + services
│
├── eureka-server/              # Service discovery (port 8761)
├── config-server/              # Centralised config (port 8888)
│   └── src/main/resources/
│       └── configs/            # Per-service config files
│           ├── application.yml     # Shared defaults (inherited by all)
│           ├── auth-service.yml
│           ├── user-service.yml
│           ├── place-service.yml
│           ├── rating-service.yml
│           ├── ai-service.yml
│           └── api-gateway.yml
│
├── api-gateway/                # Gateway (port 8080)
├── auth-service/               # Auth (port 8081) — PostgreSQL
├── user-service/               # Users (port 8082) — PostgreSQL
├── place-service/              # Places (port 8083) — PostgreSQL + PostGIS + Elasticsearch
├── rating-service/             # Ratings (port 8084) — MongoDB + Elasticsearch
├── ai-service/                 # AI agents (port 8085) — Ollama + Redis
│
└── safeher-ui/                 # React frontend (port 3000)
    └── src/
        ├── api/                # Axios API clients
        ├── store/              # Zustand auth store
        ├── hooks/              # TanStack Query hooks
        ├── pages/              # Route-level components
        ├── components/         # Shared UI components
        └── types/              # TypeScript types (mirrors backend DTOs)
```

---

## Key Design Decisions

**Materialised safety score** — `safety_score` is stored directly on the `places` row and updated asynchronously via `rating.created` Kafka events. The hot read path (GET /places/{id}) never calls Rating Service synchronously.

**Dual write on registration** — Auth Service calls User Service via Feign immediately after registration, and also publishes `auth.user.registered` to Kafka. If the Feign call fails, Kafka ensures the user profile is eventually created.

**Refresh token rotation** — every `/auth/refresh` call revokes the old token and issues a new one. A stolen refresh token becomes invalid after the real user uses it once.

**Anonymous posting** — `userId` is always stored internally for deduplication and moderation. It is never returned in any public API response when `anonymous: true`.

**PostGIS over Elasticsearch for geo-search** — `ST_DWithin` on a geography column gives true metre-based radius search with a GIST index. Elasticsearch handles keyword/text search separately. The two are not merged.

**Ollama over cloud LLMs** — all AI inference runs locally via Ollama. No API keys, no per-call cost, no data sent externally. Models run on the same server as the rest of the stack.

---

## Contributing

1. Fork the repo
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add your feature'`)
4. Push and open a pull request

Please follow the existing package structure and add unit tests for any new service-layer logic.

---

<div align="center">
Built with care for safer communities.
</div>