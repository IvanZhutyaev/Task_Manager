package com.taskmanager.web.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LabelRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 20) String color
) {
}
