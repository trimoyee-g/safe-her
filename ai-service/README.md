# AI Service — SafeHer Platform

Hosts all AI agents. Port **8085**.
Built with **Python / FastAPI + LangChain + LangGraph**, powered by **Ollama** — runs fully locally, zero API cost, no keys needed.

Health endpoint: `GET /api/v1/ai/health`

## Model recommendations

| Model | RAM | Best for |
|---|---|---|
| `llama3.2:3b` | 2 GB | Fast classification (agents 1, 4) — default |
| `llama3.1:8b` | 5 GB | Better reasoning, summaries (agents 2, 5) |
| `mistral:7b` | 4 GB | Strong instruction-following, all-rounder |
| `gemma2:9b` | 6 GB | Multilingual — Hindi, Bengali support |
| `phi3:mini` | 2 GB | Fastest on CPU |

## Quick start

```bash
# Pull inference models (one-time)
docker-compose up -d ollama
docker exec -it sp-ollama ollama pull llama3.2:3b
docker exec -it sp-ollama ollama pull nomic-embed-text   # required for vector search

docker-compose up -d ai-service
```

## Agents

| # | Agent | Trigger | Pattern |
|---|---|---|---|
| 1 | Review Moderation | `rating.created` Kafka | Async classifier |
| 2 | Safety Narrative Summarizer | `rating.created` every N reviews | Async RAG |
| 3 | Smart Review Assistant | REST (frontend) | Sync writing prompt |
| 4 | Rating Anomaly Detector | `rating.created` Kafka | Async two-stage |
| 5 | Safety Chatbot | REST multi-turn | Vectorless RAG (place_ids provided) or Vector RAG (discovery query) |
| 6 | Place Description Generator | `place.created` Kafka | Async description |

## Chatbot RAG modes

The chatbot (Agent 5) automatically picks a retrieval strategy based on the request:

| Condition | Strategy | How it works |
|---|---|---|
| `placeIds` provided by frontend | **Vectorless RAG** | Fetches live reviews directly from rating-service; always fresh, no embedding needed |
| `placeIds` empty (discovery query) | **Vector RAG** | Embeds the user's query with `nomic-embed-text`, finds the top-5 most semantically relevant places from pgvector, then runs the same live-fetch flow |

Example discovery queries handled by vector RAG:
- "which metro stations are safe at night?"
- "well-lit parks near the city centre"
- "places to avoid after 10pm"

## Vector store ingestion

Reviews are embedded and stored in pgvector as they arrive from Kafka, using a **batch of 50**:

- Every `rating.created` event is queued via `vector_store.enqueue()`
- When the queue reaches 50, all 50 reviews are fetched concurrently and embedded in one `aadd_documents` call
- A background flush loop runs every **20 minutes** to drain any partial batch, so reviews are never stuck indefinitely
- Only reviews with a text body are embedded; score-only reviews are skipped
- Embedding model: `nomic-embed-text` (via Ollama — local, no API key)

## docker-compose additions

```yaml
  postgres-vector:
    image: pgvector/pgvector:pg16
    container_name: sp-postgres-vector
    ports: ["5435:5432"]
    environment:
      POSTGRES_DB: safeher_vectors
      POSTGRES_USER: safeher
      POSTGRES_PASSWORD: safeher

  ollama:
    image: ollama/ollama:latest
    container_name: sp-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama

  ai-service:
    build: { context: ./ai-service, dockerfile: Dockerfile }
    container_name: sp-ai
    ports: ["8085:8085"]
    depends_on:
      eureka-server:    { condition: service_healthy }
      kafka:            { condition: service_healthy }
      redis:            { condition: service_healthy }
      ollama:           { condition: service_started }
      postgres-vector:  { condition: service_healthy }
    environment:
      EUREKA_URI: http://admin:admin@eureka-server:8761/eureka
      OLLAMA_URL: http://ollama:11434
      KAFKA_BOOTSTRAP: kafka:9092
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
      VECTOR_DB_URL: postgresql+psycopg://safeher:safeher@postgres-vector:5432/safeher_vectors

volumes:
  ollama_data:
  pg_vector_data:
```

## No GPU? No problem

Ollama runs on CPU — just slower. `llama3.2:3b` takes 3–8s on CPU.
Async agents (1, 2, 4, 6) are unaffected by latency. The chatbot (Agent 5)
will feel slow on CPU — disable it until you add a GPU.

`nomic-embed-text` is small (~274 MB) and fast even on CPU — embedding a batch of 50
reviews typically takes under 5 seconds.
