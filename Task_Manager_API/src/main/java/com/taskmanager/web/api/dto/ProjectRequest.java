package com.taskmanager.web.api.dto;

import com.taskmanager.domain.ProjectTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProjectRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description,
        Boolean strictBusinessRules,
        Boolean withDefaultBoard,
        ProjectTemplate template,
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
        Boolean blockDoneOnOpenRisks,
        Long organizationId
) {
    public ProjectRequest(String name, String description, Boolean strictBusinessRules, Boolean withDefaultBoard) {
        this(name, description, strictBusinessRules, withDefaultBoard, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    public boolean strictBusinessRulesOrDefault() {
        return Boolean.TRUE.equals(strictBusinessRules);
    }

    public boolean withDefaultBoardOrDefault() {
        if (template != null && template != ProjectTemplate.NONE) {
            return true;
        }
        return Boolean.TRUE.equals(withDefaultBoard);
    }

    public ProjectTemplate templateOrDefault() {
        return template == null ? ProjectTemplate.NONE : template;
    }
}
