package com.taskmanager.web.api.dto;

import com.taskmanager.domain.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DodTemplateRequest(
        @NotNull TaskType taskType,
        @NotBlank @Size(max = 500) String title,
        Integer position
) {
}
