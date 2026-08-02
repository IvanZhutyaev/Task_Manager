package com.taskmanager.android

data class AuthResponse(val token: String, val user: UserDto)
data class UserDto(val id: Long, val email: String, val name: String)
data class ProjectDto(
    val id: Long,
    val name: String,
    val description: String?,
    val ownerId: Long,
    val ownerName: String,
    val currentUserRole: String?,
    val strictBusinessRules: Boolean = false,
    val organizationId: Long? = null
)
data class BoardDto(
    val id: Long,
    val projectId: Long,
    val name: String,
    val accessMode: String? = "OPEN",
    val createdById: Long? = null,
    val teamIds: List<Long>? = null
)
data class OrganizationDto(
    val id: Long,
    val name: String,
    val type: String,
    val currentUserRole: String? = null,
    val maxMembers: Int? = null,
    val maxTeams: Int? = null,
    val maxProjects: Int? = null
)
data class TeamDto(
    val id: Long,
    val organizationId: Long,
    val name: String
)
data class OrgMemberDto(
    val userId: Long,
    val email: String,
    val name: String?,
    val role: String
)
data class TeamMemberDto(
    val userId: Long,
    val email: String,
    val name: String?
)
data class DodSuggestionDto(
    val items: List<SuggestedDodItemDto>? = null
)
data class SuggestedDodItemDto(
    val title: String,
    val position: Int
)
data class ChecklistItemDto(
    val id: Long? = null,
    val title: String,
    val done: Boolean = false,
    val position: Int? = null
)
data class ColumnDto(
    val id: Long,
    val boardId: Long,
    val name: String,
    val position: Int,
    val wipLimit: Int? = null,
    val mappedStatus: String? = null
)
data class TaskDto(
    val id: Long,
    val columnId: Long,
    val title: String,
    val description: String?,
    val priority: String,
    val status: String,
    val deadline: String?,
    val assigneeId: Long?,
    val assigneeName: String?,
    val overdue: Boolean = false,
    val estimateHours: Double? = null,
    val spentHours: Double? = null,
    val taskType: String? = null,
    val sprintId: Long? = null,
    val labels: List<LabelDto>? = null
)
data class LabelDto(val id: Long, val projectId: Long, val name: String, val color: String?)
data class MemberDto(val userId: Long, val email: String, val name: String, val role: String)
data class InvitationDto(
    val id: Long,
    val projectId: Long,
    val projectName: String?,
    val email: String?,
    val role: String?,
    val status: String?
)
data class PageDto<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
data class NotificationDto(
    val id: Long,
    val type: String,
    val message: String,
    val read: Boolean,
    val projectId: Long?,
    val taskId: Long?,
    val createdAt: String? = null
)
data class ApiError(val status: Int?, val message: String?, val timestamp: String? = null)
