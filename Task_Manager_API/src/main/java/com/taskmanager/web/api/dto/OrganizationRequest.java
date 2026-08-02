package com.taskmanager.web.api.dto;

import com.taskmanager.domain.OrganizationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationRequest(
        @NotBlank @Size(max = 255) String name,
        OrganizationType type
) {
    public OrganizationType typeOrDefault() {
        return type == null ? OrganizationType.LOCAL : type;
    }
}
