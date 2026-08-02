package com.taskmanager.web.api.dto;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectRole;
import java.math.BigDecimal;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Long ownerId,
        String ownerName,
        ProjectRole currentUserRole,
        boolean strictBusinessRules,
        boolean customWorkflowEnabled,
        BigDecimal capacityLimitHours,
        Integer slaWarningDays,
        Integer slaEscalateDays,
        boolean dodTemplatesEnabled,
        boolean requireApprovalForDone,
        boolean priorityQueueRules,
        Integer maxUnfinishedSprintPercent,
        Integer autoArchiveDoneDays,
        boolean restrictHighPriorityToOwner,
        Integer timeInStatusAlertsDays,
        boolean blockDoneOnOpenRisks,
        Long organizationId
) {

    public static ProjectResponse from(Project project, ProjectRole currentUserRole) {
        Long organizationId = project.getOrganization() != null ? project.getOrganization().getId() : null;
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOwner().getId(),
                project.getOwner().getName(),
                currentUserRole,
                project.isStrictBusinessRules(),
                project.isCustomWorkflowEnabled(),
                project.getCapacityLimitHours(),
                project.getSlaWarningDays(),
                project.getSlaEscalateDays(),
                project.isDodTemplatesEnabled(),
                project.isRequireApprovalForDone(),
                project.isPriorityQueueRules(),
                project.getMaxUnfinishedSprintPercent(),
                project.getAutoArchiveDoneDays(),
                project.isRestrictHighPriorityToOwner(),
                project.getTimeInStatusAlertsDays(),
                project.isBlockDoneOnOpenRisks(),
                organizationId
        );
    }
}
