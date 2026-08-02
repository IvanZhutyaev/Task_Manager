package com.taskmanager.web.api.dto;

import com.taskmanager.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record WorkflowTransitionRequest(
        @NotNull TaskStatus fromStatus,
        @NotNull TaskStatus toStatus
) {
}
