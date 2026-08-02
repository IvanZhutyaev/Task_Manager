package com.taskmanager.web.api.dto;

import com.taskmanager.domain.TaskChecklistItem;

public record ChecklistItemResponse(Long id, Long taskId, String title, boolean done, int position) {
    public static ChecklistItemResponse from(TaskChecklistItem item) {
        return new ChecklistItemResponse(item.getId(), item.getTask().getId(), item.getTitle(), item.isDone(), item.getPosition());
    }
}
