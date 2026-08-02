# Клиенты к Task Manager API

Один backend (`Task_Manager_API`) — три разных клиента. Все ходят в один и тот же REST API:

```
http://localhost:8080/api/v1
Authorization: Bearer <JWT>
```

| Клиент | Папка | Стек | Зачем |
|--------|--------|------|--------|
| Веб-сайт | `web-spa/` | HTML + JS | SPA в браузере (CORS) |
| Android | `android/` | Kotlin | Нативное мобильное приложение |
| CLI | `cli/` | Python 3 | Терминал / автоматизация |

Новые возможности API (совместимость B): `strictBusinessRules`, WIP/`mappedStatus` колонок, overdue, comments/checklist, invites, transfer ownership, labels/deps/sprints, notifications, activity, поиск `GET /projects/{id}/tasks`.

Клиенты выровнены под актуальный `/api/v1`:
- **Thymeleaf UI** (`:8080`) — invite, assignee, WIP, search, activity, badges настроек
- **Web SPA** (`:3000`) — edit task, assignee, WIP/mappedStatus, invites, labels/sprints/activity
- **Android** — create/move task, invite, members, search, notifications
- **CLI** — CRUD-срез API + `settings`/`goals`/`risks`/`approve`/`time-report` + templates

### Бизнес-фичи (совместимость B)

Все жёсткие правила **выключены по умолчанию**. Включаются через `PATCH /projects/{id}/settings` или флаги при создании:
workflow, capacity, SLA, DoD templates, approval→DONE, priority queue, sprint unfinished %, auto-archive, CONTRACTOR, goals/OKR, risks, time-in-status, time-report CSV, шаблоны `KANBAN|SCRUM|BUG_TRIAGE|PERSONAL`.

Строгие правила включаются флагом проекта; без него старый CRUD ведёт себя как раньше.

## Перед запуском клиентов

1. Поднимите API:

```bash
cd ../Task_Manager_API
mvn spring-boot:run
```

2. Проверьте health: http://localhost:8080/api/v1/health

## Быстрый старт

### Веб (`web-spa`)

```bash
cd web-spa
npx --yes serve -l 3000
```

Откройте http://localhost:3000

### Android

Откройте папку `android/` в Android Studio → Run на эмуляторе.  
Эмулятор ходит к хосту через `http://10.0.2.2:8080` (уже задано в `ApiConfig.kt`).

### CLI

```bash
cd cli
pip install -r requirements.txt
python tm.py register --email you@example.com --password secret12 --name You
python tm.py login --email you@example.com --password secret12
python tm.py projects
```

## Идея для курсовой

Backend не знает, кто клиент. Контракт один: JSON + JWT.  
Демо-UI внутри Spring (Thymeleaf) — тоже просто ещё один клиент.  
Эти три клиента в `clients/` показывают универсальность API «как есть».
