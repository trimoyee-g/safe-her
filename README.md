<div align="center">

# SafeHer

**Ask-first AI safety assistant for women — grounded in crowdsourced reviews, backed by
cited web research, and honest about what it doesn't know.**

SafeHer lets you ask a question — *"is this café well lit at night?"*, *"which areas feel
unsafe in this city?"* — and get an answer synthesized by AI agents from real community
reviews and the open web, with every claim cited and a clear confidence level. Behind the
chat, it's still a full platform: rate places, read structured reviews, and browse a map
when you'd rather explore than ask.

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.1-green?style=flat-square&logo=spring)](https://spring.io/projects/spring-cloud)
[![Python](https://img.shields.io/badge/Python-3.12-blue?style=flat-square&logo=python)](https://python.org)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115-009688?style=flat-square&logo=fastapi)](https://fastapi.tiangolo.com)
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

- **Ask SafeGuide** anything about a place or an area, in plain language, and get a cited,
  confidence-scored answer — this is the home screen for signed-in users, not a buried feature
- **Solve cold start** — a new city or a place with zero reviews still gets a useful answer,
  because the AI falls back to live web research instead of an empty screen
- **Browse** safety-rated places on an interactive map (dark-themed, still one tap away)
- **Search** by keyword or location radius
- **Post reviews** with a 1–5 safety score, structured tags, and optional body text
- **Post anonymously** — identity stored for deduplication only, never shown publicly
- **Trust the data** — AI agents automatically moderate fake reviews and flag coordinated attacks,
  and every AI answer shows its sources rather than asserting a flat verdict

Unauthenticated users can read everything and see the marketing/landing experience. Asking
SafeGuide, writing reviews, and adding places require an account.

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
       ┌──────────┐       ┌──────────┐       ┌──────────────────┐
       │pgvector  │       │  Redis   │       │  Eureka :8761    │
       │(review   │       │ (tokens  │       │  Config  :8888   │
       │embeddings│       │  + rate  │       │  (service disco  │
       │ :5435)   │       │  limits) │       │  + central conf) │
       └──────────┘       └──────────┘       └──────────────────┘
```

> The AI Service also reaches outside this box: the **Web Research Agent** calls out to the
> public web (DuckDuckGo, no API key) to fill in gaps where our own review data is thin —
> see [AI Agents](#ai-agents) for how it's combined with first-party data.

---

## Services

| Service | Port | Description | Database |
|---|---|---|---|
| **API Gateway** | 8080 | Single entry point — JWT validation, routing, rate limiting, circuit breaking | Redis |
| **Auth Service** | 8081 | Registration, login, JWT issuance, refresh token rotation, account lockout | PostgreSQL |
| **User Service** | 8082 | User profiles, avatar, location, roles | PostgreSQL |
| **Place Service** | 8083 | Place CRUD, PostGIS geo-search, keyword search, safety score materialisation | PostgreSQL + PostGIS |
| **Rating Service** | 8084 | Reviews (1–5), anonymous posting, helpful votes, score aggregation | MongoDB |
| **AI Service** | 8085 | 6 agents — moderation, summarisation, review assistant, anomaly detection, description generator, and the SafeGuide chatbot (itself a 3-agent retrieval + web research + synthesis pipeline). Python/FastAPI + LangChain + LangGraph | Redis · pgvector |
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

### AI Service (Python)

| Concern | Technology |
|---|---|
| Language | Python 3.12 |
| Framework | FastAPI 0.115 + Uvicorn |
| AI orchestration | LangChain 0.3 · LangGraph 0.2 · LangChain-Ollama |
| Vector store | pgvector (PostgreSQL 16) via langchain-postgres |
| Embeddings | Ollama `nomic-embed-text` (local, no API key) |
| Kafka client | aiokafka |
| HTTP client | httpx (async) |
| Service discovery | py-eureka-client (registers as `AI-SERVICE`) |
| Cache | redis[asyncio] |
| Web search (cold-start bridge) | `ddgs` (DuckDuckGo, no API key) |

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
| Map | Leaflet + React-Leaflet + OpenStreetMap (dark tiles via CSS filter — no separate tile provider) |
| Forms | React Hook Form + Zod |
| Theming | Dark theme, app-wide (no light/dark toggle) |

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
| `rating.created` | Rating | Place, AI | Updates materialised score · triggers moderation + summarisation + anomaly check + vector ingestion |
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
| 5 | **SafeGuide Chatbot** | REST (multi-turn, streaming or not) | Multi-agent agentic RAG — see below |
| 6 | **Place Description Generator** | `place.created` Kafka | Auto-drafts a factual 2-sentence description for new places that have none |

**Default model:** `llama3.2:3b` (2 GB RAM). Swap to `llama3.1:8b` or `mistral:7b` for better reasoning on larger hardware. `gemma2:9b` recommended for Hindi/Bengali multilingual support.

### SafeGuide: the chatbot is three agents, not one

Agent 5 isn't a single LLM call — it's a small pipeline (`ai-service/app/agents/chatbot.py` +
`web_research.py`) built specifically to solve the cold-start problem: a brand-new city or a
place with zero reviews should still get a useful, honest answer instead of an empty screen.

1. **Retrieval agent** — pulls whatever first-party data we already have: pgvector similarity
   search resolves relevant places from the question itself, then live review/place data is
   fetched from Rating Service and Place Service.
2. **Web research agent** (`web_research.py`) — searches the open web (DuckDuckGo via `ddgs`,
   no API key) for the place and surrounding area — news, forums, other review sites — and
   returns raw, attributed snippets. It never asserts a verdict itself.
3. **Synthesis agent** — a LangGraph state machine (`generate` → `grade` → retry up to twice)
   combines both sources into one answer, is instructed to attribute every web-sourced claim
   rather than state it as fact, and a lightweight `phi3:mini` grader checks the response stays
   grounded in the provided context before it's returned.

Confidence is **computed, not just asked of the LLM** — `_compute_confidence()` counts how many
first-party reviews and web results actually back the answer and returns `high` / `medium` /
`low` / `no_data`. The frontend renders this as a color-coded badge (green/amber/red/gray) above
every answer, and every claim is backed by a source chip — a teal check-mark chip linking back
to the review, or a gray external-link chip out to the web result. As the review corpus for a
place grows past `chatbot_first_party_confidence_floor` (default 3), answers shift from
web-sourced to first-party automatically — the web research agent is a permanent supplement to
the cold-start bridge, not a permanent crutch; first-party review data is the long-term moat.

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
| POST | `/chat` | Yes | SafeGuide chatbot (multi-turn). Response includes `message`, `sources[]` (`kind: "review" \| "web"`), and `confidence` (`high` / `medium` / `low` / `no_data`) |
| POST | `/chat/stream` | Yes | Same, streamed as SSE tokens; final event carries `sources` + `confidence` |
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
# Start databases, Kafka, Redis, Elasticsearch, pgvector
docker-compose up -d postgres-auth postgres-users postgres-places postgres-vector mongodb elasticsearch redis zookeeper kafka
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
# Inference model (~2 GB, one-time)
docker exec -it sp-ollama ollama pull llama3.2:3b
# Embedding model for vector search (~274 MB, one-time)
docker exec -it sp-ollama ollama pull nomic-embed-text
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
| `OLLAMA_MODEL` | `llama3.2:3b` | LLM model — set in ai-service `config.py` |
| `VECTOR_DB_URL` | `postgresql+psycopg://safeher:safeher@postgres-vector:5432/safeher_vectors` | pgvector connection (ai-service only) |
| `GOOGLE_PLACES_API_KEY` | _(empty)_ | Optional — only needed for place seeding |
| `WEB_SEARCH_ENABLED` | `true` | Toggle the SafeGuide web research agent (`ai-service` `config.py`) |
| `WEB_SEARCH_MAX_RESULTS` | `5` | Max DuckDuckGo results fetched per question |
| `WEB_SEARCH_TIMEOUT_S` | `8.0` | Timeout before the web research agent gives up and falls back to first-party data only |
| `CHATBOT_FIRST_PARTY_CONFIDENCE_FLOOR` | `3` | First-party review count at which an answer is graded `high` confidence instead of leaning on web results |

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
├── ai-service/                 # AI agents (port 8085) — Python/FastAPI + LangChain + Ollama + Redis + pgvector
│   └── app/
│       ├── agents/             # 6 LangChain / LangGraph agents
│       │   ├── chatbot.py          # SafeGuide synthesis agent — LangGraph generate/grade loop
│       │   └── web_research.py     # Web research agent — DuckDuckGo (ddgs), cold-start bridge
│       ├── clients/            # Async HTTP clients (place-service, rating-service)
│       ├── kafka/              # aiokafka consumer + producer
│       ├── middleware/         # JWT auth middleware
│       ├── models/             # Pydantic request / response / event models
│       ├── routers/            # FastAPI route handlers
│       └── vector_store.py     # pgvector setup, batch ingestion (size 50), semantic search
│
└── safeher-ui/                 # React frontend (port 3000) — dark theme, chat-first home
    └── src/
        ├── api/                # Axios API clients
        ├── store/              # Zustand auth store
        ├── hooks/              # TanStack Query hooks
        ├── pages/              # Route-level components (ChatbotPage is the authenticated root route)
        ├── components/
        │   ├── chat/                # ConfidenceBadge, SourceChips — the "show your work" UI
        │   ├── ui/                  # Shared design-system primitives (dark palette)
        │   └── ...                  # place, rating, map, layout, auth components
        └── types/              # TypeScript types (mirrors backend DTOs, incl. ChatSource/ChatConfidence)
```

---

## Key Design Decisions

**Materialised safety score** — `safety_score` is stored directly on the `places` row and updated asynchronously via `rating.created` Kafka events. The hot read path (GET /places/{id}) never calls Rating Service synchronously.

**Dual write on registration** — Auth Service calls User Service via Feign immediately after registration, and also publishes `auth.user.registered` to Kafka. If the Feign call fails, Kafka ensures the user profile is eventually created.

**Refresh token rotation** — every `/auth/refresh` call revokes the old token and issues a new one. A stolen refresh token becomes invalid after the real user uses it once.

**Anonymous posting** — `userId` is always stored internally for deduplication and moderation. It is never returned in any public API response when `anonymous: true`.

**PostGIS over Elasticsearch for geo-search** — `ST_DWithin` on a geography column gives true metre-based radius search with a GIST index. Elasticsearch handles keyword/text search separately. The two are not merged.

**Ollama over cloud LLMs** — all AI inference runs locally via Ollama. No API keys, no per-call cost, no data sent externally. Models run on the same server as the rest of the stack.

**Python for the AI service** — the AI service is written in Python (FastAPI + LangChain + LangGraph) rather than Java to take full advantage of the LangChain/LangGraph ecosystem. It registers with Eureka as `AI-SERVICE` so the Spring Cloud Gateway can route to it exactly like any other service.

**Dual-mode chatbot retrieval** — the Safety Chatbot uses two retrieval strategies depending on the request. When the frontend passes specific `placeIds`, it fetches live reviews directly from rating-service (vectorless, always fresh). For open discovery queries with no place context, it embeds the user's message with `nomic-embed-text` and does a semantic similarity search across all ingested reviews in pgvector, returning the top-5 matching places to build context from.

**Batched vector ingestion** — review embeddings are written to pgvector in batches of 50, not one at a time. Each `rating.created` Kafka event enqueues the review; when the buffer fills, all 50 fetches (rating + place metadata) run concurrently in one `asyncio.gather`, then a single `aadd_documents` call embeds and inserts the batch. A 20-minute periodic flush drains any partial batch so reviews never stall indefinitely.

**Web search as the cold-start bridge, not the product** — a crowdsourced safety platform is
useless in a brand-new city on day one: no reviews means no answers means no reason to come
back. Rather than wait for organic review density (the way most "crowdsourced" safety apps
actually work in practice — SafetiPin and Safecity both rely on paid field audits or in-person
outreach, not spontaneous app usage), SafeGuide gives every question a real answer immediately
by falling back to live web research. This is explicitly a bridge: `chatbot_first_party_confidence_floor`
shifts weight to first-party review data as it accumulates, because a live web-search wrapper
alone isn't a defensible long-term product — the review corpus is.

**Confidence is computed, never just claimed** — `_compute_confidence()` in `chatbot.py` counts
first-party reviews and web sources and derives `high` / `medium` / `low` / `no_data`
programmatically, rather than trusting the LLM to self-report certainty. The synthesis prompt is
also instructed to attribute every web-sourced claim ("according to a recent article...") instead
of stating it as settled fact. On a safety-critical topic, a wrong answer that sounds confident is
worse than no answer — the UI never renders a flat verdict without showing its work.

**Dark theme, chat-first home, no light mode** — SafeGuide is the root route for authenticated
users, not the map or search. The rest of the app (search, place detail, profile, reviews, auth)
was rebuilt to match: a single dark palette app-wide, including a CSS-filter trick
(`invert + hue-rotate + brightness` on `.leaflet-tile-pane`) to theme the OpenStreetMap tiles
without switching tile providers.

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