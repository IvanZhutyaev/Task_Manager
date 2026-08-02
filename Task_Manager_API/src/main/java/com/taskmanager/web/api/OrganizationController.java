package com.taskmanager.web.api;

import com.taskmanager.config.ApiConstants;
import com.taskmanager.service.OrganizationService;
import com.taskmanager.service.TeamService;
import com.taskmanager.web.api.dto.OrgMemberRequest;
import com.taskmanager.web.api.dto.OrgMemberResponse;
import com.taskmanager.web.api.dto.OrganizationRequest;
import com.taskmanager.web.api.dto.OrganizationResponse;
import com.taskmanager.web.api.dto.TeamMemberRequest;
import com.taskmanager.web.api.dto.TeamMemberResponse;
import com.taskmanager.web.api.dto.TeamRequest;
import com.taskmanager.web.api.dto.TeamResponse;
import com.taskmanager.web.api.dto.UpdateOrgMemberRoleRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = ApiConstants.API_V1 + "/organizations", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrganizationController {

    private final OrganizationService organizationService;
    private final TeamService teamService;

    public OrganizationController(OrganizationService organizationService, TeamService teamService) {
        this.organizationService = organizationService;
        this.teamService = teamService;
    }

    @GetMapping
    public List<OrganizationResponse> list() {
        return organizationService.listMyOrganizations();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@Valid @RequestBody OrganizationRequest request) {
        return organizationService.createOrganization(request);
    }

    @GetMapping("/{organizationId}")
    public OrganizationResponse get(@PathVariable Long organizationId) {
        return organizationService.getOrganization(organizationId);
    }

    @GetMapping("/{organizationId}/members")
    public List<OrgMemberResponse> listMembers(@PathVariable Long organizationId) {
        return organizationService.listMembers(organizationId);
    }

    @PostMapping("/{organizationId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public OrgMemberResponse addMember(
            @PathVariable Long organizationId,
            @Valid @RequestBody OrgMemberRequest request) {
        return organizationService.addMember(organizationId, request);
    }

    @PutMapping("/{organizationId}/members/{userId}")
    public OrgMemberResponse updateMemberRole(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateOrgMemberRoleRequest request) {
        return organizationService.updateMemberRole(organizationId, userId, request);
    }

    @DeleteMapping("/{organizationId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long organizationId, @PathVariable Long userId) {
        organizationService.removeMember(organizationId, userId);
    }

    @GetMapping("/{organizationId}/teams")
    public List<TeamResponse> listTeams(@PathVariable Long organizationId) {
        return teamService.listTeams(organizationId);
    }

    @PostMapping("/{organizationId}/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse createTeam(
            @PathVariable Long organizationId,
            @Valid @RequestBody TeamRequest request) {
        return teamService.createTeam(organizationId, request);
    }

    @PutMapping("/{organizationId}/teams/{teamId}")
    public TeamResponse updateTeam(
            @PathVariable Long organizationId,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamRequest request) {
        return teamService.updateTeam(organizationId, teamId, request);
    }

    @DeleteMapping("/{organizationId}/teams/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeam(@PathVariable Long organizationId, @PathVariable Long teamId) {
        teamService.deleteTeam(organizationId, teamId);
    }

    @GetMapping("/{organizationId}/teams/{teamId}/members")
    public List<TeamMemberResponse> listTeamMembers(
            @PathVariable Long organizationId,
            @PathVariable Long teamId) {
        return teamService.listMembers(organizationId, teamId);
    }

    @PostMapping("/{organizationId}/teams/{teamId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamMemberResponse addTeamMember(
            @PathVariable Long organizationId,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamMemberRequest request) {
        return teamService.addMember(organizationId, teamId, request);
    }

    @DeleteMapping("/{organizationId}/teams/{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTeamMember(
            @PathVariable Long organizationId,
            @PathVariable Long teamId,
            @PathVariable Long userId) {
        teamService.removeMember(organizationId, teamId, userId);
    }
}
