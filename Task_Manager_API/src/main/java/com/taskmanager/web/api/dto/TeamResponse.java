package com.taskmanager.web.api.dto;

import com.taskmanager.domain.Team;
import java.time.Instant;

public record TeamResponse(Long id, Long organizationId, String name, Instant createdAt) {

    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getOrganization().getId(),
                team.getName(),
                team.getCreatedAt()
        );
    }
}
