package com.taskmanager.web.api.dto;

import com.taskmanager.domain.TaskChangeHistory;
import java.time.Instant;

public record TaskHistoryEntryResponse(
        Long id,
        String action,
        String details,
        Instant createdAt,
        String changedByName
) {
    public static TaskHistoryEntryResponse from(TaskChangeHistory history) {
        return new TaskHistoryEntryResponse(
                history.getId(),
                history.getAction(),
                history.getDetails(),
                history.getCreatedAt(),
                history.getChangedBy() != null ? history.getChangedBy().getName() : null
        );
    }
}
