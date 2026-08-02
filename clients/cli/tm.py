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


def load_config() -> dict:
    if CONFIG_PATH.exists():
        return json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
    return {"base_url": DEFAULT_BASE, "token": None}


def save_config(cfg: dict) -> None:
    CONFIG_PATH.write_text(json.dumps(cfg, indent=2), encoding="utf-8")


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


def cmd_config(args: argparse.Namespace) -> None:
    cfg = load_config()
    if args.base_url:
        cfg["base_url"] = args.base_url.rstrip("/")
        save_config(cfg)
        print(f"API base URL -> {cfg['base_url']}")
    else:
        pp(cfg)


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


def cmd_whoami(_: argparse.Namespace) -> None:
    pp(request("GET", "/users/me"))


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
        pp(request("POST", "/projects", body))
        return
    data = request("GET", "/projects")
    if not data:
        print("No projects")
        return
    for p in data:
        strict = "strict" if p.get("strictBusinessRules") else "legacy"
        org = f"  org=#{p['organizationId']}" if p.get("organizationId") else ""
        print(f"#{p['id']:>3}  {p['name']:<28}  role={p['currentUserRole']:<6}  {strict}{org}")


def cmd_orgs(args: argparse.Namespace) -> None:
    if args.create:
        pp(request("POST", "/organizations", {"name": args.create, "type": args.type or "LOCAL"}))
        return
    if args.add_member:
        pp(request(
            "POST",
            f"/organizations/{args.org_id}/members",
            {"email": args.add_member, "role": args.role or "MEMBER"},
        ))
        return
    if args.org_id:
        pp(request("GET", f"/organizations/{args.org_id}"))
        return
    for o in request("GET", "/organizations") or []:
        print(f"#{o['id']:>3}  {o['type']:<10}  {o['name']}  role={o.get('currentUserRole')}")


def cmd_teams(args: argparse.Namespace) -> None:
    if args.create:
        pp(request("POST", f"/organizations/{args.org_id}/teams", {"name": args.create}))
        return
    if args.add_member:
        pp(request(
            "POST",
            f"/organizations/{args.org_id}/teams/{args.team_id}/members",
            {"email": args.add_member},
        ))
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
    pp(request("PATCH", f"/projects/{args.project_id}/settings", body))


def cmd_goals(args: argparse.Namespace) -> None:
    if args.create:
        pp(request("POST", f"/projects/{args.project_id}/goals", {"title": args.create, "description": args.description}))
        return
    if args.link:
        pp(request("POST", f"/projects/{args.project_id}/goals/{args.goal}/tasks/{args.link}"))
        return
    for g in request("GET", f"/projects/{args.project_id}/goals") or []:
        prog = g.get("progress")
        print(f"#{g['id']:>3}  {g['title']:<28}  progress={prog}")


def cmd_risks(args: argparse.Namespace) -> None:
    if args.create:
        pp(request("POST", f"/projects/{args.project_id}/risks", {
            "title": args.create,
            "taskId": args.task,
            "description": args.description,
        }))
        return
    if args.resolve is not None:
        pp(request("PUT", f"/projects/{args.project_id}/risks/{args.resolve}", {"status": "RESOLVED", "title": args.title or "Risk"}))
        return
    for r in request("GET", f"/projects/{args.project_id}/risks") or []:
        print(f"#{r['id']:>3}  {r['status']:<8}  {r['title']}  task={r.get('taskId')}")


