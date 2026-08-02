package com.taskmanager.web.api.dto;

import jakarta.validation.constraints.NotNull;

public record DependencyRequest(@NotNull Long blockerId) {
}
