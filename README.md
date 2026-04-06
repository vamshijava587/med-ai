# Med-AI Platform

Production-ready multi-agent AI platform for medical assistance.

## Services

| Service              | Port  | Description                                      |
|----------------------|-------|--------------------------------------------------|
| `auth-service`       | 8083  | Central JWT auth — issues and validates tokens   |
| `api-gateway`        | 8080  | Spring Cloud Gateway — rate limiting, routing    |
| `main-orchestrator`  | 8081  | Multi-agent LLM orchestration                    |
| `vector-data-service`| 8082  | RAG pipeline — Qdrant + Ollama embeddings        |
| `platform-shared`    | —     | Shared library (DTOs, security models)           |

## Architecture

```
Client
  │
  ▼
api-gateway :8080
  │  validates JWT → auth-service :8083
  │
  ▼
main-orchestrator :8081
  │  validates JWT → auth-service :8083
  │  RAG search  → vector-data-service :8082
  │
  ▼
vector-data-service :8082
  │  embeddings  → ollama :11434
  │  vectors     → qdrant :6333
  │  (internal/* — no JWT required)
```

## Run with Docker Compose

```bash
docker compose up --build
```

First run pulls Ollama models (~4.7 GB). Subsequent runs use cached volumes.

## Run Each Service Independently

**Step 1 — Install shared library (once, or after any shared change):**
```bash
cd platform-shared
mvn install -DskipTests
```

**Step 2 — Start infrastructure:**
```bash
docker compose up mysql qdrant ollama ollama-init -d
```

**Step 3 — Start auth-service first:**
```bash
cd auth-service
mvn spring-boot:run
```

**Step 4 — Start other services (each in its own terminal):**
```bash
cd vector-data-service && mvn spring-boot:run
cd main-orchestrator   && mvn spring-boot:run
cd api-gateway         && mvn spring-boot:run
```

## Environment Variables

| Variable             | Default                              | Used by                        |
|----------------------|--------------------------------------|--------------------------------|
| `JWT_SECRET`         | `change-this-32-char-jwt-secret-key` | auth-service only              |
| `JWT_TTL_SECONDS`    | `3600`                               | auth-service only              |
| `INTERNAL_API_KEY`   | `medai-internal-secret`              | all services                   |
| `AUTH_SERVICE_URL`   | `http://localhost:8083`              | gateway, orchestrator, vector  |
| `MYSQL_URL`          | `jdbc:mysql://localhost:3306/med_ai` | main-orchestrator              |
| `VECTOR_SERVICE_URL` | `http://localhost:8082`              | main-orchestrator              |
| `QDRANT_URL`         | `http://localhost:6333`              | vector-data-service            |
| `OLLAMA_BASE_URL`    | `http://localhost:11434`             | orchestrator, vector           |
| `OPENAI_API_KEY`     | `disabled`                           | orchestrator, vector           |

## Getting a Token (Postman / curl)

```bash
curl -s -X POST http://localhost:8083/auth/token \
  -H "Content-Type: application/json" \
  -d '{"userId": "user-123", "roles": ["USER"]}' | jq .token
```

Use the returned token as `Authorization: Bearer <token>` on all `/api/v1/**` requests.

## Flyway Note

Flyway 10+ requires `flyway-mysql` for MySQL 8.x support. It is included in `main-orchestrator/pom.xml`.
