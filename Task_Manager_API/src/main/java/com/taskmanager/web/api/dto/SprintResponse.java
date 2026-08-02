package com.taskmanager.web.api.dto;

import com.taskmanager.domain.Sprint;
import com.taskmanager.domain.SprintStatus;
import java.time.Instant;
import java.time.LocalDate;

public record SprintResponse(
        Long id,
        Long projectId,
        String name,
        SprintStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt
) {
    public static SprintResponse from(Sprint sprint) {
        return new SprintResponse(
                sprint.getId(),
                sprint.getProject().getId(),
                sprint.getName(),
                sprint.getStatus(),
                sprint.getStartDate(),
                sprint.getEndDate(),
                sprint.getCreatedAt());
    }
}
