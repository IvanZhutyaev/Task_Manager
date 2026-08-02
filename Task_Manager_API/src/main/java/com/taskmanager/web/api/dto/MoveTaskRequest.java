package com.taskmanager.web.api.dto;

import jakarta.validation.constraints.NotNull;

public record MoveTaskRequest(@NotNull Long columnId) {
}
