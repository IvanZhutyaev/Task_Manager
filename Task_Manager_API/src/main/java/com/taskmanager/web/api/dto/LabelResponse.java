package com.taskmanager.web.api.dto;

import com.taskmanager.domain.Label;

public record LabelResponse(Long id, Long projectId, String name, String color) {
    public static LabelResponse from(Label label) {
        return new LabelResponse(label.getId(), label.getProject().getId(), label.getName(), label.getColor());
    }
}
