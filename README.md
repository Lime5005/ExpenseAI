# Expense-AI

Conversational expense tracking with tool-calling, monthly insights, and built-in observability (traces, metrics, logs).

## Prerequisites

- Java 21
- Maven 3.9+
- Docker + Docker Compose (for the full stack)
- Ollama running locally
  - Models used by default:
    - `qwen2.5:7b-instruct`
    - `nomic-embed-text:latest`
  - Example:
    - `ollama pull qwen2.5:7b-instruct`
    - `ollama pull nomic-embed-text:latest`

## Quick start (Docker)

```bash
docker compose up --build
```

Services and ports:
- App: http://localhost:8080
- Jaeger: http://localhost:16686
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000
- Loki: http://localhost:3100

The app container expects Ollama on `host.docker.internal:11434`.

## Local run (without Docker)

```bash
mvn spring-boot:run
```

Ollama is expected at `http://localhost:11434` (see `src/main/resources/application.yml`).

## Endpoints

UI:
- `GET /` — main dashboard

Chat:
- `POST /chat` — conversational expense entry
  - body: `{ "message": "In July 25th, 2025, I bought a coffee, cost 6.66 euros" }`

Expenses:
- `GET /expenses` — list all expenses
- `GET /expenses/month/{yyyy-MM}` — list by month
- `GET /expenses/date/{yyyy-MM-dd}` — list by date
- `POST /expenses` — create expense
- `PUT /expenses/{id}` — update expense

Insights:
- `GET /insight?month=yyyy-MM&lang=en` — monthly insight (language optional)

H2 console:
- `GET /h2-console`

## Observability

OpenTelemetry Collector runs via `otel/collector-config.yml` and exports:
- Traces → Jaeger + Azure Monitor
- Metrics → Prometheus + Azure Monitor
- Logs → Loki + Azure Monitor

### Jaeger (Traces)
- Open: http://localhost:16686

### Prometheus (Metrics)
- Open: http://localhost:9090
- Check scrape targets: http://localhost:9090/targets

### Grafana (Metrics + Logs)
- Open: http://localhost:3000 (admin/admin by default)
- Add data sources:
  - Prometheus: `http://prometheus:9090`
  - Loki: `http://loki:3100`
  - Jaeger (optional): `http://jaeger:16686`

### Azure Monitor (optional)

Set `APPLICATIONINSIGHTS_CONNECTION_STRING` before starting:

```bash
export APPLICATIONINSIGHTS_CONNECTION_STRING="InstrumentationKey=...;IngestionEndpoint=..."
```

The collector will send traces/metrics/logs to Azure Monitor via the `azuremonitor` exporter.

## Configuration

- App config: `src/main/resources/application.yml`
- Collector config: `otel/collector-config.yml`
- Prometheus config: `otel/prometheus.yml`
- Docker compose: `docker-compose.yml`

## Notes

- The app uses H2 by default for simplicity. For production or vector search, consider PostgreSQL + pgvector.
- If Grafana shows no Loki labels/data, ensure the Loki data source uses `http://loki:3100` (not `localhost`).
