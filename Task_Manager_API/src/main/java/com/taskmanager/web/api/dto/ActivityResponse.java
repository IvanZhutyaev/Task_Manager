package com.taskmanager.web.api.dto;

import com.taskmanager.domain.ProjectActivity;
import java.time.Instant;

public record ActivityResponse(
        Long id,
        Long projectId,
        Long actorId,
        String actorName,
        String action,
        String details,
        Instant createdAt
) {
    public static ActivityResponse from(ProjectActivity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getProject().getId(),
                activity.getActor() != null ? activity.getActor().getId() : null,
                activity.getActor() != null ? activity.getActor().getName() : null,
                activity.getAction(),
                activity.getDetails(),
                activity.getCreatedAt());
    }
}
