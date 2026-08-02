package com.taskmanager.web.api.dto;

import com.taskmanager.domain.TaskStatus;
import com.taskmanager.domain.TaskStatusHistory;
import java.time.Instant;

public record StatusHistoryResponse(
        Long id,
        TaskStatus status,
        Instant enteredAt,
        Instant leftAt
) {
    public static StatusHistoryResponse from(TaskStatusHistory h) {
        return new StatusHistoryResponse(h.getId(), h.getStatus(), h.getEnteredAt(), h.getLeftAt());
    }
}