def cmd_approve(args: argparse.Namespace) -> None:
    if args.request:
        pp(request("POST", f"/tasks/{args.task_id}/approvals", {"approverId": args.request, "comment": args.comment}))
        return
    if args.accept is not None:
        pp(request("POST", f"/tasks/{args.task_id}/approvals/{args.accept}/approve", {"comment": args.comment}))
        return
    if args.reject is not None:
        pp(request("POST", f"/tasks/{args.task_id}/approvals/{args.reject}/reject", {"comment": args.comment}))
        return
    for a in request("GET", f"/tasks/{args.task_id}/approvals") or []:
        print(f"#{a['id']:>3}  {a['status']:<10}  approver={a.get('approverId')}")


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
        pp(request("POST", f"/projects/{args.project_id}/boards", body))
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
        pp(request("PUT", f"/projects/{args.project_id}/boards/{args.set_access}", body))
        return
    for b in request("GET", f"/projects/{args.project_id}/boards") or []:
        teams = b.get("teamIds") or []
        print(f"#{b['id']:>3}  {b.get('accessMode', 'OPEN'):<8}  {b['name']}  teams={teams}")


def cmd_suggest_dod(args: argparse.Namespace) -> None:
    data = request("POST", f"/tasks/{args.task_id}/ai/suggest-dod")
    if args.apply:
        items = [{"title": i["title"], "done": False} for i in data.get("items", [])]
        pp(request("POST", f"/tasks/{args.task_id}/checklist/bulk", items))
        return
    for i in data.get("items", []):
        print(f"{i['position']}. {i['title']}")
    print("(preview only — pass --apply to create checklist items)")


def cmd_columns(args: argparse.Namespace) -> None:
    if args.create:
        body = {"name": args.create, "wipLimit": args.wip_limit, "mappedStatus": args.mapped_status}
        pp(request("POST", f"/boards/{args.board_id}/columns", body))
        return
    for c in request("GET", f"/boards/{args.board_id}/columns") or []:
        print(
            f"#{c['id']:>3}  pos={c['position']}  {c['name']:<20}  "
            f"wip={c.get('wipLimit')}  map={c.get('mappedStatus')}"
        )


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
        pp(request("POST", f"/columns/{args.column_id}/tasks", body))
        return
    qs = urllib.parse.urlencode({"size": args.size})
    data = request("GET", f"/columns/{args.column_id}/tasks?{qs}")
    for t in data.get("content", []):
        overdue = " OVERDUE" if t.get("overdue") else ""
        who = f" @{t['assigneeName']}" if t.get("assigneeName") else ""
        print(f"#{t['id']:>3}  [{t['priority']:<6}]  {t['status']:<12}  {t['title']}{who}{overdue}")


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
        pp(request("PUT", f"/tasks/{args.task_id}", body))
        return
    pp(request("GET", f"/tasks/{args.task_id}"))


def cmd_members(args: argparse.Namespace) -> None:
    for m in request("GET", f"/projects/{args.project_id}/members") or []:
        print(f"#{m['userId']:>3}  {m['role']:<6}  {m['name']}  <{m['email']}>")


def cmd_my_invites(_: argparse.Namespace) -> None:
    for inv in request("GET", "/users/me/invitations") or []:
        print(f"#{inv['id']:>3}  {inv['status']:<10}  project=#{inv['projectId']} {inv.get('projectName')} → {inv['role']}")


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
        print(f"#{t['id']:>3}  col={t['columnId']}  {t['status']:<12}  {t['title']}")


def cmd_move(args: argparse.Namespace) -> None:
    pp(request("PATCH", f"/tasks/{args.task_id}/move", {"columnId": args.column_id}))


def cmd_invite(args: argparse.Namespace) -> None:
    pp(request("POST", f"/projects/{args.project_id}/invitations", {"email": args.email, "role": args.role}))


def cmd_accept_invite(args: argparse.Namespace) -> None:
    pp(request("POST", f"/invitations/{args.invitation_id}/accept"))


def cmd_decline_invite(args: argparse.Namespace) -> None:
    pp(request("POST", f"/invitations/{args.invitation_id}/decline"))


def cmd_transfer(args: argparse.Namespace) -> None:
    pp(request("POST", f"/projects/{args.project_id}/transfer-ownership", {"newOwnerUserId": args.user_id}))


