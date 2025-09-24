You are an expert DevOps engineer. Inside infrastructure/ (already created by workspace), generate production-ready Docker, Kubernetes, Helm templates, GitHub Actions, monitoring stack config, and ops runbook tailored to the Java backend + React frontend + **either** MSSQL (SQL Server) or Oracle DB.

Key changes vs a cloud-only Azure SQL prompt:
- The infra templates must be DB-agnostic and support two DB types:
  - `mssql` (SQL Server) — good for local dev (docker-compose) and production (managed SQL Server).
  - `oracle` (Oracle XE local for dev or managed Oracle DB in production) — include guidance that Oracle is typically provided as a managed service in production and is not usually run inside the same Kubernetes cluster.
- docker-compose:
  - Include `mssql` service by default.
  - Include a **commented** `oracle` service template with notes: which public container images are commonly used (community images) and legal/licensing caveats; instruct the user to enable it consciously and add the Oracle JDBC driver.
- Helm chart / k8s:
  - Provide templates with env placeholders: `DB_TYPE`, `JDBC_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_SECRET_NAME`.
  - For production values, recommend using managed DB (Azure SQL / AWS RDS / Oracle Cloud DB) and show how to configure external DB access.
  - Provide sample `secrets.yaml` to store DB creds (templated), and show example `sealedsecret` or Vault integration.
- Backups & DR:
  - For MSSQL: include a script that uses `sqlcmd`/`sqlpackage` or `mssql-scripter` to export schema/data (or use bacpac).
  - For Oracle: provide a sample backup script using `expdp`/`impdp` or `Data Pump`, with instructions to run against the DB host. If using Oracle XE container locally, provide example `docker exec` commands.
- CI/CD:
  - The CD pipeline should not assume Azure: provide steps to push images to a registry (GHCR/ACR) and deploy via `helm upgrade --install` to a generic Kubernetes cluster. Include placeholders for registry and cluster credentials.
- Observability:
  - Prometheus/Grafana + Loki setup as before; include DB-specific metrics endpoints that can be scraped: for MSSQL use exporters (sql_exporter or windows exporter) or use JDBC metrics exporter; for Oracle mention `oracle_exporter` or `ojdbc`-based exporters (document that additional exporters and permissions may be required).
- Scripts & runbook:
  - `ops/README-ops.md` will include DB-specific runbook sections: how to rotate DB credentials, how to restore from a MSSQL bacpac or Oracle dump, how to grant necessary monitoring user permissions for exporters, and how to handle cross-DB migration considerations.
- Deliverables:
  - `infrastructure/helm-chart/*` (DB-agnostic templates)
  - `infrastructure/k8s/*` plain manifests
  - `infrastructure/monitoring/*` Prometheus/Grafana values and dashboard JSON
  - `.github/workflows/ci.yml` and `.github/workflows/cd.yml` CI/CD pipelines (registry-neutral)
  - `ops/README-ops.md` runbook, with MSSQL and Oracle sections
  - `security/trivy.yml` or trivy CI fragment.

Constraints & notes:
- Do NOT embed credentials or JDBC drivers.
- When Oracle driver is required, include explicit instructions to add `ojdbc` to project or Maven repo or mount it into the container — do not attempt to fetch it automatically.
- For production, recommend managed DB services (e.g., managed SQL Server or managed Oracle DB). If the user chooses Oracle in production, note licensing and performance considerations.
- Provide TODO placeholders and clear instructions for how to enable Oracle locally and how to run DB-specific backups/restores.
