package com.taskmanager.web.api.dto;

import com.taskmanager.domain.TaskDependency;
import com.taskmanager.domain.TaskStatus;

public record DependencyResponse(Long id, Long taskId, Long blockerId, String blockerTitle, TaskStatus blockerStatus) {
    public static DependencyResponse from(TaskDependency dependency) {
        return new DependencyResponse(
                dependency.getId(),
                dependency.getTask().getId(),
                dependency.getBlocker().getId(),
                dependency.getBlocker().getTitle(),
                dependency.getBlocker().getStatus());
    }
}