def cmd_comments(args: argparse.Namespace) -> None:
    if args.create:
        pp(request("POST", f"/tasks/{args.task_id}/comments", {"body": args.create}))
        return
    for c in request("GET", f"/tasks/{args.task_id}/comments") or []:
        print(f"#{c['id']:>3}  {c['authorName']}: {c['body']}")


def cmd_checklist(args: argparse.Namespace) -> None:
    if args.create:
        pp(request("POST", f"/tasks/{args.task_id}/checklist", {"title": args.create, "done": False}))
        return
    if args.done is not None:
        items = request("GET", f"/tasks/{args.task_id}/checklist") or []
        item = next((i for i in items if i["id"] == args.done), None)
        title = args.title or (item["title"] if item else None)
        if not title:
            raise SystemExit(f"Checklist item #{args.done} not found; pass --title")
        pp(request("PUT", f"/tasks/checklist/{args.done}", {"title": title, "done": True}))
        return
    for i in request("GET", f"/tasks/{args.task_id}/checklist") or []:
        mark = "x" if i["done"] else " "
        print(f"[{mark}] #{i['id']}  {i['title']}")


def cmd_deps(args: argparse.Namespace) -> None:
    if args.add:
        pp(request("POST", f"/tasks/{args.task_id}/dependencies", {"blockerId": args.add}))
        return
    for d in request("GET", f"/tasks/{args.task_id}/dependencies") or []:
        print(f"blocked by #{d['blockerId']}  {d['blockerTitle']} ({d['blockerStatus']})")


def cmd_labels(args: argparse.Namespace) -> None:
    if args.create:
        pp(request("POST", f"/projects/{args.project_id}/labels", {"name": args.create, "color": args.color or "#3ecf8e"}))
        return
    for lab in request("GET", f"/projects/{args.project_id}/labels") or []:
        print(f"#{lab['id']:>3}  {lab['color']}  {lab['name']}")


def cmd_sprints(args: argparse.Namespace) -> None:
    if args.create:
        pp(request("POST", f"/projects/{args.project_id}/sprints", {"name": args.create}))
        return
    if args.close:
        pp(request(
            "POST",
            f"/projects/{args.project_id}/sprints/{args.close}/close",
            {"unfinishedAction": args.unfinished_action or "MOVE_TO_BACKLOG"},
        ))
        return
    for s in request("GET", f"/projects/{args.project_id}/sprints") or []:
        print(f"#{s['id']:>3}  {s['status']:<8}  {s['name']}")


def cmd_notifications(args: argparse.Namespace) -> None:
    if args.read is not None:
        pp(request("PUT", f"/users/me/notifications/{args.read}/read"))
        return
    for n in request("GET", "/users/me/notifications") or []:
        flag = " " if n["read"] else "*"
        print(f"{flag} #{n['id']:>3}  [{n['type']}]  {n['message']}")


def cmd_activity(args: argparse.Namespace) -> None:
    data = request("GET", f"/projects/{args.project_id}/activity?size={args.size}")
    for a in data.get("content", []):
        who = a.get("actorName") or "?"
        print(f"{a['createdAt']}  {who}: {a['action']} — {a.get('details')}")


