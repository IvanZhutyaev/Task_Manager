package com.taskmanager.service;

import com.taskmanager.domain.Organization;
import com.taskmanager.domain.OrganizationMember;
import com.taskmanager.domain.OrgRole;
import com.taskmanager.domain.User;
import com.taskmanager.repository.OrganizationMemberRepository;
import com.taskmanager.repository.OrganizationRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.OrgMemberRequest;
import com.taskmanager.web.api.dto.OrgMemberResponse;
import com.taskmanager.web.api.dto.OrganizationRequest;
import com.taskmanager.web.api.dto.OrganizationResponse;
import com.taskmanager.web.api.dto.UpdateOrgMemberRoleRequest;
import com.taskmanager.web.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            CurrentUserService currentUserService,
            UserService userService) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listMyOrganizations() {
        User user = currentUserService.getCurrentUser();
        return organizationRepository.findAllByMemberUserId(user.getId()).stream()
                .map(org -> OrganizationResponse.from(org, requireMembership(org, user).getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(Long organizationId) {
        Organization organization = getOrganizationOrThrow(organizationId);
        User user = currentUserService.getCurrentUser();
        return OrganizationResponse.from(organization, requireMembership(organization, user).getRole());
    }

    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        User owner = currentUserService.getCurrentUser();
        Organization organization = new Organization();
        organization.setName(request.name());
        organization.setType(request.typeOrDefault());
        organization = organizationRepository.save(organization);

        OrganizationMember member = new OrganizationMember();
        member.setOrganization(organization);
        member.setUser(owner);
        member.setRole(OrgRole.OWNER);
        organizationMemberRepository.save(member);

        return OrganizationResponse.from(organization, OrgRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<OrgMemberResponse> listMembers(Long organizationId) {
        Organization organization = getOrganizationOrThrow(organizationId);
        requireMembership(organization, currentUserService.getCurrentUser());
        return organizationMemberRepository.findByOrganization(organization).stream()
                .map(OrgMemberResponse::from)
                .toList();
    }

    @Transactional
    public OrgMemberResponse addMember(Long organizationId, OrgMemberRequest request) {
        Organization organization = getOrganizationOrThrow(organizationId);
        requireCanAdminister(organization, currentUserService.getCurrentUser());
        if (request.role() == OrgRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot assign OWNER role to a member");
        }
        ensureMemberCapacity(organization);

        User user = userService.getUserByEmail(request.email());
        if (organizationMemberRepository.existsByOrganizationAndUser(organization, user)) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "User is already an organization member");
        }

        OrganizationMember member = new OrganizationMember();
        member.setOrganization(organization);
        member.setUser(user);
        member.setRole(request.role());
        return OrgMemberResponse.from(organizationMemberRepository.save(member));
    }

    @Transactional
    public OrgMemberResponse updateMemberRole(Long organizationId, Long userId, UpdateOrgMemberRoleRequest request) {
        Organization organization = getOrganizationOrThrow(organizationId);
        requireCanAdminister(organization, currentUserService.getCurrentUser());
        if (request.role() == OrgRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot assign OWNER role to a member");
        }
        User user = userService.getUserById(userId);
        OrganizationMember member = requireMembership(organization, user);
        if (member.getRole() == OrgRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot change organization owner role");
        }
        member.setRole(request.role());
        return OrgMemberResponse.from(organizationMemberRepository.save(member));
    }

    @Transactional
    public void removeMember(Long organizationId, Long userId) {
        Organization organization = getOrganizationOrThrow(organizationId);
        requireCanAdminister(organization, currentUserService.getCurrentUser());
        User user = userService.getUserById(userId);
        OrganizationMember member = requireMembership(organization, user);
        if (member.getRole() == OrgRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot remove organization owner");
        }
        organizationMemberRepository.delete(member);
    }

    public Organization getOrganizationOrThrow(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Organization not found"));
    }

    public OrganizationMember requireMembership(Organization organization, User user) {
        return organizationMemberRepository.findByOrganizationAndUser(organization, user)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN.value(), "Access denied"));
    }

    public void requireCanAdminister(Organization organization, User user) {
        OrganizationMember member = requireMembership(organization, user);
        if (!member.getRole().canAdminister()) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Only organization owner or admin can manage this");
        }
    }

    public void requireOrgWrite(Organization organization, User user) {
        OrganizationMember member = requireMembership(organization, user);
        if (!member.getRole().canWriteContent()) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Insufficient organization permissions");
        }
    }

    public boolean isOrgAdminOrOwner(Organization organization, User user) {
        return organizationMemberRepository.findByOrganizationAndUser(organization, user)
                .map(m -> m.getRole().canAdminister())
                .orElse(false);
    }

    public void ensureMemberCapacity(Organization organization) {
        long count = organizationMemberRepository.countByOrganization(organization);
        if (count >= organization.getType().maxMembers()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                    "Organization member limit reached for " + organization.getType() + " plan");
        }
    }

    public void ensureTeamCapacity(Organization organization, long currentTeams) {
        if (currentTeams >= organization.getType().maxTeams()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                    "Organization team limit reached for " + organization.getType() + " plan");
        }
    }

    public void ensureProjectCapacity(Organization organization, long currentProjects) {
        if (currentProjects >= organization.getType().maxProjects()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                    "Organization project limit reached for " + organization.getType() + " plan");
        }
    }
}
