package com.taskmanager.web.api.dto;

import com.taskmanager.domain.OrgRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrgMemberRequest(
        @NotBlank @Email String email,
        @NotNull OrgRole role
) {
}
