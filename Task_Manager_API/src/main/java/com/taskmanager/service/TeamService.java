package com.taskmanager.service;

import com.taskmanager.domain.Organization;
import com.taskmanager.domain.Team;
import com.taskmanager.domain.TeamMember;
import com.taskmanager.domain.User;
import com.taskmanager.repository.TeamMemberRepository;
import com.taskmanager.repository.TeamRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.TeamMemberRequest;
import com.taskmanager.web.api.dto.TeamMemberResponse;
import com.taskmanager.web.api.dto.TeamRequest;
import com.taskmanager.web.api.dto.TeamResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final OrganizationService organizationService;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    public TeamService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            OrganizationService organizationService,
            CurrentUserService currentUserService,
            UserService userService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.organizationService = organizationService;
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listTeams(Long organizationId) {
        Organization organization = organizationService.getOrganizationOrThrow(organizationId);
        organizationService.requireMembership(organization, currentUserService.getCurrentUser());
        return teamRepository.findByOrganizationOrderByCreatedAtAsc(organization).stream()
                .map(TeamResponse::from)
                .toList();
    }

    @Transactional
    public TeamResponse createTeam(Long organizationId, TeamRequest request) {
        Organization organization = organizationService.getOrganizationOrThrow(organizationId);
        organizationService.requireCanAdminister(organization, currentUserService.getCurrentUser());
        organizationService.ensureTeamCapacity(organization, teamRepository.countByOrganization(organization));

        Team team = new Team();
        team.setOrganization(organization);
        team.setName(request.name());
        return TeamResponse.from(teamRepository.save(team));
    }

    @Transactional
    public TeamResponse updateTeam(Long organizationId, Long teamId, TeamRequest request) {
        Team team = getTeamInOrgOrThrow(organizationId, teamId);
        organizationService.requireCanAdminister(team.getOrganization(), currentUserService.getCurrentUser());
        team.setName(request.name());
        return TeamResponse.from(teamRepository.save(team));
    }

    @Transactional
    public void deleteTeam(Long organizationId, Long teamId) {
        Team team = getTeamInOrgOrThrow(organizationId, teamId);
        organizationService.requireCanAdminister(team.getOrganization(), currentUserService.getCurrentUser());
        teamRepository.delete(team);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> listMembers(Long organizationId, Long teamId) {
        Team team = getTeamInOrgOrThrow(organizationId, teamId);
        organizationService.requireMembership(team.getOrganization(), currentUserService.getCurrentUser());
        return teamMemberRepository.findByTeam(team).stream()
                .map(TeamMemberResponse::from)
                .toList();
    }

    @Transactional
    public TeamMemberResponse addMember(Long organizationId, Long teamId, TeamMemberRequest request) {
        Team team = getTeamInOrgOrThrow(organizationId, teamId);
        Organization organization = team.getOrganization();
        organizationService.requireCanAdminister(organization, currentUserService.getCurrentUser());

        User user = userService.getUserByEmail(request.email());
        organizationService.requireMembership(organization, user);
        if (teamMemberRepository.existsByTeamAndUser(team, user)) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "User is already a team member");
        }

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        return TeamMemberResponse.from(teamMemberRepository.save(member));
    }

    @Transactional
    public void removeMember(Long organizationId, Long teamId, Long userId) {
        Team team = getTeamInOrgOrThrow(organizationId, teamId);
        organizationService.requireCanAdminister(team.getOrganization(), currentUserService.getCurrentUser());
        User user = userService.getUserById(userId);
        teamMemberRepository.findByTeamAndUser(team, user)
                .ifPresent(teamMemberRepository::delete);
    }

    public Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Team not found"));
    }

    public Team getTeamInOrgOrThrow(Long organizationId, Long teamId) {
        Organization organization = organizationService.getOrganizationOrThrow(organizationId);
        return teamRepository.findByIdAndOrganization(teamId, organization)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Team not found"));
    }
}
