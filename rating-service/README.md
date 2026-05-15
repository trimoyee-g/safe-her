# Rating Service — SafeHer Platform

Manages crowd-sourced safety reviews and ratings for places. Port **8084**.

## Responsibilities

| Concern | Implementation |
|---|---|
| Reviews | MongoDB documents – rich, flexible schema (title, body, photos, tags) |
| Scores | 1–5 per review; aggregated avg + distribution computed via MongoDB aggregation pipeline |
| One review per user/place | Compound unique index enforces this; updates allowed |
| Anonymous posting | `userId` stored for moderation; masked in all public responses |
| Keyword search | Elasticsearch multi-field (title, body, tags) with fuzzy matching and score/place filters |
| Safety score propagation | `rating.created/updated/deleted` Kafka events → Place Service updates materialised score |
| Helpful votes | Toggle on/off; cannot vote on own review |
| Reporting | Flag abusive reviews; admins can delete any review |
| Author info | Denormalised at write time (displayName, avatarUrl) to avoid User Service at read time |

## API Endpoints

### Public (no auth)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/ratings/{id}` | Single review |
| GET | `/api/v1/ratings/place/{placeId}?sortBy=NEWEST&page=0&size=20` | Reviews for a place |
| GET | `/api/v1/ratings/place/{placeId}/summary` | Avg score + distribution |
| GET | `/api/v1/ratings/user/{userId}` | Reviews by a user |
| GET | `/api/v1/ratings/search?query=&placeId=&minScore=&maxScore=` | Keyword search |

### Authenticated (Bearer JWT)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/ratings` | Create review |
| PUT | `/api/v1/ratings/{id}` | Edit own review (admin edits any) |
| DELETE | `/api/v1/ratings/{id}` | Delete own review (admin deletes any) |
| GET | `/api/v1/ratings/my-reviews` | Own reviews |
| POST | `/api/v1/ratings/{id}/helpful` | Toggle helpful vote |
| POST | `/api/v1/ratings/{id}/report` | Flag review |

### Internal (Feign, gateway-blocked)

| Method | Path | Consumer |
|---|---|---|
| GET | `/api/v1/internal/ratings/places/{placeId}/score` | Place Service |
| GET | `/api/v1/internal/ratings/places/{placeId}/summary` | Place Service |

## Kafka Topics Published

| Topic | Trigger | Consumer |
|---|---|---|
| `rating.created` | New review posted | Place Service → updates safety_score |
| `rating.updated` | Score changed on edit | Place Service → updates safety_score |
| `rating.deleted` | Review soft-deleted | Place Service → updates safety_score |

## Sort options for `/place/{placeId}`
`NEWEST` · `OLDEST` · `HIGHEST_SCORE` · `LOWEST_SCORE` · `MOST_HELPFUL`

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `MONGO_URI` | `mongodb://safeher:safeher@localhost:27017/...` | MongoDB URI |
| `ES_URIS` | `http://localhost:9200` | Elasticsearch |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka brokers |
| `JWT_SECRET` | (dev value) | Shared with all services |
| `EUREKA_URI` | `http://admin:admin@localhost:8761/eureka` | Eureka server |
