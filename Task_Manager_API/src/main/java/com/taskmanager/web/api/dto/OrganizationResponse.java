package com.taskmanager.web.api.dto;

import com.taskmanager.domain.Organization;
import com.taskmanager.domain.OrganizationType;
import com.taskmanager.domain.OrgRole;
import java.time.Instant;

public record OrganizationResponse(
        Long id,
        String name,
        OrganizationType type,
        Instant createdAt,
        OrgRole currentUserRole,
        int maxMembers,
        int maxTeams,
        int maxProjects
) {
    public static OrganizationResponse from(Organization organization, OrgRole currentUserRole) {
        OrganizationType type = organization.getType();
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                type,
                organization.getCreatedAt(),
                currentUserRole,
                type.maxMembers(),
                type.maxTeams(),
                type.maxProjects()
        );
    }
}
