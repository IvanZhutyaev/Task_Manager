# Web SPA client

Браузерный клиент к Task Manager API (отдельный origin → проверяется CORS).

## Запуск

Сначала поднимите API на `:8080`, затем:

```bash
npm start
# или: npx --yes serve -l 3000 .
```

Откройте http://localhost:3000

В форме можно сменить API base URL (по умолчанию `http://localhost:8080/api/v1`).

## Возможности

- регистрация / вход
- проекты, доски, колонки, задачи
- перенос задачи между колонками (`PATCH /tasks/{id}/move`)
