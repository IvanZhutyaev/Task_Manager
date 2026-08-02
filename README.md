# Task Manager

Многопользовательский менеджер задач: REST API + несколько клиентов.

## Структура

```
Task_Manager_API/   Spring Boot backend (Java 17, JWT, Liquibase)
clients/web-spa/    Браузерный SPA (HTML/JS/CSS)
clients/android/    Android-клиент (Kotlin)
clients/cli/        CLI (Python)
```

## Docker (рекомендуемый запуск)

Нужны Docker Desktop / Docker Engine + Compose.

```bash
# из корня репозитория
cp .env.example .env   # опционально: пароли и JWT_SECRET
docker compose up --build -d
```

> Если Compose ругается на пустое имя проекта (кириллица в пути), уже задано `name: taskmanager` в `docker-compose.yml`. Альтернатива: `docker compose -p taskmanager up --build -d`.

Поднимутся:
- **API** — http://localhost:8080 (`/api/v1`, Swagger, demo UI `/login`)
- **Web SPA** — http://localhost:3000
- **PostgreSQL** — `localhost:5432`

Остановка: `docker compose down`  
С очисткой данных БД: `docker compose down -v`

Без Postgres (H2 in-memory):

```bash
docker compose -f docker-compose.dev.yml up --build
```

## Backend (локально без Docker)

```bash
cd Task_Manager_API
mvn spring-boot:run
```

- API: http://localhost:8080/api/v1  
- Swagger: http://localhost:8080/swagger-ui.html  
- Demo UI: http://localhost:8080/login  

## Клиенты

Все ходят в один контракт: `JSON + JWT`.

### Web SPA

```bash
cd clients/web-spa
npx --yes serve -l 3000
```

Откройте http://localhost:3000

### Android

Откройте `clients/android` в Android Studio и Run на эмуляторе.  
URL по умолчанию: `http://10.0.2.2:8080/api/v1`

### CLI

```bash
cd clients/cli
tm.bat health
# или: python tm.py health
```

## Стек

- Java 17, Spring Boot 3, Spring Security + JWT  
- Spring Data JPA, Liquibase, H2 (dev) / PostgreSQL (prod)  
- OpenAPI / Swagger  
