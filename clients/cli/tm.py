"""
Task Manager CLI — HTTP client for the same /api/v1 backend.
"""

from __future__ import annotations

import argparse
import json
import os
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

DEFAULT_BASE = os.environ.get("TM_API_BASE", "http://localhost:8080/api/v1")
CONFIG_PATH = Path.home() / ".taskmanager_cli.json"

ROLE_RU = {
    "OWNER": "владелец",
    "ADMIN": "админ",
    "EDITOR": "редактор",
    "VIEWER": "наблюдатель",
    "MEMBER": "участник",
    "CONTRACTOR": "подрядчик",
}

STATUS_RU = {
    "BACKLOG": "бэклог",
    "IN_PROGRESS": "в работе",
    "DONE": "готово",
    "ARCHIVED": "архив",
    "OPEN": "открыт",
    "CLOSED": "закрыт",
    "ACTIVE": "активен",
    "PLANNED": "запланирован",
    "PENDING": "ожидает",
    "RESOLVED": "решён",
    "APPROVED": "одобрено",
    "REJECTED": "отклонено",
    "ACCEPTED": "принято",
    "DECLINED": "отклонено",
}

PRIORITY_RU = {
    "LOW": "низкий",
    "MEDIUM": "средний",
    "HIGH": "высокий",
}

ORG_TYPE_RU = {
    "LOCAL": "локальная",
    "COMMERCIAL": "коммерческая",
}

ACCESS_RU = {
    "OPEN": "открытая",
    "PRIVATE": "приватная",
    "TEAM_ACL": "по командам",
}

EPILOG = """
Группы команд:
  Auth       config, health, register, login, whoami
  Workspace  projects, orgs, teams, settings, goals, risks, members,
             my-invites, invite, accept-invite, decline-invite, transfer,
             search, labels, sprints, notifications, activity, time-report, approve
  Board      boards, columns, tasks, move
  Task       task, comments, checklist, deps, suggest-dod
"""


def load_config() -> dict:
    if CONFIG_PATH.exists():
        return json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
    return {"base_url": DEFAULT_BASE, "token": None}


def save_config(cfg: dict) -> None:
    CONFIG_PATH.write_text(json.dumps(cfg, indent=2), encoding="utf-8")


def mask_token(token: str | None) -> str:
    if not token:
        return "(не задан)"
    if len(token) <= 8:
        return "***"
    return f"{token[:4]}…{token[-4:]}"


def human_role(role: str | None) -> str:
    if not role:
        return "?"
    return ROLE_RU.get(role, role.lower())


def human_status(status: str | None) -> str:
    if not status:
        return "?"
    return STATUS_RU.get(status, status.lower())


def human_priority(priority: str | None) -> str:
    if not priority:
        return "?"
    return PRIORITY_RU.get(priority, priority.lower())


def emit_json(args: argparse.Namespace, data) -> bool:
    if getattr(args, "json", False):
        pp(data)
        return True
    return False


