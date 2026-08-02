package com.taskmanager.web.api.dto;

import com.taskmanager.domain.OrgRole;
import jakarta.validation.constraints.NotNull;

public record UpdateOrgMemberRoleRequest(@NotNull OrgRole role) {
}
