# Task Manager

Multi-user task manager: REST API + several clients.

## Structure

```
Task_Manager_API/   Spring Boot backend (Java 17, JWT, Liquibase)
                    + Web UI (Thymeleaf) on the same port
clients/web-spa/    Browser SPA (HTML/JS/CSS)
clients/cli/        CLI (Python)
clients/android/    Android client (Kotlin) — local / Android Studio only
```

Clients are user-oriented: no technical IDs, Swagger, or raw enums in the interface.  
Swagger is available only on the API: http://localhost:8080/swagger-ui.html

## Docker (recommended launch)

Requires Docker Desktop / Docker Engine + Compose.

A single command brings up **backend** and **all clients except Android**:

| Service | What it is | Address / how to use |
|--------|---------|--------------------------|
| `api` | REST API + Web UI | http://localhost:8080 — API `/api/v1`, login `/login`, Swagger `/swagger-ui.html` |
| `web-spa` | Browser SPA | http://localhost:3000 |
| `cli` | Python CLI | `docker compose exec cli tm …` |
| `postgres` | DB (prod profile) | `localhost:5432` |

Android is **not** included in Docker: you need an emulator / device and Android Studio.

### Quick start

```bash
# from the repository root
cp .env.example .env   # optional: passwords and JWT_SECRET
docker compose up --build -d
```

> `docker-compose.yml` specifies `name: taskmanager` (convenient when the folder path contains Cyrillic characters).  
> Alternative: `docker compose -p taskmanager up --build -d`.

Check:

```bash
curl http://localhost:8080/api/v1/health
# browser: http://localhost:3000  and  http://localhost:8080/login
docker compose exec cli tm health
```

### CLI in Docker

The token and settings are stored in the `cli-data` volume (`HOME=/data` → `/data/.taskmanager_cli.json`).

```bash
docker compose exec cli tm health
docker compose exec cli tm register --email cli@example.com --password secret12 --name "CLI User"
docker compose exec cli tm login --email cli@example.com --password secret12
docker compose exec cli tm projects

# one-off run (overrides sleep infinity):
docker compose run --rm --entrypoint tm cli health
```

Inside the Compose network, the CLI connects to the API at `http://api:8080/api/v1` (the `TM_API_BASE` variable).

### Stopping

```bash
docker compose down          # stop containers
docker compose down -v       # + delete Postgres and CLI data
```

### Mode without PostgreSQL (H2)

```bash
docker compose -f docker-compose.dev.yml up --build -d
```

Same thing: `api` (with Web UI), `web-spa`, `cli` — without the `postgres` service.

### Environment variables

See `.env.example`: `POSTGRES_*`, `JWT_SECRET`.

---

## Backend (locally without Docker)

```bash
cd Task_Manager_API
mvn spring-boot:run
```

- API: http://localhost:8080/api/v1  
- Swagger: http://localhost:8080/swagger-ui.html  
- Web UI: http://localhost:8080/login  

Prod profile: `SPRING_PROFILES_ACTIVE=prod` + `DB_URL` / `DB_USER` / `DB_PASSWORD` + `JWT_SECRET`.

## Clients (locally without Docker)

All use the same contract: `JSON + JWT`.

### Web SPA

```bash
cd clients/web-spa
npx --yes serve -l 3000
```

Open http://localhost:3000  
API by default: `http://localhost:8080/api/v1` (no settings in the UI).

### Web UI (Thymeleaf)

Comes with the backend: http://localhost:8080/login

### CLI

```bash
cd clients/cli
tm.bat health
# or: python tm.py health
# full JSON if needed: tm.bat projects --json
```

### Android

Does not run in Docker. Open `clients/android` in Android Studio and Run on an emulator.  
Default URL: `http://10.0.2.2:8080/api/v1` (API on the host).  
You can change the URL by long-pressing the brand on the login screen.

## Stack

- Java 17, Spring Boot 3, Spring Security + JWT  
- Spring Data JPA, Liquibase, H2 (dev) / PostgreSQL (prod)  
- OpenAPI / Swagger (API only)  
- Docker Compose: API + Postgres + Web SPA + CLI
