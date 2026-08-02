package com.taskmanager.android

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class ApiClient(
    var baseUrl: String = ApiConfig.DEFAULT_BASE_URL,
    var token: String? = null
) {
    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        )
        .build()

    class ApiException(message: String) : Exception(message)

    private fun url(path: String): String = baseUrl.trimEnd('/') + path

    private suspend fun <T> execute(
        method: String,
        path: String,
        body: Any? = null,
        type: java.lang.reflect.Type
    ): T = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(url(path))
        token?.let { builder.header("Authorization", "Bearer $it") }
        builder.header("Accept", "application/json")

        val requestBody = body?.let {
            gson.toJson(it).toRequestBody(jsonMedia)
        }

        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(requestBody ?: "".toRequestBody(jsonMedia))
            "PUT" -> builder.put(requestBody ?: "".toRequestBody(jsonMedia))
            "PATCH" -> builder.patch(requestBody ?: "".toRequestBody(jsonMedia))
            "DELETE" -> builder.delete(requestBody)
            else -> error("Unsupported method $method")
        }

        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    gson.fromJson(raw, ApiError::class.java)?.message
                }.getOrNull() ?: raw.ifBlank { "HTTP ${response.code}" }
                throw ApiException(message)
            }
            if (raw.isBlank() || type == Unit::class.java) {
                @Suppress("UNCHECKED_CAST")
                return@use Unit as T
            }
            gson.fromJson(raw, type)
        }
    }

    suspend fun health(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url("/health")).get().build()
        client.newCall(request).execute().use { it.body?.string().orEmpty() }
    }

    suspend fun login(email: String, password: String): AuthResponse =
        execute(
            "POST",
            "/auth/login",
            mapOf("email" to email, "password" to password),
            AuthResponse::class.java
        )

    suspend fun register(name: String, email: String, password: String): AuthResponse =
        execute(
            "POST",
            "/auth/register",
            mapOf("name" to name, "email" to email, "password" to password),
            AuthResponse::class.java
        )

    suspend fun me(): UserDto =
        execute("GET", "/users/me", null, UserDto::class.java)

    suspend fun listProjects(): List<ProjectDto> =
        execute("GET", "/projects", null, object : TypeToken<List<ProjectDto>>() {}.type)

    suspend fun createProject(
        name: String,
        description: String?,
        strictBusinessRules: Boolean = false,
        withDefaultBoard: Boolean = true,
        organizationId: Long? = null
    ): ProjectDto {
        val body = mutableMapOf<String, Any?>(
            "name" to name,
            "description" to description,
            "strictBusinessRules" to strictBusinessRules,
            "withDefaultBoard" to withDefaultBoard
        )
        if (organizationId != null) {
            body["organizationId"] = organizationId
        }
        return execute("POST", "/projects", body, ProjectDto::class.java)
    }

    suspend fun listOrganizations(): List<OrganizationDto> =
        execute("GET", "/organizations", null, object : TypeToken<List<OrganizationDto>>() {}.type)

    suspend fun createOrganization(name: String, type: String = "LOCAL"): OrganizationDto =
        execute(
            "POST",
            "/organizations",
            mapOf("name" to name, "type" to type),
            OrganizationDto::class.java
        )

    suspend fun listOrgMembers(organizationId: Long): List<OrgMemberDto> =
        execute(
            "GET",
            "/organizations/$organizationId/members",
            null,
            object : TypeToken<List<OrgMemberDto>>() {}.type
        )

    suspend fun addOrgMember(organizationId: Long, email: String, role: String = "MEMBER"): Map<String, Any> =
        execute(
            "POST",
            "/organizations/$organizationId/members",
            mapOf("email" to email, "role" to role),
            object : TypeToken<Map<String, Any>>() {}.type
        )

    suspend fun listTeams(organizationId: Long): List<TeamDto> =
        execute(
            "GET",
            "/organizations/$organizationId/teams",
            null,
            object : TypeToken<List<TeamDto>>() {}.type
        )

    suspend fun listTeamMembers(organizationId: Long, teamId: Long): List<TeamMemberDto> =
        execute(
            "GET",
            "/organizations/$organizationId/teams/$teamId/members",
            null,
            object : TypeToken<List<TeamMemberDto>>() {}.type
        )

    suspend fun createTeam(organizationId: Long, name: String): TeamDto =
        execute(
            "POST",
            "/organizations/$organizationId/teams",
            mapOf("name" to name),
            TeamDto::class.java
        )

    suspend fun addTeamMember(organizationId: Long, teamId: Long, email: String): Map<String, Any> =
        execute(
            "POST",
            "/organizations/$organizationId/teams/$teamId/members",
            mapOf("email" to email),
            object : TypeToken<Map<String, Any>>() {}.type
        )

    suspend fun listBoards(projectId: Long): List<BoardDto> =
        execute(
            "GET",
            "/projects/$projectId/boards",
            null,
            object : TypeToken<List<BoardDto>>() {}.type
        )

    suspend fun createBoard(
        projectId: Long,
        name: String,
        accessMode: String = "OPEN",
        teamIds: List<Long>? = null
    ): BoardDto {
        val body = mutableMapOf<String, Any?>(
            "name" to name,
            "accessMode" to accessMode
        )
        if (teamIds != null) {
            body["teamIds"] = teamIds
        }
        return execute("POST", "/projects/$projectId/boards", body, BoardDto::class.java)
    }

    suspend fun updateBoardAccess(
        projectId: Long,
        boardId: Long,
        name: String,
        accessMode: String,
        teamIds: List<Long>? = null
    ): BoardDto {
        val body = mutableMapOf<String, Any?>(
            "name" to name,
            "accessMode" to accessMode
        )
        if (teamIds != null) {
            body["teamIds"] = teamIds
        }
        return execute("PUT", "/projects/$projectId/boards/$boardId", body, BoardDto::class.java)
    }

    suspend fun suggestDod(taskId: Long): DodSuggestionDto =
        execute("POST", "/tasks/$taskId/ai/suggest-dod", emptyMap<String, Any>(), DodSuggestionDto::class.java)

    suspend fun createChecklistBulk(taskId: Long, titles: List<String>): List<ChecklistItemDto> =
        execute(
            "POST",
            "/tasks/$taskId/checklist/bulk",
            titles.map { mapOf("title" to it, "done" to false) },
            object : TypeToken<List<ChecklistItemDto>>() {}.type
        )

    suspend fun listColumns(boardId: Long): List<ColumnDto> =
        execute(
            "GET",
            "/boards/$boardId/columns",
            null,
            object : TypeToken<List<ColumnDto>>() {}.type
        )

    suspend fun createColumn(
        boardId: Long,
        name: String,
        wipLimit: Int? = null,
        mappedStatus: String? = null
    ): ColumnDto =
        execute(
            "POST",
            "/boards/$boardId/columns",
            mapOf(
                "name" to name,
                "wipLimit" to wipLimit,
                "mappedStatus" to mappedStatus
            ),
            ColumnDto::class.java
        )

    suspend fun listTasks(columnId: Long): List<TaskDto> {
        val page: PageDto<TaskDto> = execute(
            "GET",
            "/columns/$columnId/tasks?size=50",
            null,
            object : TypeToken<PageDto<TaskDto>>() {}.type
        )
        return page.content
    }

    suspend fun createTask(
        columnId: Long,
        title: String,
        priority: String = "MEDIUM",
        description: String? = null,
        assigneeId: Long? = null,
        deadline: String? = null,
        taskType: String? = null,
        estimateHours: Double? = null,
        spentHours: Double? = null,
        status: String? = "BACKLOG"
    ): TaskDto =
        execute(
            "POST",
            "/columns/$columnId/tasks",
            mapOf(
                "title" to title,
                "description" to description,
                "priority" to priority,
                "status" to status,
                "deadline" to deadline,
                "assigneeId" to assigneeId,
                "estimateHours" to estimateHours,
                "spentHours" to spentHours,
                "taskType" to taskType,
                "sprintId" to null,
                "labelIds" to null
            ),
            TaskDto::class.java
        )

    suspend fun updateTask(
        taskId: Long,
        title: String,
        description: String?,
        priority: String,
        status: String?,
        deadline: String?,
        assigneeId: Long?,
        estimateHours: Double?,
        spentHours: Double?,
        taskType: String?,
        sprintId: Long? = null
    ): TaskDto =
        execute(
            "PUT",
            "/tasks/$taskId",
            mapOf(
                "title" to title,
                "description" to description,
                "priority" to priority,
                "status" to status,
                "deadline" to deadline,
                "assigneeId" to assigneeId,
                "estimateHours" to estimateHours,
                "spentHours" to spentHours,
                "taskType" to taskType,
                "sprintId" to sprintId,
                "labelIds" to null
            ),
            TaskDto::class.java
        )

    suspend fun moveTask(taskId: Long, columnId: Long): TaskDto =
        execute(
            "PATCH",
            "/tasks/$taskId/move",
            mapOf("columnId" to columnId),
            TaskDto::class.java
        )

    suspend fun getTask(taskId: Long): TaskDto =
        execute("GET", "/tasks/$taskId", null, TaskDto::class.java)

    suspend fun searchProjectTasks(projectId: Long, q: String): List<TaskDto> {
        val page: PageDto<TaskDto> = execute(
            "GET",
            "/projects/$projectId/tasks?q=${java.net.URLEncoder.encode(q, "UTF-8")}&size=50",
            null,
            object : TypeToken<PageDto<TaskDto>>() {}.type
        )
        return page.content
    }

    suspend fun listNotifications(): List<NotificationDto> =
        execute("GET", "/users/me/notifications", null, object : TypeToken<List<NotificationDto>>() {}.type)

    suspend fun listMyInvitations(): List<InvitationDto> =
        execute("GET", "/users/me/invitations", null, object : TypeToken<List<InvitationDto>>() {}.type)

    suspend fun acceptInvitation(invitationId: Long): InvitationDto =
        execute("POST", "/invitations/$invitationId/accept", emptyMap<String, Any>(), InvitationDto::class.java)

    suspend fun declineInvitation(invitationId: Long): InvitationDto =
        execute("POST", "/invitations/$invitationId/decline", emptyMap<String, Any>(), InvitationDto::class.java)

    suspend fun inviteMember(projectId: Long, email: String, role: String = "EDITOR"): InvitationDto =
        execute(
            "POST",
            "/projects/$projectId/invitations",
            mapOf("email" to email, "role" to role),
            InvitationDto::class.java
        )

    suspend fun listMembers(projectId: Long): List<MemberDto> =
        execute("GET", "/projects/$projectId/members", null, object : TypeToken<List<MemberDto>>() {}.type)

    /** Debug helper for unexpected payloads. */
    fun peekJson(raw: String): String =
        runCatching { JsonParser.parseString(raw).toString() }.getOrDefault(raw)
}
