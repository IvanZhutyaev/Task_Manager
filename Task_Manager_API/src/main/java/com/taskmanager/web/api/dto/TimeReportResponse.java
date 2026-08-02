package com.taskmanager.web.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record TimeReportResponse(Long projectId, BigDecimal totalSpentHours, List<Row> rows) {
    public record Row(Long taskId, String taskTitle, Long assigneeId, String assigneeName, BigDecimal spentHours) {
    }
}
