package com.taskmanager.web.api.dto;

import com.taskmanager.domain.ProjectRisk;
import com.taskmanager.domain.RiskStatus;
import java.time.Instant;
import java.time.LocalDate;

public record RiskResponse(
        Long id, String title, String description, RiskStatus status,
        LocalDate unblockAt, Long taskId, Instant createdAt
) {
    public static RiskResponse from(ProjectRisk risk) {
        return new RiskResponse(risk.getId(), risk.getTitle(), risk.getDescription(), risk.getStatus(),
                risk.getUnblockAt(), risk.getTask() == null ? null : risk.getTask().getId(), risk.getCreatedAt());
    }
}
