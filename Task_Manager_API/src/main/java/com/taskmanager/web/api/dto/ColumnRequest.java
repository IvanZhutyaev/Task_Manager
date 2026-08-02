package com.taskmanager.web.api.dto;

import com.taskmanager.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ColumnRequest(
        @NotBlank @Size(max = 255) String name,
        Integer wipLimit,
        TaskStatus mappedStatus
) {
}
