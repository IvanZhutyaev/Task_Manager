package com.taskmanager.web.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoalRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description
) {
}
