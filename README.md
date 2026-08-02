# Task Manager

Многопользовательский менеджер задач: REST API + несколько клиентов.

## Структура

```
Task_Manager_API/   Spring Boot backend (Java 17, JWT, Liquibase)
                    + Web UI (Thymeleaf) на том же порту
clients/web-spa/    Браузерный SPA (HTML/JS/CSS)
clients/cli/        CLI (Python)
clients/android/    Android-клиент (Kotlin) — только локально / Android Studio
```

Клиенты ориентированы на пользователя: без технических ID, Swagger и сырых enum’ов в интерфейсе.  
Swagger доступен только у API: http://localhost:8080/swagger-ui.html

## Docker (рекомендуемый запуск)

Нужны Docker Desktop / Docker Engine + Compose.

Одним командой поднимаются **backend** и **все клиенты, кроме Android**:

| Сервис | Что это | Адрес / как пользоваться |
|--------|---------|--------------------------|
| `api` | REST API + Web UI | http://localhost:8080 — API `/api/v1`, вход `/login`, Swagger `/swagger-ui.html` |
| `web-spa` | Браузерный SPA | http://localhost:3000 |
| `cli` | Python CLI | `docker compose exec cli tm …` |
| `postgres` | БД (prod-профиль) | `localhost:5432` |

Android в Docker **не** входит: нужен эмулятор / устройство и Android Studio.

### Быстрый старт

```bash
# из корня репозитория
cp .env.example .env   # опционально: пароли и JWT_SECRET
docker compose up --build -d
```

> В `docker-compose.yml` задано `name: taskmanager` (удобно при кириллице в пути папки).  
> Альтернатива: `docker compose -p taskmanager up --build -d`.

Проверка:

```bash
curl http://localhost:8080/api/v1/health
# браузер: http://localhost:3000  и  http://localhost:8080/login
docker compose exec cli tm health
```

### CLI в Docker

Токен и настройки хранятся в volume `cli-data` (`HOME=/data` → `/data/.taskmanager_cli.json`).

```bash
docker compose exec cli tm health
docker compose exec cli tm register --email cli@example.com --password secret12 --name "CLI User"
docker compose exec cli tm login --email cli@example.com --password secret12
docker compose exec cli tm projects

# одноразовый запуск (переопределяет sleep infinity):
docker compose run --rm --entrypoint tm cli health
```

Внутри сети Compose CLI ходит на API по адресу `http://api:8080/api/v1` (переменная `TM_API_BASE`).

### Остановка

```bash
docker compose down          # остановить контейнеры
docker compose down -v       # + удалить данные Postgres и CLI
```

### Режим без PostgreSQL (H2)

```bash
docker compose -f docker-compose.dev.yml up --build -d
```

То же самое: `api` (с Web UI), `web-spa`, `cli` — без сервиса `postgres`.

### Переменные окружения

См. `.env.example`: `POSTGRES_*`, `JWT_SECRET`.

---

## Backend (локально без Docker)

```bash
cd Task_Manager_API
mvn spring-boot:run
```

- API: http://localhost:8080/api/v1  
- Swagger: http://localhost:8080/swagger-ui.html  
- Web UI: http://localhost:8080/login  

Prod-профиль: `SPRING_PROFILES_ACTIVE=prod` + `DB_URL` / `DB_USER` / `DB_PASSWORD` + `JWT_SECRET`.

## Клиенты (локально без Docker)

Все ходят в один контракт: `JSON + JWT`.

### Web SPA

```bash
cd clients/web-spa
npx --yes serve -l 3000
```

Откройте http://localhost:3000  
API по умолчанию: `http://localhost:8080/api/v1` (без настроек в UI).

### Web UI (Thymeleaf)

Идёт вместе с backend: http://localhost:8080/login

### CLI

```bash
cd clients/cli
tm.bat health
# или: python tm.py health
# полный JSON при необходимости: tm.bat projects --json
```

### Android

В Docker не запускается. Откройте `clients/android` в Android Studio и Run на эмуляторе.  
URL по умолчанию: `http://10.0.2.2:8080/api/v1` (API на хосте).  
Сменить URL можно долгим нажатием на бренд на экране входа.

## Стек

- Java 17, Spring Boot 3, Spring Security + JWT  
- Spring Data JPA, Liquibase, H2 (dev) / PostgreSQL (prod)  
- OpenAPI / Swagger (только API)  
- Docker Compose: API + Postgres + Web SPA + CLI  
