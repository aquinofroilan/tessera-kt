# Observability (Prometheus + Grafana)

This repository exposes Spring Boot Actuator metrics in Prometheus format and provides a ready-to-run Prometheus + Grafana setup via Docker Compose.

## 1) Run the application

Start the app (defaults to port `8080` and context path `/api`):

```sh
bash ./gradlew bootRun
```

Prometheus metrics endpoint:

- `http://localhost:8080/api/actuator/prometheus`

Custom app metrics added in code (examples):

- `synectix_fx_auto_fetch_job_runs_total`
- `synectix_fx_auto_fetch_job_failures_total`
- `synectix_fx_auto_fetch_base_fetches_total` (tags: `base`, `outcome`)
- `synectix_fx_auto_fetch_rate_upserts_total` (tag: `outcome`)

## 2) Run Prometheus + Grafana

From the repo root:

```sh
docker compose -f observability/compose.yaml up
```

Services:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (default login is `admin` / `admin` unless you changed it)

Grafana is provisioned with a Prometheus datasource pointing at the Prometheus container (`http://prometheus:9090`).

## 3) Import a dashboard (optional)

In Grafana, import a Spring Boot dashboard, for example:

- Grafana Dashboard ID `4701` (“Spring Boot Statistics”)

## Notes

- Prometheus scrapes the app via `host.docker.internal:8080` and `metrics_path: /api/actuator/prometheus` (see `observability/prometheus/prometheus.yml`).
- The `/actuator/prometheus` endpoint is explicitly permitted in `SecurityConfig`, so Prometheus can scrape without authentication.

## Production hardening

The Prometheus endpoint is intentionally unauthenticated so the scraper can reach it without a user JWT (the app has no service-account token mechanism). Because metrics can leak internal signals (heap usage, request patterns, per-org/rate counters), do **not** expose `/actuator/prometheus` on a public interface in production. Recommended options:

- Restrict access at the network layer (firewall/security group / service mesh) so only the Prometheus scraper can reach it.
- Or move actuator to a separate, non-public management port via `management.server.port` and update `observability/prometheus/prometheus.yml` accordingly.
