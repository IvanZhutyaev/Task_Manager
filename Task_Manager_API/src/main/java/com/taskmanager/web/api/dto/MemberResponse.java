package com.taskmanager.web.api.dto;

import com.taskmanager.domain.ProjectRole;

public record MemberResponse(Long userId, String email, String name, ProjectRole role) {
}
