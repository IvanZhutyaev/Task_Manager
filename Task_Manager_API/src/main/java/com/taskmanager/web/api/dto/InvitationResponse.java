package com.taskmanager.web.api.dto;

import com.taskmanager.domain.InvitationStatus;
import com.taskmanager.domain.ProjectInvitation;
import com.taskmanager.domain.ProjectRole;
import java.time.Instant;

public record InvitationResponse(
        Long id,
        Long projectId,
        String projectName,
        String email,
        ProjectRole role,
        InvitationStatus status,
        Long invitedById,
        Instant createdAt
) {
    public static InvitationResponse from(ProjectInvitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getProject().getId(),
                invitation.getProject().getName(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getInvitedBy().getId(),
                invitation.getCreatedAt());
    }
}
