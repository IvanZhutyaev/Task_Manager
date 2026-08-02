package com.taskmanager.web.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChecklistItemRequest(
        @NotBlank @Size(max = 500) String title,
        Boolean done
) {
}
