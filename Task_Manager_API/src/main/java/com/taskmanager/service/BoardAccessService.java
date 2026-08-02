package com.taskmanager.service;

import com.taskmanager.domain.Board;
import com.taskmanager.domain.BoardAccessMode;
import com.taskmanager.domain.Organization;
import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectMember;
import com.taskmanager.domain.ProjectRole;
import com.taskmanager.domain.User;
import com.taskmanager.repository.TeamMemberRepository;
import com.taskmanager.web.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BoardAccessService {

    private final ProjectAccessService projectAccessService;
    private final OrganizationService organizationService;
    private final TeamMemberRepository teamMemberRepository;

    public BoardAccessService(
            ProjectAccessService projectAccessService,
            OrganizationService organizationService,
            TeamMemberRepository teamMemberRepository) {
        this.projectAccessService = projectAccessService;
        this.organizationService = organizationService;
        this.teamMemberRepository = teamMemberRepository;
    }

    public void requireCanViewBoard(Board board, User user) {
        Project project = board.getProject();
        projectAccessService.requireCanRead(project, user);
        if (!canAccessBoard(board, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Access denied to this board");
        }
    }

    public void requireCanWriteBoard(Board board, User user) {
        Project project = board.getProject();
        projectAccessService.requireCanWriteContent(project, user);
        if (!canAccessBoard(board, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Access denied to this board");
        }
    }

    public boolean canAccessBoard(Board board, User user) {
        BoardAccessMode mode = board.getAccessMode() == null ? BoardAccessMode.OPEN : board.getAccessMode();
        if (mode == BoardAccessMode.OPEN) {
            return true;
        }
        if (isPrivilegedForBoard(board, user)) {
            return true;
        }
        if (mode == BoardAccessMode.PRIVATE) {
            return board.getCreatedBy() != null && board.getCreatedBy().getId().equals(user.getId());
        }
        if (mode == BoardAccessMode.TEAM_ACL) {
            return teamMemberRepository.existsOnBoardTeams(board.getId(), user.getId());
        }
        return false;
    }

    public boolean canSetTeamAcl(Project project, User user) {
        ProjectMember member = projectAccessService.requireMembership(project, user);
        if (member.getRole() == ProjectRole.OWNER) {
            return true;
        }
        Organization organization = project.getOrganization();
        return organization != null && organizationService.isOrgAdminOrOwner(organization, user);
    }

    public void assertCanSetAccessMode(Project project, User user, BoardAccessMode mode) {
        if (mode == null || mode == BoardAccessMode.OPEN || mode == BoardAccessMode.PRIVATE) {
            return;
        }
        if (mode == BoardAccessMode.TEAM_ACL) {
            if (project.getOrganization() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                        "TEAM_ACL boards require a project linked to an organization");
            }
            if (!canSetTeamAcl(project, user)) {
                throw new ApiException(HttpStatus.FORBIDDEN.value(),
                        "Only project owner or organization admin can set TEAM_ACL");
            }
        }
    }

    public boolean isPrivilegedForBoard(Board board, User user) {
        Project project = board.getProject();
        ProjectMember member = projectAccessService.requireMembership(project, user);
        if (member.getRole() == ProjectRole.OWNER) {
            return true;
        }
        Organization organization = project.getOrganization();
        return organization != null && organizationService.isOrgAdminOrOwner(organization, user);
    }
}
