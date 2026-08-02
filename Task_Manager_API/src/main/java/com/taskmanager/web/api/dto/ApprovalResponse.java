package com.taskmanager.web.api.dto;

import com.taskmanager.domain.ApprovalStatus;
import com.taskmanager.domain.TaskApproval;
import java.time.Instant;

public record ApprovalResponse(
        Long id, Long taskId, Long approverId, String approverName,
        ApprovalStatus status, String comment, Instant createdAt
) {
    public static ApprovalResponse from(TaskApproval approval) {
        return new ApprovalResponse(approval.getId(), approval.getTask().getId(),
                approval.getApprover().getId(), approval.getApprover().getName(),
                approval.getStatus(), approval.getComment(), approval.getCreatedAt());
    }
}
