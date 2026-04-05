# Med AI Platform

This workspace contains a Java 21, Spring Boot WebFlux, and Spring AI multi-agent platform built as three applications:

- `main-orchestrator`: the single-app multi-agent runtime with AI routing, tool orchestration, evaluation, memory, audit logging, and SSE streaming
- `api-gateway`: Spring Cloud Gateway with JWT validation and rate limiting
- `vector-data-service`: chunking, embedding, Qdrant indexing, and RAG retrieval

## Core architecture

- Agents inside one orchestrator app: `Symptom`, `Allergy`, `Medication`, `Triage`, `Financial`, `Fallback`
- AI-driven routing with prompt-based JSON decisions, not keyword routing
- Primary model provider: Ollama
- Secondary fallback provider: OpenAI
- Post-response evaluation for hallucination, confidence, and relevance
- Safe fallback agent path when validation fails or providers are unavailable
- Tool loop driven by LLM decisions with Spring AI `@Tool`-annotated methods
- Sliding-window memory and persistence in MySQL
- Separate vector collections per agent in Qdrant
- Docker Compose for local development

## Run locally

1. Set `OPENAI_API_KEY` if you want OpenAI provider fallback.
2. Set `JWT_SECRET` to a shared HMAC secret used by all services.
3. Start the stack:

```bash
docker compose up --build
```

4. Use the gateway at `http://localhost:8080`.

## Example JWT

The services expect an HMAC-signed JWT whose `sub` matches `userId`.

## Important note

This environment does not currently include Maven, so the workspace has not been locally compiled here. The Dockerfiles use Maven build stages so the stack can still be built in Docker or in CI.
