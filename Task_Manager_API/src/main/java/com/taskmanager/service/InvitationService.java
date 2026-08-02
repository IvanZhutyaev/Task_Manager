package com.taskmanager.service;

import com.taskmanager.domain.InvitationStatus;
import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectInvitation;
import com.taskmanager.domain.ProjectMember;
import com.taskmanager.domain.ProjectRole;
import com.taskmanager.domain.User;
import com.taskmanager.repository.ProjectInvitationRepository;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.InvitationRequest;
import com.taskmanager.web.api.dto.InvitationResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationService {

    private final ProjectInvitationRepository invitationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectAccessService projectAccessService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final ProjectActivityService projectActivityService;

    public InvitationService(
            ProjectInvitationRepository invitationRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            ProjectService projectService,
            ProjectAccessService projectAccessService,
            CurrentUserService currentUserService,
            NotificationService notificationService,
            ProjectActivityService projectActivityService) {
        this.invitationRepository = invitationRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.projectAccessService = projectAccessService;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.projectActivityService = projectActivityService;
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> list(Long projectId) {
        Project project = projectService.getProjectOrThrow(projectId);
        projectAccessService.requireCanManageProject(project, currentUserService.getCurrentUser());
        return invitationRepository.findByProjectOrderByCreatedAtDesc(project).stream()
                .map(InvitationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listMine() {
        User user = currentUserService.getCurrentUser();
        return invitationRepository.findByEmailIgnoreCaseAndStatus(user.getEmail(), InvitationStatus.PENDING).stream()
                .map(InvitationResponse::from)
                .toList();
    }

    @Transactional
    public InvitationResponse invite(Long projectId, InvitationRequest request) {
        Project project = projectService.getProjectOrThrow(projectId);
        User actor = currentUserService.getCurrentUser();
        projectAccessService.requireCanManageProject(project, actor);
        if (request.role() == ProjectRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot invite as OWNER");
        }
        invitationRepository.findByProjectAndEmailIgnoreCaseAndStatus(project, request.email(), InvitationStatus.PENDING)
                .ifPresent(i -> {
                    throw new ApiException(HttpStatus.CONFLICT.value(), "Pending invitation already exists");
                });
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            if (projectMemberRepository.existsByProjectAndUser(project, user)) {
                throw new ApiException(HttpStatus.CONFLICT.value(), "User is already a project member");
            }
        });

        ProjectInvitation invitation = new ProjectInvitation();
        invitation.setProject(project);
        invitation.setEmail(request.email().trim().toLowerCase());
        invitation.setRole(request.role());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setInvitedBy(actor);
        invitation = invitationRepository.save(invitation);

        userRepository.findByEmailIgnoreCase(invitation.getEmail()).ifPresent(user ->
                notificationService.notify(user, "INVITED", "You were invited to project: " + project.getName(),
                        project.getId(), null));
        projectActivityService.record(project, actor, "INVITE_SENT", "Invited " + invitation.getEmail());
        return InvitationResponse.from(invitation);
    }

    @Transactional
    public InvitationResponse accept(Long invitationId) {
        ProjectInvitation invitation = getPending(invitationId);
        User user = currentUserService.getCurrentUser();
        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Invitation email does not match current user");
        }
        Project project = invitation.getProject();
        if (projectMemberRepository.existsByProjectAndUser(project, user)) {
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitationRepository.save(invitation);
            throw new ApiException(HttpStatus.CONFLICT.value(), "User is already a project member");
        }
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(invitation.getRole());
        projectMemberRepository.save(member);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);
        projectActivityService.record(project, user, "INVITE_ACCEPTED", user.getEmail() + " joined");
        return InvitationResponse.from(invitation);
    }

    @Transactional
    public InvitationResponse decline(Long invitationId) {
        ProjectInvitation invitation = getPending(invitationId);
        User user = currentUserService.getCurrentUser();
        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Invitation email does not match current user");
        }
        invitation.setStatus(InvitationStatus.DECLINED);
        return InvitationResponse.from(invitationRepository.save(invitation));
    }

    private ProjectInvitation getPending(Long id) {
        ProjectInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Invitation not found"));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Invitation is not pending");
        }
        return invitation;
    }
}
