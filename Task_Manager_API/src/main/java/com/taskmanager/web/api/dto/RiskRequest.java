package com.taskmanager.web.api.dto;

import com.taskmanager.domain.RiskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RiskRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description,
        RiskStatus status,
        LocalDate unblockAt,
        Long taskId
) {
}
