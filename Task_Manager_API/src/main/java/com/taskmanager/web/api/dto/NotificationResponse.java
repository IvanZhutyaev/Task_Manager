package com.taskmanager.web.api.dto;

import com.taskmanager.domain.Notification;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String message,
        boolean read,
        Long projectId,
        Long taskId,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getProjectId(),
                notification.getTaskId(),
                notification.getCreatedAt());
    }
}