def cmd_health(_: argparse.Namespace) -> None:
    pp(request("GET", "/health", token=""))


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="tm", description="CLI client for Task Manager API")
    sub = p.add_subparsers(dest="command", required=True)

    c = sub.add_parser("config"); c.add_argument("--base-url"); c.set_defaults(func=cmd_config)
    c = sub.add_parser("health"); c.set_defaults(func=cmd_health)
    c = sub.add_parser("register")
    c.add_argument("--email", required=True); c.add_argument("--password", required=True); c.add_argument("--name", required=True)
    c.set_defaults(func=cmd_register)
    c = sub.add_parser("login")
    c.add_argument("--email", required=True); c.add_argument("--password", required=True)
    c.set_defaults(func=cmd_login)
    c = sub.add_parser("whoami"); c.set_defaults(func=cmd_whoami)

    c = sub.add_parser("projects")
    c.add_argument("--create"); c.add_argument("--description", default=None)
    c.add_argument("--strict", action="store_true"); c.add_argument("--with-default-board", action="store_true")
    c.add_argument("--template", choices=["NONE", "KANBAN", "SCRUM", "BUG_TRIAGE", "PERSONAL"], default="NONE")
    c.add_argument("--org", type=int, help="organization id for org-linked project")
    c.set_defaults(func=cmd_projects)

    c = sub.add_parser("orgs")
    c.add_argument("--create"); c.add_argument("--type", choices=["LOCAL", "COMMERCIAL"], default="LOCAL")
    c.add_argument("org_id", nargs="?", type=int); c.add_argument("--add-member")
    c.add_argument("--role", choices=["ADMIN", "MEMBER", "VIEWER"], default="MEMBER")
    c.set_defaults(func=cmd_orgs)

    c = sub.add_parser("teams")
    c.add_argument("org_id", type=int); c.add_argument("--create"); c.add_argument("--team-id", type=int)
    c.add_argument("--add-member"); c.set_defaults(func=cmd_teams)

    c = sub.add_parser("settings"); c.add_argument("project_id", type=int)
    c.add_argument("--capacity", type=float); c.add_argument("--priority-queue", action="store_true")
    c.add_argument("--require-approval", action="store_true"); c.add_argument("--sla-warning", type=int)
    c.add_argument("--auto-archive", type=int); c.add_argument("--dod", action="store_true")
    c.set_defaults(func=cmd_settings)

    c = sub.add_parser("goals"); c.add_argument("project_id", type=int); c.add_argument("--create")
    c.add_argument("--description"); c.add_argument("--goal", type=int); c.add_argument("--link", type=int, help="task id")
    c.set_defaults(func=cmd_goals)

    c = sub.add_parser("risks"); c.add_argument("project_id", type=int); c.add_argument("--create")
    c.add_argument("--description"); c.add_argument("--task", type=int); c.add_argument("--resolve", type=int)
    c.add_argument("--title"); c.set_defaults(func=cmd_risks)

    c = sub.add_parser("approve"); c.add_argument("task_id", type=int)
    c.add_argument("--request", type=int, help="approver user id"); c.add_argument("--accept", type=int)
    c.add_argument("--reject", type=int); c.add_argument("--comment"); c.set_defaults(func=cmd_approve)

    c = sub.add_parser("time-report"); c.add_argument("project_id", type=int)
    c.add_argument("--format", choices=["json", "csv"], default="json"); c.set_defaults(func=cmd_time_report)

    c = sub.add_parser("boards"); c.add_argument("project_id", type=int); c.add_argument("--create")
    c.add_argument("--access-mode", choices=["OPEN", "PRIVATE", "TEAM_ACL"])
    c.add_argument("--team-ids", help="comma-separated team ids for TEAM_ACL")
    c.add_argument("--set-access", type=int, help="board id to update access mode")
    c.set_defaults(func=cmd_boards)

    c = sub.add_parser("suggest-dod"); c.add_argument("task_id", type=int)
    c.add_argument("--apply", action="store_true", help="create checklist items from suggestion")
    c.set_defaults(func=cmd_suggest_dod)

    c = sub.add_parser("columns"); c.add_argument("board_id", type=int); c.add_argument("--create")
    c.add_argument("--wip-limit", type=int); c.add_argument("--mapped-status", choices=["BACKLOG", "IN_PROGRESS", "DONE", "ARCHIVED"])
    c.set_defaults(func=cmd_columns)

    c = sub.add_parser("tasks"); c.add_argument("column_id", type=int); c.add_argument("--create")
    c.add_argument("--description"); c.add_argument("--priority", default="MEDIUM", choices=["LOW", "MEDIUM", "HIGH"])
    c.add_argument("--deadline"); c.add_argument("--estimate", type=float); c.add_argument("--spent", type=float)
    c.add_argument("--type", choices=["BUG", "FEATURE", "CHORE"]); c.add_argument("--size", type=int, default=20)
    c.add_argument("--assignee", type=int); c.add_argument("--status", choices=["BACKLOG", "IN_PROGRESS", "DONE", "ARCHIVED"])
    c.add_argument("--sprint", type=int)
    c.set_defaults(func=cmd_tasks)

    c = sub.add_parser("task"); c.add_argument("task_id", type=int); c.add_argument("--update", action="store_true")
    c.add_argument("--title"); c.add_argument("--description"); c.add_argument("--priority", choices=["LOW", "MEDIUM", "HIGH"])
    c.add_argument("--deadline"); c.add_argument("--estimate", type=float); c.add_argument("--spent", type=float)
    c.add_argument("--type", choices=["BUG", "FEATURE", "CHORE"]); c.add_argument("--assignee", type=int)
    c.add_argument("--status", choices=["BACKLOG", "IN_PROGRESS", "DONE", "ARCHIVED"]); c.add_argument("--sprint", type=int)
    c.set_defaults(func=cmd_task)

    c = sub.add_parser("members"); c.add_argument("project_id", type=int); c.set_defaults(func=cmd_members)
    c = sub.add_parser("my-invites"); c.set_defaults(func=cmd_my_invites)

    c = sub.add_parser("search"); c.add_argument("project_id", type=int); c.add_argument("--q"); c.add_argument("--status")
    c.add_argument("--priority"); c.add_argument("--type"); c.add_argument("--size", type=int, default=20)
    c.set_defaults(func=cmd_search)

    c = sub.add_parser("move"); c.add_argument("task_id", type=int); c.add_argument("column_id", type=int); c.set_defaults(func=cmd_move)
    c = sub.add_parser("invite"); c.add_argument("project_id", type=int); c.add_argument("--email", required=True)
    c.add_argument("--role", default="EDITOR", choices=["EDITOR", "VIEWER", "CONTRACTOR"]); c.set_defaults(func=cmd_invite)
    c = sub.add_parser("accept-invite"); c.add_argument("invitation_id", type=int); c.set_defaults(func=cmd_accept_invite)
    c = sub.add_parser("decline-invite"); c.add_argument("invitation_id", type=int); c.set_defaults(func=cmd_decline_invite)
    c = sub.add_parser("transfer"); c.add_argument("project_id", type=int); c.add_argument("user_id", type=int); c.set_defaults(func=cmd_transfer)

    c = sub.add_parser("comments"); c.add_argument("task_id", type=int); c.add_argument("--create"); c.set_defaults(func=cmd_comments)
    c = sub.add_parser("checklist"); c.add_argument("task_id", type=int); c.add_argument("--create")
    c.add_argument("--done", type=int, help="Mark checklist item id done"); c.add_argument("--title"); c.set_defaults(func=cmd_checklist)
    c = sub.add_parser("deps"); c.add_argument("task_id", type=int); c.add_argument("--add", type=int); c.set_defaults(func=cmd_deps)
    c = sub.add_parser("labels"); c.add_argument("project_id", type=int); c.add_argument("--create"); c.add_argument("--color"); c.set_defaults(func=cmd_labels)
    c = sub.add_parser("sprints"); c.add_argument("project_id", type=int); c.add_argument("--create"); c.add_argument("--close", type=int)
    c.add_argument("--unfinished-action", choices=["ARCHIVE", "MOVE_TO_BACKLOG"]); c.set_defaults(func=cmd_sprints)
    c = sub.add_parser("notifications"); c.add_argument("--read", type=int, help="Mark notification id as read"); c.set_defaults(func=cmd_notifications)
    c = sub.add_parser("activity"); c.add_argument("project_id", type=int); c.add_argument("--size", type=int, default=20); c.set_defaults(func=cmd_activity)

    return p


def main(argv: list[str] | None = None) -> None:
    parser = build_parser()
    args = parser.parse_args(argv)
    args.func(args)


if __name__ == "__main__":
    main()
