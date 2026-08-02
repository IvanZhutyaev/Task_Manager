package com.taskmanager.web.api.dto;

import java.math.BigDecimal;

public record SprintMetricsResponse(
        Long sprintId, int totalTasks, int completedTasks, int unfinishedTasks,
        int unfinishedPercent, BigDecimal estimatedHours, BigDecimal spentHours
) {
}
