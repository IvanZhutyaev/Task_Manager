package com.taskmanager.android

object L10n {
    fun role(role: String?): String = when (role?.uppercase()) {
        "OWNER" -> "Владелец"
        "ADMIN" -> "Администратор"
        "MEMBER" -> "Участник"
        "VIEWER" -> "Наблюдатель"
        "EDITOR" -> "Редактор"
        null, "", "—", "ROLE" -> ""
        else -> role
    }

    fun accessMode(mode: String?): String? = when (mode?.uppercase()) {
        "OPEN" -> "Открытая"
        "CLOSED" -> "Закрытая"
        "TEAM_ONLY", "TEAM" -> "Только команда"
        null, "" -> null
        else -> mode
    }

    /** Subtitle for board cards; omits default OPEN access. */
    fun boardAccessLabel(mode: String?): String? {
        if (mode.isNullOrBlank() || mode.uppercase() == "OPEN") return null
        return accessMode(mode)
    }

    fun boardAccessHeader(mode: String?): String? {
        val label = boardAccessLabel(mode) ?: return null
        return "Доступ: $label"
    }

    fun priority(priority: String): String = when (priority.uppercase()) {
        "LOW" -> "Низкий"
        "MEDIUM" -> "Средний"
        "HIGH" -> "Высокий"
        "CRITICAL" -> "Критический"
        else -> priority
    }

    fun status(status: String): String = when (status.uppercase()) {
        "BACKLOG" -> "Бэклог"
        "TODO" -> "К выполнению"
        "IN_PROGRESS" -> "В работе"
        "DONE" -> "Готово"
        "OVERDUE" -> "Просрочено"
        else -> status
    }

    fun taskMeta(task: TaskDto, columnMappedStatus: String?): String {
        val parts = mutableListOf<String>()
        parts.add(priority(task.priority))

        if (task.overdue) {
            parts.add("Просрочено")
        } else {
            val impliedByColumn = !columnMappedStatus.isNullOrBlank() &&
                task.status.equals(columnMappedStatus, ignoreCase = true)
            if (!impliedByColumn && task.status.isNotBlank()) {
                parts.add(status(task.status))
            }
        }

        parts.add(task.assigneeName ?: "без исполнителя")
        return parts.joinToString(" · ")
    }

    val orgMemberRoleLabels = listOf("Участник", "Администратор", "Наблюдатель")
    val orgMemberRoleValues = listOf("MEMBER", "ADMIN", "VIEWER")
}
