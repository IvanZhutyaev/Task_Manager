package com.taskmanager.web.api.dto;

import java.math.BigDecimal;

/** Partial settings update — null fields mean "leave unchanged". */
public record ProjectSettingsRequest(
        Boolean strictBusinessRules,
        Boolean customWorkflowEnabled,
        BigDecimal capacityLimitHours,
        Integer slaWarningDays,
        Integer slaEscalateDays,
        Boolean dodTemplatesEnabled,
        Boolean requireApprovalForDone,
        Boolean priorityQueueRules,
        Integer maxUnfinishedSprintPercent,
        Integer autoArchiveDoneDays,
        Boolean restrictHighPriorityToOwner,
        Integer timeInStatusAlertsDays,
        Boolean blockDoneOnOpenRisks
) {
}
