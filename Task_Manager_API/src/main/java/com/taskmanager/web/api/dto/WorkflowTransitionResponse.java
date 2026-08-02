package com.taskmanager.web.api.dto;

import com.taskmanager.domain.ProjectWorkflowTransition;
import com.taskmanager.domain.TaskStatus;

public record WorkflowTransitionResponse(Long id, TaskStatus fromStatus, TaskStatus toStatus) {
    public static WorkflowTransitionResponse from(ProjectWorkflowTransition transition) {
        return new WorkflowTransitionResponse(
                transition.getId(), transition.getFromStatus(), transition.getToStatus());
    }
}
