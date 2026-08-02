package com.taskmanager.web.api.dto;

import com.taskmanager.domain.DodTemplate;
import com.taskmanager.domain.TaskType;

public record DodTemplateResponse(Long id, TaskType taskType, String title, int position) {
    public static DodTemplateResponse from(DodTemplate template) {
        return new DodTemplateResponse(
                template.getId(), template.getTaskType(), template.getTitle(), template.getPosition());
    }
}
