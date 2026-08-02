package com.taskmanager.web.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SprintRequest(
        @NotBlank @Size(max = 255) String name,
        LocalDate startDate,
        LocalDate endDate
) {
}
