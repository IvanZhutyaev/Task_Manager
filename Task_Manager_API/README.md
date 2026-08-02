# Task Manager API

Client-agnostic REST API for multi-user task management. The backend exposes JSON over HTTP with JWT authentication and can be consumed by **any client**: web site, SPA (React/Vue), mobile app (Android/iOS), Postman, Swagger UI.

The bundled Thymeleaf UI is an **optional demo client**, not part of the API contract.

## Architecture

```
Clients (web / mobile / SPA / Postman)
        │  HTTP + JSON + JWT
        ▼
   /api/v1/**  ← REST API (the product)
        │
   Services → Repositories → Database

Optional: /login, /projects  ← demo HTML UI (app.web-ui.enabled=true)
```

## Stack

- Java 17, Spring Boot 3
- Spring Security + JWT (stateless)
- Spring Data JPA, Liquibase
- H2 (dev/test), PostgreSQL (prod)
- CORS for cross-origin clients (SPA, separate frontend)
- OpenAPI / Swagger

## Quick start

### Requirements

- JDK 17+
- Maven 3.9+

### Run (dev, H2)

```bash
mvn spring-boot:run
```

- API base URL: `http://localhost:8080/api/v1`
- Swagger: http://localhost:8080/swagger-ui.html
- Demo UI: http://localhost:8080/login
- H2 Console: http://localhost:8080/h2-console

### Authentication

1. `POST /api/v1/auth/register` or `POST /api/v1/auth/login`
2. Use returned `token` in header: `Authorization: Bearer <token>`

### API-only mode (no demo UI)

```yaml
app:
  web-ui:
    enabled: false
```

### CORS (for SPA / separate frontend)

Configure allowed origins in `application.yml`:

```yaml
app:
  cors:
    allowed-origins:
      - http://localhost:3000
      - https://my-frontend.example.com
```

### Production (PostgreSQL)

```bash
set SPRING_PROFILES_ACTIVE=prod
set DB_URL=jdbc:postgresql://localhost:5432/taskmanager
set DB_USER=postgres
set DB_PASSWORD=postgres
set JWT_SECRET=your-256-bit-secret-key-for-production-use
mvn spring-boot:run
```

### Tests

```bash
mvn test
```

## API v1 endpoints

| Group | Endpoints |
|-------|-----------|
| Auth | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` |
| Profile | `GET/PUT /api/v1/users/me` |
| Projects | `GET/POST /api/v1/projects`, `GET/PUT/DELETE /api/v1/projects/{id}` |
| Members | `GET/POST /api/v1/projects/{id}/members`, `PUT/DELETE .../members/{userId}` |
| Boards | CRUD under `/api/v1/projects/{projectId}/boards`, `GET /api/v1/boards/{boardId}` |
| Columns | CRUD under `/api/v1/boards/{boardId}/columns` |
| Tasks | CRUD, `PATCH /api/v1/tasks/{id}/move`, filter via query params |
| Health | `GET /api/v1/health` |

## Error format (all API errors)

```json
{
  "status": 403,
  "message": "Access denied",
  "timestamp": "2026-07-01T12:00:00Z"
}
```

401/403 from Spring Security also return this JSON for `/api/**` requests.

## Project roles

- **OWNER** — full access, manage members
- **EDITOR** — create/edit boards, columns, tasks
- **VIEWER** — read-only

## Business rules (compatibility B)

- `strictBusinessRules` on project (default `false`) enables deadline/assignee/checklist/hours/Bug→HIGH gates.
- Column optional `wipLimit` / `mappedStatus` enable WIP and status-on-move without flipping legacy columns.
- Additive APIs: comments, checklist, invitations, transfer-ownership, labels, dependencies, sprints, notifications, activity, `GET /projects/{id}/tasks` search.
