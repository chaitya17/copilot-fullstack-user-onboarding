You are an expert devtools engineer. Create a complete, ready-to-use workspace for the "user-onboard" project so I can immediately open it in Intellij  (or GitHub Codespaces) and use GitHub Copilot to generate the app. Do not implement business logic here — only create the workspace, repo layout, tooling, devcontainer, and CI skeletons that the other Copilot prompts will fill.

Important DB note: This workspace must support **either** local MS SQL Server (MSSQL) **or** Oracle (XE / managed) as the backing relational database. Default local dev will use MSSQL for convenience, but include a commented-out Oracle service template and clear instructions showing how to switch to Oracle by setting `DB_TYPE=oracle` and supplying an appropriate `JDBC_URL` and JDBC driver.

Requirements:
- Repo name: user-onboard
- Root structure:
    - backend/
    - frontend/
    - infrastructure/
    - docs/
    - .github/workflows/
    - docker-compose.yml (local dev: backend, frontend, mssql for SQL Server compatibility, optional commented oracle-xe service, and a RabbitMQ queue for decoupling)
    - Makefile with common commands: `make build`, `make up`, `make test`, `make clean`, `make docker-build`
    - devcontainer.json for Intellij with Java 17, Maven, Node 20, Docker CLI, Azure CLI optional, kubectl, helm, and recommended extensions (vscjava.vscode-java-pack, ms-azuretools.vscode-docker, ms-kubernetes-tools.vscode-kubernetes-tools)
    - .vscode/settings.json and recommended extensions file
    - .editorconfig, .gitattributes, .gitignore
    - README.md with workspace setup steps (how to open devcontainer, run docker-compose, where to paste secrets, how to use Copilot to run each prompt)
- docker-compose.yml details:
    - service `mssql` using official MS SQL Server image `mcr.microsoft.com/mssql/server:2019-latest` with SA_PASSWORD from env file `.env.local`, expose port 1433 for local dev only.
    - Include an **optional commented** `oracle` service block with instructions pointing to Oracle XE image usage (note: Oracle images sometimes require accepting Oracle license or pulling from Oracle Container Registry; include instructions and a placeholder image reference `oracleinanutshell/oracle-xe-11g` or `gvenzl/oracle-xe` with comment). Do not enable Oracle by default.
    - service `rabbitmq` (official image) for queue decoupling.
    - service `backend` build from `backend/Dockerfile` with env placeholders for `DB_TYPE`, JDBC URL, JWT key mounts, and RabbitMQ URL.
    - service `frontend` build from `frontend/Dockerfile`, serve on port 3000.
    - healthchecks for backend and frontend.
- Provide `.env.example` (for local dev) with placeholders:
    - DB_TYPE=mssql          # or "oracle"
    - MSSQL_SA_PASSWORD=
    - JDBC_URL=jdbc:sqlserver://mssql:1433;databaseName=useronboard
    - ORACLE_JDBC_URL=jdbc:oracle:thin:@oracle:1521/XEPDB1
    - ORACLE_SYS_PASSWORD=
    - JWT_PUBLIC_KEY_PATH=
    - JWT_PRIVATE_KEY_PATH=
    - RABBITMQ_URL=amqp://guest:guest@rabbitmq:5672/
    - FRONTEND_API_URL=http://localhost:8080
    - SPRING_PROFILES_ACTIVE=local
- Add initial GitHub Actions workflow stubs:
    - `.github/workflows/ci.yml` that runs a matrix: backend tests (mvn -DskipTests=false test), frontend tests (npm test), lints; artifacts placeholder.
    - `.github/workflows/build-and-push.yml` skeleton to build images and push to GHCR or other registry (use `${{ secrets.REGISTRY_USERNAME }}` placeholders). Do not include secrets — use `${{ secrets.* }}` placeholders and document required secrets.
- Add `infrastructure/README.md` explaining local vs cloud SQL options: how to use local MSSQL (docker-compose), how to switch to Oracle XE local container (instructions and caveats), and how to point to a managed Oracle/SQL server in production. Explain JDBC_URL env swapping.
- Add `docs/copilot-guides.md` with exact instructions how to run the next three Copilot prompts (where to paste them and which directories to run them in).
- Add `scripts/` folder with helper scripts: `wait-for-it.sh`, `db-init/` with a sample Flyway/V1__init.sql placeholder and instructions for Oracle vs MSSQL differences.

Constraints:
- Do NOT add any real secrets or private keys.
- Use clear TODO comments where the developer must add secrets or DB details.
- Make the workspace friendly for both local dev (docker-compose) and cloud (K8s/ACR/Vault).
- In docker-compose, keep Oracle section commented-out by default and include clear instructions showing how to enable it and provide driver.
