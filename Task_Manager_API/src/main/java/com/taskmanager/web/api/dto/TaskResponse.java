package com.taskmanager.web.api.dto;

import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskPriority;
import com.taskmanager.domain.TaskStatus;
import com.taskmanager.domain.TaskType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TaskResponse(
        Long id,
        Long columnId,
        String title,
        String description,
        TaskPriority priority,
        TaskStatus status,
        LocalDate deadline,
        Long assigneeId,
        String assigneeName,
        boolean overdue,
        BigDecimal estimateHours,
        BigDecimal spentHours,
        TaskType taskType,
        Long sprintId,
        List<LabelResponse> labels
) {

    public static TaskResponse from(Task task) {
        List<LabelResponse> labels = task.getLabels() == null
                ? List.of()
                : task.getLabels().stream().map(LabelResponse::from).toList();
        return new TaskResponse(
                task.getId(),
                task.getColumn().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus(),
                task.getDeadline(),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getAssignee() != null ? task.getAssignee().getName() : null,
                task.isOverdue(),
                task.getEstimateHours(),
                task.getSpentHours(),
                task.getTaskType(),
                task.getSprint() != null ? task.getSprint().getId() : null,
                labels
        );
    }
}
