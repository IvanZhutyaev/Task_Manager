package com.taskmanager.web.api.dto;

import com.taskmanager.domain.TeamMember;

public record TeamMemberResponse(Long userId, String email, String name) {

    public static TeamMemberResponse from(TeamMember member) {
        return new TeamMemberResponse(
                member.getUser().getId(),
                member.getUser().getEmail(),
                member.getUser().getName()
        );
    }
}
