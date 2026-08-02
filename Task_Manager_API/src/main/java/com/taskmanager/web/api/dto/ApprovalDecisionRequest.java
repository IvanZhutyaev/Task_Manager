package com.taskmanager.web.api.dto;

import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(@Size(max = 2000) String comment) {
}
