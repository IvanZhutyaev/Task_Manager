package com.taskmanager.web.api.dto;

import com.taskmanager.domain.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record MemberRequest(
        @NotNull Long userId,
        @NotNull ProjectRole role
) {
}