def add_json_flag(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--json", action="store_true", help="вывести полный JSON ответа API")


def request(method: str, path: str, body: dict | None = None, token: str | None = None, base_url: str | None = None):
    cfg = load_config()
    base = (base_url or cfg.get("base_url") or DEFAULT_BASE).rstrip("/")
    url = f"{base}{path}"
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    auth = token if token is not None else cfg.get("token")
    if auth:
        headers["Authorization"] = f"Bearer {auth}"

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read().decode("utf-8")
            if not raw:
                return None
            return json.loads(raw)
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        try:
            payload = json.loads(detail)
            message = payload.get("message", detail)
        except json.JSONDecodeError:
            message = detail or str(exc)
        raise SystemExit(f"API error {exc.code}: {message}") from None
    except urllib.error.URLError as exc:
        raise SystemExit(f"Cannot reach API at {url}: {exc.reason}") from None


def pp(data) -> None:
    print(json.dumps(data, indent=2, ensure_ascii=False))


def format_task_summary(task: dict) -> str:
    lines = [
        f"#{task['id']}  {task['title']}",
        f"  статус:    {human_status(task.get('status'))}",
        f"  приоритет: {human_priority(task.get('priority'))}",
    ]
    if task.get("assigneeName"):
        lines.append(f"  исполнитель: {task['assigneeName']}")
    if task.get("deadline"):
        lines.append(f"  дедлайн: {task['deadline']}")
    if task.get("columnId"):
        lines.append(f"  колонка: #{task['columnId']}")
    if task.get("description"):
        desc = task["description"]
        if len(desc) > 120:
            desc = desc[:117] + "..."
        lines.append(f"  описание: {desc}")
    return "\n".join(lines)


def cmd_config(args: argparse.Namespace) -> None:
    cfg = load_config()
    if args.base_url:
        cfg["base_url"] = args.base_url.rstrip("/")
        save_config(cfg)
        print(f"API base URL -> {cfg['base_url']}")
        return

    user = cfg.get("user") or {}
    email = user.get("email")
    token = cfg.get("token")
    print(f"Конфиг: {CONFIG_PATH}")
    print(f"API:    {cfg.get('base_url') or DEFAULT_BASE}")
    if token:
        print(f"Токен:  {mask_token(token)}")
        if email:
            print(f"Вход:   {email}")
        else:
            print("Вход:   токен сохранён")
    else:
        print("Вход:   не выполнен (tm login / tm register)")


def cmd_register(args: argparse.Namespace) -> None:
    data = request(
        "POST",
        "/auth/register",
        {"email": args.email, "password": args.password, "name": args.name},
        token="",
    )
    cfg = load_config()
    cfg["token"] = data["token"]
    cfg["user"] = data["user"]
    save_config(cfg)
    print(f"Registered and logged in as {data['user']['email']}")


def cmd_login(args: argparse.Namespace) -> None:
    data = request(
        "POST",
        "/auth/login",
        {"email": args.email, "password": args.password},
        token="",
    )
    cfg = load_config()
    cfg["token"] = data["token"]
    cfg["user"] = data["user"]
    save_config(cfg)
    print(f"Logged in as {data['user']['email']}")


def cmd_whoami(args: argparse.Namespace) -> None:
    data = request("GET", "/users/me")
    if emit_json(args, data):
        return
    print(f"#{data['id']}  {data.get('name', '?')}  <{data['email']}>")


def cmd_projects(args: argparse.Namespace) -> None:
    if args.create:
        body = {
            "name": args.create,
            "description": args.description,
            "strictBusinessRules": bool(args.strict),
            "withDefaultBoard": bool(args.with_default_board),
            "template": args.template,
        }
        if args.org:
            body["organizationId"] = args.org
        data = request("POST", "/projects", body)
        if emit_json(args, data):
            return
        print(f"Создан проект #{data['id']} «{data['name']}»")
        return

    data = request("GET", "/projects")
    if emit_json(args, data):
        return
    if not data:
        print("No projects")
        return
    for p in data:
        role = human_role(p.get("currentUserRole"))
        print(f"#{p['id']:>3}  {p['name']:<28}  {role}")


def cmd_orgs(args: argparse.Namespace) -> None:
    if args.create:
        data = request("POST", "/organizations", {"name": args.create, "type": args.type or "LOCAL"})
        if emit_json(args, data):
            return
        org_type = ORG_TYPE_RU.get(data.get("type", ""), data.get("type", ""))
        print(f"Создана организация #{data['id']} «{data['name']}» ({org_type})")
        return
    if args.add_member:
        request(
            "POST",
            f"/organizations/{args.org_id}/members",
            {"email": args.add_member, "role": args.role or "MEMBER"},
        )
        print(f"Участник {args.add_member} добавлен в организацию #{args.org_id}")
        return
    if args.org_id:
        data = request("GET", f"/organizations/{args.org_id}")
        if emit_json(args, data):
            return
        org_type = ORG_TYPE_RU.get(data.get("type", ""), data.get("type", ""))
        print(f"#{data['id']}  {data['name']}  ({org_type})")
        return

    data = request("GET", "/organizations") or []
    if emit_json(args, data):
        return
    for o in data:
        org_type = ORG_TYPE_RU.get(o.get("type", ""), o.get("type", ""))
        role = human_role(o.get("currentUserRole"))
        print(f"#{o['id']:>3}  {o['name']:<24}  {org_type:<12}  {role}")


def cmd_teams(args: argparse.Namespace) -> None:
    if args.create:
        data = request("POST", f"/organizations/{args.org_id}/teams", {"name": args.create})
        print(f"Создана команда #{data['id']} «{data['name']}»")
        return
    if args.add_member:
        request(
            "POST",
            f"/organizations/{args.org_id}/teams/{args.team_id}/members",
            {"email": args.add_member},
        )
        print(f"Участник {args.add_member} добавлен в команду #{args.team_id}")
        return
    if args.team_id:
        for m in request("GET", f"/organizations/{args.org_id}/teams/{args.team_id}/members") or []:
            print(f"#{m['userId']:>3}  {m['name']}  <{m['email']}>")
        return
    for t in request("GET", f"/organizations/{args.org_id}/teams") or []:
        print(f"#{t['id']:>3}  {t['name']}")


def cmd_settings(args: argparse.Namespace) -> None:
    body = {}
    if args.capacity is not None:
        body["capacityLimitHours"] = args.capacity
    if args.priority_queue:
        body["priorityQueueRules"] = True
    if args.require_approval:
        body["requireApprovalForDone"] = True
    if args.sla_warning is not None:
        body["slaWarningDays"] = args.sla_warning
    if args.auto_archive is not None:
        body["autoArchiveDoneDays"] = args.auto_archive
    if args.dod:
        body["dodTemplatesEnabled"] = True
    if not body:
        pp(request("GET", f"/projects/{args.project_id}"))
        return
    request("PATCH", f"/projects/{args.project_id}/settings", body)
    print(f"Настройки проекта #{args.project_id} обновлены")


def cmd_goals(args: argparse.Namespace) -> None:
    if args.create:
        data = request("POST", f"/projects/{args.project_id}/goals", {"title": args.create, "description": args.description})
        print(f"Создана цель #{data['id']} «{data['title']}»")
        return
    if args.link:
        request("POST", f"/projects/{args.project_id}/goals/{args.goal}/tasks/{args.link}")
        print(f"Задача #{args.link} привязана к цели #{args.goal}")
        return
    for g in request("GET", f"/projects/{args.project_id}/goals") or []:
        prog = g.get("progress")
        prog_s = f"  {int(prog * 100)}%" if isinstance(prog, (int, float)) else ""
        print(f"#{g['id']:>3}  {g['title']:<28}{prog_s}")


def cmd_risks(args: argparse.Namespace) -> None:
    if args.create:
        data = request("POST", f"/projects/{args.project_id}/risks", {
            "title": args.create,
            "taskId": args.task,
            "description": args.description,
        })
        print(f"Создан риск #{data['id']} «{data['title']}»")
        return
    if args.resolve is not None:
        request("PUT", f"/projects/{args.project_id}/risks/{args.resolve}", {"status": "RESOLVED", "title": args.title or "Risk"})
        print(f"Риск #{args.resolve} отмечен как решён")
        return
    for r in request("GET", f"/projects/{args.project_id}/risks") or []:
        status = human_status(r.get("status"))
        task = f"  задача #{r['taskId']}" if r.get("taskId") else ""
        print(f"#{r['id']:>3}  {status:<10}  {r['title']}{task}")


def cmd_approve(args: argparse.Namespace) -> None:
    if args.request:
        data = request("POST", f"/tasks/{args.task_id}/approvals", {"approverId": args.request, "comment": args.comment})
        print(f"Запрошено согласование #{data['id']} для задачи #{args.task_id}")
        return
    if args.accept is not None:
        request("POST", f"/tasks/{args.task_id}/approvals/{args.accept}/approve", {"comment": args.comment})
        print(f"Согласование #{args.accept} одобрено")
        return
    if args.reject is not None:
        request("POST", f"/tasks/{args.task_id}/approvals/{args.reject}/reject", {"comment": args.comment})
        print(f"Согласование #{args.reject} отклонено")
        return
    for a in request("GET", f"/tasks/{args.task_id}/approvals") or []:
        status = human_status(a.get("status"))
        print(f"#{a['id']:>3}  {status:<10}  согласующий #{a.get('approverId')}")


def cmd_time_report(args: argparse.Namespace) -> None:
    fmt = args.format or "json"
    data = request("GET", f"/projects/{args.project_id}/time-report?format={fmt}")
    if isinstance(data, str):
        print(data)
    else:
        pp(data)


def cmd_boards(args: argparse.Namespace) -> None:
    if args.create:
        body = {"name": args.create, "accessMode": args.access_mode or "OPEN"}
        if args.team_ids:
            body["teamIds"] = [int(x) for x in args.team_ids.split(",") if x.strip()]
        data = request("POST", f"/projects/{args.project_id}/boards", body)
        print(f"Создана доска #{data['id']} «{data['name']}»")
        return
    if args.set_access:
        boards = request("GET", f"/projects/{args.project_id}/boards") or []
        board = next((b for b in boards if b["id"] == args.set_access), None)
        if not board:
            raise SystemExit(f"Board #{args.set_access} not found")
        body = {
            "name": board["name"],
            "accessMode": args.access_mode or board.get("accessMode") or "OPEN",
        }
        if args.team_ids:
            body["teamIds"] = [int(x) for x in args.team_ids.split(",") if x.strip()]
        request("PUT", f"/projects/{args.project_id}/boards/{args.set_access}", body)
        mode = ACCESS_RU.get(body["accessMode"], body["accessMode"].lower())
        print(f"Доступ доски #{args.set_access} обновлён ({mode})")
        return
    for b in request("GET", f"/projects/{args.project_id}/boards") or []:
        mode = ACCESS_RU.get(b.get("accessMode", "OPEN"), b.get("accessMode", "OPEN").lower())
        teams = b.get("teamIds") or []
        suffix = f"  teams={teams}" if teams else ""
        print(f"#{b['id']:>3}  {mode:<10}  {b['name']}{suffix}")


def cmd_suggest_dod(args: argparse.Namespace) -> None:
    data = request("POST", f"/tasks/{args.task_id}/ai/suggest-dod")
    if args.apply:
        items = [{"title": i["title"], "done": False} for i in data.get("items", [])]
        created = request("POST", f"/tasks/{args.task_id}/checklist/bulk", items)
        count = len(created) if isinstance(created, list) else len(items)
        print(f"Добавлено {count} пунктов чек-листа к задаче #{args.task_id}")
        return
    for i in data.get("items", []):
        print(f"{i['position']}. {i['title']}")
    print("(preview only — pass --apply to create checklist items)")


def cmd_columns(args: argparse.Namespace) -> None:
    if args.create:
        body = {"name": args.create, "wipLimit": args.wip_limit, "mappedStatus": args.mapped_status}
        data = request("POST", f"/boards/{args.board_id}/columns", body)
        print(f"Создана колонка #{data['id']} «{data['name']}»")
        return
    for c in request("GET", f"/boards/{args.board_id}/columns") or []:
        extras = []
        if c.get("wipLimit") is not None:
            extras.append(f"wip={c['wipLimit']}")
        if c.get("mappedStatus"):
            extras.append(human_status(c["mappedStatus"]))
        suffix = f"  {' · '.join(extras)}" if extras else ""
        print(f"#{c['id']:>3}  {c['name']:<20}{suffix}")


def cmd_tasks(args: argparse.Namespace) -> None:
    if args.create:
        body = {
            "title": args.create,
            "description": args.description,
            "priority": args.priority,
            "deadline": args.deadline,
            "assigneeId": args.assignee,
            "status": args.status,
            "estimateHours": args.estimate,
            "spentHours": args.spent,
            "taskType": args.type,
            "sprintId": args.sprint,
            "labelIds": None,
        }
        data = request("POST", f"/columns/{args.column_id}/tasks", body)
        print(f"Создана задача #{data['id']} «{data['title']}»")
        return
    qs = urllib.parse.urlencode({"size": args.size})
    data = request("GET", f"/columns/{args.column_id}/tasks?{qs}")
    for t in data.get("content", []):
        overdue = " просрочена" if t.get("overdue") else ""
        who = f" @{t['assigneeName']}" if t.get("assigneeName") else ""
        pri = human_priority(t.get("priority"))
        status = human_status(t.get("status"))
        print(f"#{t['id']:>3}  [{pri:<8}]  {status:<10}  {t['title']}{who}{overdue}")


def cmd_task(args: argparse.Namespace) -> None:
    if args.update:
        current = request("GET", f"/tasks/{args.task_id}")
        body = {
            "title": args.title or current["title"],
            "description": args.description if args.description is not None else current.get("description"),
            "priority": args.priority or current["priority"],
            "status": args.status or current.get("status"),
            "deadline": args.deadline if args.deadline is not None else current.get("deadline"),
            "assigneeId": args.assignee if args.assignee is not None else current.get("assigneeId"),
            "estimateHours": args.estimate if args.estimate is not None else current.get("estimateHours"),
            "spentHours": args.spent if args.spent is not None else current.get("spentHours"),
            "taskType": args.type if args.type is not None else current.get("taskType"),
            "sprintId": args.sprint if args.sprint is not None else current.get("sprintId"),
            "labelIds": None,
        }
        data = request("PUT", f"/tasks/{args.task_id}", body)
        if emit_json(args, data):
            return
        print(f"Обновлена задача #{data['id']} «{data['title']}»")
        return

    data = request("GET", f"/tasks/{args.task_id}")
    if emit_json(args, data):
        return
    print(format_task_summary(data))


def cmd_members(args: argparse.Namespace) -> None:
    for m in request("GET", f"/projects/{args.project_id}/members") or []:
        role = human_role(m.get("role"))
        print(f"#{m['userId']:>3}  {role:<10}  {m['name']}  <{m['email']}>")


def cmd_my_invites(_: argparse.Namespace) -> None:
    for inv in request("GET", "/users/me/invitations") or []:
        status = human_status(inv.get("status"))
        role = human_role(inv.get("role"))
        print(f"#{inv['id']:>3}  {status:<10}  «{inv.get('projectName')}»  {role}")


def cmd_search(args: argparse.Namespace) -> None:
    params = {k: v for k, v in {
        "q": args.q,
        "status": args.status,
        "priority": args.priority,
        "type": args.type,
        "size": args.size,
    }.items() if v is not None}
    qs = urllib.parse.urlencode(params)
    data = request("GET", f"/projects/{args.project_id}/tasks?{qs}")
    for t in data.get("content", []):
        status = human_status(t.get("status"))
        print(f"#{t['id']:>3}  col #{t['columnId']}  {status:<10}  {t['title']}")


def cmd_move(args: argparse.Namespace) -> None:
    request("PATCH", f"/tasks/{args.task_id}/move", {"columnId": args.column_id})
    print(f"Задача #{args.task_id} перенесена в колонку #{args.column_id}")


def cmd_invite(args: argparse.Namespace) -> None:
    data = request("POST", f"/projects/{args.project_id}/invitations", {"email": args.email, "role": args.role})
    role = human_role(data.get("role", args.role))
    print(f"Приглашение #{data['id']} отправлено на {args.email} ({role})")


def cmd_accept_invite(args: argparse.Namespace) -> None:
    request("POST", f"/invitations/{args.invitation_id}/accept")
    print(f"Приглашение #{args.invitation_id} принято")


def cmd_decline_invite(args: argparse.Namespace) -> None:
    request("POST", f"/invitations/{args.invitation_id}/decline")
    print(f"Приглашение #{args.invitation_id} отклонено")


def cmd_transfer(args: argparse.Namespace) -> None:
    request("POST", f"/projects/{args.project_id}/transfer-ownership", {"newOwnerUserId": args.user_id})
    print(f"Владение проектом #{args.project_id} передано пользователю #{args.user_id}")


def cmd_comments(args: argparse.Namespace) -> None:
    if args.create:
        data = request("POST", f"/tasks/{args.task_id}/comments", {"body": args.create})
        print(f"Комментарий #{data['id']} добавлен к задаче #{args.task_id}")
        return
    for c in request("GET", f"/tasks/{args.task_id}/comments") or []:
        print(f"#{c['id']:>3}  {c['authorName']}: {c['body']}")


def cmd_checklist(args: argparse.Namespace) -> None:
    if args.create:
        data = request("POST", f"/tasks/{args.task_id}/checklist", {"title": args.create, "done": False})
        print(f"Пункт чек-листа #{data['id']} добавлен")
        return
    if args.done is not None:
        items = request("GET", f"/tasks/{args.task_id}/checklist") or []
        item = next((i for i in items if i["id"] == args.done), None)
        title = args.title or (item["title"] if item else None)
        if not title:
            raise SystemExit(f"Checklist item #{args.done} not found; pass --title")
        request("PUT", f"/tasks/checklist/{args.done}", {"title": title, "done": True})
        print(f"Пункт чек-листа #{args.done} отмечен выполненным")
        return
    for i in request("GET", f"/tasks/{args.task_id}/checklist") or []:
        mark = "x" if i["done"] else " "
        print(f"[{mark}] #{i['id']}  {i['title']}")


def cmd_deps(args: argparse.Namespace) -> None:
    if args.add:
        request("POST", f"/tasks/{args.task_id}/dependencies", {"blockerId": args.add})
        print(f"Задача #{args.task_id} заблокирована задачей #{args.add}")
        return
    for d in request("GET", f"/tasks/{args.task_id}/dependencies") or []:
        status = human_status(d.get("blockerStatus"))
        print(f"блокирует #{d['blockerId']}  {d['blockerTitle']} ({status})")


def cmd_labels(args: argparse.Namespace) -> None:
    if args.create:
        data = request("POST", f"/projects/{args.project_id}/labels", {"name": args.create, "color": args.color or "#3ecf8e"})
        print(f"Создана метка #{data['id']} «{data['name']}»")
        return
    for lab in request("GET", f"/projects/{args.project_id}/labels") or []:
        print(f"#{lab['id']:>3}  {lab['color']}  {lab['name']}")


def cmd_sprints(args: argparse.Namespace) -> None:
    if args.create:
        data = request("POST", f"/projects/{args.project_id}/sprints", {"name": args.create})
        print(f"Создан спринт #{data['id']} «{data['name']}»")
        return
    if args.close:
        request(
            "POST",
            f"/projects/{args.project_id}/sprints/{args.close}/close",
            {"unfinishedAction": args.unfinished_action or "MOVE_TO_BACKLOG"},
        )
        print(f"Спринт #{args.close} закрыт")
        return
    for s in request("GET", f"/projects/{args.project_id}/sprints") or []:
        status = human_status(s.get("status"))
        print(f"#{s['id']:>3}  {status:<10}  {s['name']}")


def cmd_notifications(args: argparse.Namespace) -> None:
    if args.read is not None:
        request("PUT", f"/users/me/notifications/{args.read}/read")
        print(f"Уведомление #{args.read} прочитано")
        return
    for n in request("GET", "/users/me/notifications") or []:
        flag = " " if n["read"] else "*"
        print(f"{flag} #{n['id']:>3}  [{n['type']}]  {n['message']}")


def cmd_activity(args: argparse.Namespace) -> None:
    data = request("GET", f"/projects/{args.project_id}/activity?size={args.size}")
    for a in data.get("content", []):
        who = a.get("actorName") or "?"
        print(f"{a['createdAt']}  {who}: {a['action']} — {a.get('details')}")


def cmd_health(args: argparse.Namespace) -> None:
    data = request("GET", "/health", token="")
    if emit_json(args, data):
        return
    status = (data or {}).get("status", "UP")
    if str(status).upper() in ("UP", "OK"):
        print("OK")
    else:
        print(f"status: {status}")


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="tm",
        description="CLI-клиент Task Manager API",
        epilog=EPILOG,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    sub = p.add_subparsers(dest="command", required=True, metavar="команда")

    c = sub.add_parser("config", help="показать или задать адрес API")
    c.add_argument("--base-url", help="базовый URL API (например http://localhost:8080/api/v1)")
    c.set_defaults(func=cmd_config)

    c = sub.add_parser("health", help="проверить доступность API")
    add_json_flag(c)
    c.set_defaults(func=cmd_health)

    c = sub.add_parser("register", help="регистрация и вход")
    c.add_argument("--email", required=True, help="email пользователя")
    c.add_argument("--password", required=True, help="пароль")
    c.add_argument("--name", required=True, help="отображаемое имя")
    c.set_defaults(func=cmd_register)

    c = sub.add_parser("login", help="вход по email и паролю")
    c.add_argument("--email", required=True, help="email пользователя")
    c.add_argument("--password", required=True, help="пароль")
    c.set_defaults(func=cmd_login)

    c = sub.add_parser("whoami", help="текущий пользователь")
    add_json_flag(c)
    c.set_defaults(func=cmd_whoami)

    c = sub.add_parser("projects", help="список или создание проектов")
    c.add_argument("--create", help="название нового проекта")
    c.add_argument("--description", default=None, help="описание проекта")
    c.add_argument("--strict", action="store_true", help="строгие бизнес-правила")
    c.add_argument("--with-default-board", action="store_true", help="создать доску по умолчанию")
    c.add_argument("--template", choices=["NONE", "KANBAN", "SCRUM", "BUG_TRIAGE", "PERSONAL"], default="NONE", help="шаблон проекта")
    c.add_argument("--org", type=int, help="id организации для проекта")
    add_json_flag(c)
    c.set_defaults(func=cmd_projects)

    c = sub.add_parser("orgs", help="организации и участники")
    c.add_argument("--create", help="название новой организации")
    c.add_argument("--type", choices=["LOCAL", "COMMERCIAL"], default="LOCAL", help="тип организации")
    c.add_argument("org_id", nargs="?", type=int, help="id организации для просмотра")
    c.add_argument("--add-member", help="email участника для добавления")
    c.add_argument("--role", choices=["ADMIN", "MEMBER", "VIEWER"], default="MEMBER", help="роль участника")
    add_json_flag(c)
    c.set_defaults(func=cmd_orgs)

    c = sub.add_parser("teams", help="команды в организации")
    c.add_argument("org_id", type=int, help="id организации")
    c.add_argument("--create", help="название новой команды")
    c.add_argument("--team-id", type=int, help="id команды")
    c.add_argument("--add-member", help="email участника для добавления в команду")
    c.set_defaults(func=cmd_teams)

    c = sub.add_parser("settings", help="настройки проекта")
    c.add_argument("project_id", type=int, help="id проекта")
    c.add_argument("--capacity", type=float, help="лимит ёмкости в часах")
    c.add_argument("--priority-queue", action="store_true", help="включить правила очереди приоритетов")
    c.add_argument("--require-approval", action="store_true", help="требовать согласование для DONE")
    c.add_argument("--sla-warning", type=int, help="дней до предупреждения SLA")
    c.add_argument("--auto-archive", type=int, help="автоархивация DONE через N дней")
    c.add_argument("--dod", action="store_true", help="включить шаблоны DoD")
    c.set_defaults(func=cmd_settings)

    c = sub.add_parser("goals", help="цели проекта")
    c.add_argument("project_id", type=int, help="id проекта")
    c.add_argument("--create", help="название новой цели")
    c.add_argument("--description", help="описание цели")
    c.add_argument("--goal", type=int, help="id цели для привязки задачи")
    c.add_argument("--link", type=int, help="id задачи для привязки к цели")
    c.set_defaults(func=cmd_goals)

    c = sub.add_parser("risks", help="риски проекта")
    c.add_argument("project_id", type=int, help="id проекта")
    c.add_argument("--create", help="название нового риска")
    c.add_argument("--description", help="описание риска")
    c.add_argument("--task", type=int, help="id связанной задачи")
    c.add_argument("--resolve", type=int, help="id риска для закрытия")
    c.add_argument("--title", help="заголовок при закрытии риска")
    c.set_defaults(func=cmd_risks)

    c = sub.add_parser("approve", help="согласования задачи")
    c.add_argument("task_id", type=int, help="id задачи")
    c.add_argument("--request", type=int, help="id согласующего пользователя")
    c.add_argument("--accept", type=int, help="id согласования для одобрения")
    c.add_argument("--reject", type=int, help="id согласования для отклонения")
    c.add_argument("--comment", help="комментарий к согласованию")
    c.set_defaults(func=cmd_approve)

    c = sub.add_parser("time-report", help="отчёт по времени проекта")
    c.add_argument("project_id", type=int, help="id проекта")
    c.add_argument("--format", choices=["json", "csv"], default="json", help="формат отчёта")
    c.set_defaults(func=cmd_time_report)

    c = sub.add_parser("boards", help="доски проекта")
    c.add_argument("project_id", type=int, help="id проекта")
    c.add_argument("--create", help="название новой доски")
    c.add_argument("--access-mode", choices=["OPEN", "PRIVATE", "TEAM_ACL"], help="режим доступа")
    c.add_argument("--team-ids", help="id команд через запятую для TEAM_ACL")
    c.add_argument("--set-access", type=int, help="id доски для смены доступа")
    c.set_defaults(func=cmd_boards)

    c = sub.add_parser("suggest-dod", help="AI-подсказка Definition of Done")
    c.add_argument("task_id", type=int, help="id задачи")
    c.add_argument("--apply", action="store_true", help="создать пункты чек-листа из подсказки")
    c.set_defaults(func=cmd_suggest_dod)

    c = sub.add_parser("columns", help="колонки доски")
    c.add_argument("board_id", type=int, help="id доски")
    c.add_argument("--create", help="название новой колонки")
    c.add_argument("--wip-limit", type=int, help="WIP-лимит колонки")
    c.add_argument("--mapped-status", choices=["BACKLOG", "IN_PROGRESS", "DONE", "ARCHIVED"], help="сопоставленный статус")
    c.set_defaults(func=cmd_columns)

    c = sub.add_parser("tasks", help="задачи в колонке")
    c.add_argument("column_id", type=int, help="id колонки")
    c.add_argument("--create", help="заголовок новой задачи")
    c.add_argument("--description", help="описание задачи")
    c.add_argument("--priority", default="MEDIUM", choices=["LOW", "MEDIUM", "HIGH"], help="приоритет")
    c.add_argument("--deadline", help="дедлайн (ISO дата)")
    c.add_argument("--estimate", type=float, help="оценка в часах")
    c.add_argument("--spent", type=float, help="затрачено часов")
    c.add_argument("--type", choices=["BUG", "FEATURE", "CHORE"], help="тип задачи")
    c.add_argument("--size", type=int, default=20, help="размер страницы списка")
    c.add_argument("--assignee", type=int, help="id исполнителя")
    c.add_argument("--status", choices=["BACKLOG", "IN_PROGRESS", "DONE", "ARCHIVED"], help="статус")
    c.add_argument("--sprint", type=int, help="id спринта")
    c.set_defaults(func=cmd_tasks)

    c = sub.add_parser("task", help="просмотр или обновление задачи")
    c.add_argument("task_id", type=int, help="id задачи")
    c.add_argument("--update", action="store_true", help="обновить задачу")
    c.add_argument("--title", help="новый заголовок")
    c.add_argument("--description", help="новое описание")
    c.add_argument("--priority", choices=["LOW", "MEDIUM", "HIGH"], help="приоритет")
    c.add_argument("--deadline", help="дедлайн (ISO дата)")
    c.add_argument("--estimate", type=float, help="оценка в часах")
    c.add_argument("--spent", type=float, help="затрачено часов")
    c.add_argument("--type", choices=["BUG", "FEATURE", "CHORE"], help="тип задачи")
    c.add_argument("--assignee", type=int, help="id исполнителя")
    c.add_argument("--status", choices=["BACKLOG", "IN_PROGRESS", "DONE", "ARCHIVED"], help="статус")
    c.add_argument("--sprint", type=int, help="id спринта")
    add_json_flag(c)
    c.set_defaults(func=cmd_task)

    c = sub.add_parser("members", help="участники проекта")
    c.add_argument("project_id", type=int, help="id проекта")
    c.set_defaults(func=cmd_members)

    c = sub.add_parser("my-invites", help="мои приглашения в проекты")
    c.set_defaults(func=cmd_my_invites)

    c = sub.add_parser("search", help="поиск задач в проекте")
    c.add_argument("project_id", type=int, help="id проекта")
    c.add_argument("--q", help="поисковый запрос")
    c.add_argument("--status", help="фильтр по статусу")
    c.add_argument("--priority", help="фильтр по приоритету")
    c.add_argument("--type", help="фильтр по типу")
    c.add_argument("--size", type=int, default=20, help="размер страницы")
    c.set_defaults(func=cmd_search)

    c = sub.add_parser("move", help="переместить задачу в колонку")
    c.add_argument("task_id", type=int, help="id задачи")
    c.add_argument("column_id", type=int, help="id целевой колонки")
    c.set_defaults(func=cmd_move)

    c = sub.add_parser("invite", help="пригласить участника в проект")
    c.add_argument("project_id", type=int, help="id проекта")
    c.add_argument("--email", required=True, help="email приглашённого")
    c.add_argument("--role", default="EDITOR", choices=["EDITOR", "VIEWER", "CONTRACTOR"], help="роль в проекте")
    c.set_defaults(func=cmd_invite)

    c = sub.add_parser("accept-invite", help="принять приглашение")
    c.add_argument("invitation_id", type=int, help="id приглашения")
    c.set_defaults(func=cmd_accept_invite)

    c = sub.add_parser("decline-invite", help="отклонить приглашение")
    c.add_argument("invitation_id", type=int, help="id приглашения")
    c.set_defaults(func=cmd_decline_invite)

    c = sub.add_parser("transfer", help="передать владение проектом")
    c.add_argument("project_id", type=int, help="id проекта")
    c.add_argument("user_id", type=int, help="id нового владельца")
    c.set_defaults(func=cmd_transfer)

    c = sub.add_parser("comments", help="комментарии к задаче")
    c.add_argument("task_id", type=int, help="id задачи")
    c.add_argument("--create", help="текст нового комментария")
    c.set_defaults(func=cmd_comments)

    c = sub.add_parser("checklist", help="чек-лист задачи")
    c.add_argument("task_id", type=int, help="id задачи")
    c.add_argument("--create", help="название нового пункта")
    c.add_argument("--done", type=int, help="id пункта для отметки выполненным")
    c.add_argument("--title", help="заголовок при отметке пункта")
    c.set_defaults(func=cmd_checklist)

    c = sub.add_parser("deps", help="зависимости задачи")
    c.add_argument("task_id", type=int, help="id задачи")
    c.add_argument("--add", type=int, help="id блокирующей задачи")
    c.set_defaults(func=cmd_deps)

    c = sub.add_parser("labels", help="метки проекта")
    c.add_argument("project_id", type=int, help="id проекта")
    c.add_argument("--create", help="название новой метки")
    c.add_argument("--color", help="цвет метки (hex)")
    c.set_defaults(func=cmd_labels)

    c = sub.add_parser("sprints", help="спринты проекта")
    c.add_argument("project_id", type=int, help="id проекта")
    c.add_argument("--create", help="название нового спринта")
    c.add_argument("--close", type=int, help="id спринта для закрытия")
    c.add_argument("--unfinished-action", choices=["ARCHIVE", "MOVE_TO_BACKLOG"], help="действие с незавершёнными задачами")
    c.set_defaults(func=cmd_sprints)

    c = sub.add_parser("notifications", help="уведомления пользователя")
    c.add_argument("--read", type=int, help="id уведомления для отметки прочитанным")
    c.set_defaults(func=cmd_notifications)

    c = sub.add_parser("activity", help="лента активности проекта")
    c.add_argument("project_id", type=int, help="id проекта")
    c.add_argument("--size", type=int, default=20, help="размер страницы")
    c.set_defaults(func=cmd_activity)

    return p


def main(argv: list[str] | None = None) -> None:
    parser = build_parser()
    args = parser.parse_args(argv)
    args.func(args)


if __name__ == "__main__":
    main()
