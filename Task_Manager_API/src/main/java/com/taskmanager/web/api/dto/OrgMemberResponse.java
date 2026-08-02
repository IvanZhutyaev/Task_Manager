package com.taskmanager.web.api.dto;

import com.taskmanager.domain.OrgRole;
import com.taskmanager.domain.OrganizationMember;

public record OrgMemberResponse(Long userId, String email, String name, OrgRole role) {

    public static OrgMemberResponse from(OrganizationMember member) {
        return new OrgMemberResponse(
                member.getUser().getId(),
                member.getUser().getEmail(),
                member.getUser().getName(),
                member.getRole()
        );
    }
}
