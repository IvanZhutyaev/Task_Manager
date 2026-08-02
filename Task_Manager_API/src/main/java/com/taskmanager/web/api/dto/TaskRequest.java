package com.taskmanager.web.api.dto;

import com.taskmanager.domain.TaskPriority;
import com.taskmanager.domain.TaskStatus;
import com.taskmanager.domain.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TaskRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 4000) String description,
        @NotNull TaskPriority priority,
        LocalDate deadline,
        Long assigneeId,
        TaskStatus status,
        BigDecimal estimateHours,
        BigDecimal spentHours,
        TaskType taskType,
        Long sprintId,
        List<Long> labelIds
) {
}
