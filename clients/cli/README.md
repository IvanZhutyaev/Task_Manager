# Task Manager CLI

Клиент командной строки к тому же API, что Web SPA и Android.

## Запуск

Нужен Python 3.10+ (только стандартная библиотека).

```bash
# API должен быть запущен на :8080
python tm.py health          # → OK
python tm.py config            # адрес API и маскированный токен (без сырого JWT)

python tm.py register --email cli@example.com --password secret12 --name "CLI User"
python tm.py projects --create "Demo from CLI" --description "created by terminal client"
# → Создан проект #1 «Demo from CLI»

python tm.py projects          # #id  название  роль
python tm.py boards 1 --create "Sprint"
python tm.py columns 1 --create "Todo"
python tm.py tasks 1 --create "First task" --priority HIGH
python tm.py tasks 1           # список задач колонки

# полный JSON ответа API (power users):
python tm.py health --json
python tm.py whoami --json
python tm.py projects --json
python tm.py task 1 --json
```

Токен сохраняется в `~/.taskmanager_cli.json`.

Другой адрес API:

```bash
python tm.py config --base-url http://127.0.0.1:8080/api/v1
# или
set TM_API_BASE=http://127.0.0.1:8080/api/v1
```
