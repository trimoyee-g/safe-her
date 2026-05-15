# Place Service — SafeHer Platform

Manages places (venues, streets, transit stops, etc.) with geo-search, keyword search via Elasticsearch,
Google Places seeding, aggregated safety scores (materialised via Kafka), and full CRUD with RBAC. Port **8083**.

## Tech Stack

| Concern | Choice                                                     |
|---|------------------------------------------------------------|
| Framework | Spring Boot 3.2 + Spring Cloud 2023                        |
| Primary DB | PostgreSQL 16 + **PostGIS** (spatial queries)              |
| Search | **Elasticsearch 8** (multi-field keyword, fuzzy, filtered) |
| Messaging | Apache Kafka (producer + consumer)                         |
| Migrations | Flyway                                                     |
| Discovery | Netflix Eureka                                             |
| Fault tolerance | Resilience4j (CB, retry, rate limiter)                     |
| Mapping | MapStruct                                                  |
| Java | 17                                                         |

## Running locally

```bash
# 1 – Start infra (PostGIS, Elasticsearch, Kafka, Eureka)
docker-compose up -d

# 2 – Start service
./mvnw spring-boot:run
```

Service: **http://localhost:8083**

## API Reference

### Public (no auth)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/places` | Paginated list of all active places |
| GET | `/api/v1/places/{id}` | Full place detail + aggregated safety score |
| GET | `/api/v1/places/category/{category}` | Places by category, sorted by safety score |
| GET | `/api/v1/places/top-rated` | Top 10 highest-rated places |
| GET | `/api/v1/places/geo-search?latitude=&longitude=&radiusMeters=&category=&page=&size=` | PostGIS radius search, results sorted by distance |
| GET | `/api/v1/places/search?query=&city=&category=&country=&page=&size=` | Elasticsearch multi-field keyword search |

### Authenticated (Bearer JWT)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/places` | Create a new place |
| PUT | `/api/v1/places/{id}` | Update place (owner or admin) |
| DELETE | `/api/v1/places/{id}` | Soft-delete place (owner or admin) |
| GET | `/api/v1/places/my-places` | Places created by the calling user |
| POST | `/api/v1/places/{id}/report` | Flag a place for moderation |

### Admin only

| Method | Path | Description |
|---|---|---|
| PATCH | `/api/v1/places/{id}/verify` | Mark a place as verified |
| DELETE | `/api/v1/places/{id}/admin` | Force-delete any place |
| POST | `/api/v1/places/seed/google?lat=&lng=&radiusMeters=&type=` | Seed from Google Places API |

### Internal (Feign, gateway-blocked externally)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/internal/places/{id}` | Full place for cross-service use |
| PATCH | `/api/v1/internal/places/{id}/score?score=&totalRatings=` | Direct score update from Rating Service |

## Kafka

### Published topics

| Topic | Trigger |
|---|---|
| `place.created` | New place added |
| `place.updated` | Place edited |
| `place.deleted` | Place soft-deleted |
| `place.score.updated` | Safety score recalculated |

### Consumed topics

| Topic | Action |
|---|---|
| `rating.created` | Updates materialised `safety_score` + `total_ratings` on the Place row, re-indexes ES document |

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_USER` | `safeher` | PostgreSQL username |
| `DB_PASSWORD` | `safeher` | PostgreSQL password |
| `JWT_SECRET` | (dev value) | Must match Auth Service |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka brokers |
| `ES_URIS` | `http://localhost:9200` | Elasticsearch URI |
| `EUREKA_URI` | `http://localhost:8761/eureka` | Eureka server |
| `GOOGLE_PLACES_API_KEY` | _(empty)_ | Required only for seeding |

## Key Design Decisions

**PostGIS for geo-search** — `ST_DWithin` on a geography column gives true metres-based radius search with the spatial GIST index, far faster than bounding-box approximations.

**Materialised safety score** — `safety_score` and `total_ratings` are stored on the `places` row and updated asynchronously via the `rating.created` Kafka event. The hot read path (GET /places/{id}) never calls Rating Service synchronously.

**Async ES indexing** — All writes to Elasticsearch happen in a separate thread pool so the HTTP response is not blocked by the ES round-trip.

**Dual search strategy** — PostGIS for spatial proximity, Elasticsearch for keyword/fuzzy text search. The two are not merged into one query; use geo-search for "near me" and keyword search for "find a coffee shop".
