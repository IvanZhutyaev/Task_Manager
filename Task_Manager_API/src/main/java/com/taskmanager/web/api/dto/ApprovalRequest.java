package com.taskmanager.web.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApprovalRequest(@NotNull Long approverId, @Size(max = 2000) String comment) {
}
