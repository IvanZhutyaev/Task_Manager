package com.taskmanager.service;

import com.taskmanager.domain.Board;
import com.taskmanager.domain.BoardAccessMode;
import com.taskmanager.domain.BoardColumn;
import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectMember;
import com.taskmanager.domain.ProjectRole;
import com.taskmanager.domain.ProjectTemplate;
import com.taskmanager.domain.ProjectWorkflowTransition;
import com.taskmanager.domain.DodTemplate;
import com.taskmanager.domain.Sprint;
import com.taskmanager.domain.SprintStatus;
import com.taskmanager.domain.TaskStatus;
import com.taskmanager.domain.TaskType;
import com.taskmanager.domain.User;
import com.taskmanager.repository.BoardColumnRepository;
import com.taskmanager.repository.BoardRepository;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.DodTemplateRepository;
import com.taskmanager.repository.ProjectWorkflowTransitionRepository;
import com.taskmanager.repository.SprintRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.MemberRequest;
import com.taskmanager.web.api.dto.MemberResponse;
import com.taskmanager.web.api.dto.ProjectRequest;
import com.taskmanager.web.api.dto.ProjectResponse;
import com.taskmanager.web.api.dto.ProjectSettingsRequest;
import com.taskmanager.web.api.dto.TransferOwnershipRequest;
import com.taskmanager.web.api.dto.UpdateMemberRoleRequest;
import com.taskmanager.web.exception.ApiException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final ProjectAccessService projectAccessService;
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ProjectActivityService projectActivityService;
    private final DodTemplateRepository dodTemplateRepository;
    private final ProjectWorkflowTransitionRepository workflowRepository;
    private final SprintRepository sprintRepository;
    private final OrganizationService organizationService;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            BoardRepository boardRepository,
            BoardColumnRepository boardColumnRepository,
            ProjectAccessService projectAccessService,
            CurrentUserService currentUserService,
            UserService userService,
            NotificationService notificationService,
            ProjectActivityService projectActivityService,
            DodTemplateRepository dodTemplateRepository,
            ProjectWorkflowTransitionRepository workflowRepository,
            SprintRepository sprintRepository,
            OrganizationService organizationService) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.boardRepository = boardRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.projectAccessService = projectAccessService;
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.projectActivityService = projectActivityService;
        this.dodTemplateRepository = dodTemplateRepository;
        this.workflowRepository = workflowRepository;
        this.sprintRepository = sprintRepository;
        this.organizationService = organizationService;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects() {
        User user = currentUserService.getCurrentUser();
        return projectRepository.findAllByMember(user).stream()
                .map(project -> {
                    ProjectRole role = projectAccessService.requireMembership(project, user).getRole();
                    return ProjectResponse.from(project, role);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId) {
        Project project = getProjectOrThrow(projectId);
        User user = currentUserService.getCurrentUser();
        ProjectRole role = projectAccessService.requireMembership(project, user).getRole();
        return ProjectResponse.from(project, role);
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        User owner = currentUserService.getCurrentUser();

        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setOwner(owner);
        if (request.organizationId() != null) {
            var organization = organizationService.getOrganizationOrThrow(request.organizationId());
            organizationService.requireOrgWrite(organization, owner);
            organizationService.ensureProjectCapacity(
                    organization, projectRepository.countByOrganization(organization));
            project.setOrganization(organization);
        }
        applySettingsFlags(project, request.strictBusinessRules(), request.customWorkflowEnabled(),
                request.capacityLimitHours(), request.slaWarningDays(), request.slaEscalateDays(),
                request.dodTemplatesEnabled(), request.requireApprovalForDone(), request.priorityQueueRules(),
                request.maxUnfinishedSprintPercent(), request.autoArchiveDoneDays(),
                request.restrictHighPriorityToOwner(), request.timeInStatusAlertsDays(),
                request.blockDoneOnOpenRisks());
        project = projectRepository.save(project);

        ProjectMember ownerMember = new ProjectMember();
        ownerMember.setProject(project);
        ownerMember.setUser(owner);
        ownerMember.setRole(ProjectRole.OWNER);
        projectMemberRepository.save(ownerMember);

        seedTemplate(project, request.templateOrDefault(), request.withDefaultBoardOrDefault());

        projectActivityService.record(project, owner, "PROJECT_CREATED", "Project created");
        log.info("Project created: id={}, owner={}", project.getId(), owner.getEmail());
        return ProjectResponse.from(project, ProjectRole.OWNER);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        Project project = getProjectOrThrow(projectId);
        projectAccessService.requireCanManageProject(project, currentUserService.getCurrentUser());

        project.setName(request.name());
        project.setDescription(request.description());
        applySettingsFlags(project, request.strictBusinessRules(), request.customWorkflowEnabled(),
                request.capacityLimitHours(), request.slaWarningDays(), request.slaEscalateDays(),
                request.dodTemplatesEnabled(), request.requireApprovalForDone(), request.priorityQueueRules(),
                request.maxUnfinishedSprintPercent(), request.autoArchiveDoneDays(),
                request.restrictHighPriorityToOwner(), request.timeInStatusAlertsDays(),
                request.blockDoneOnOpenRisks());
        project = projectRepository.save(project);

        log.info("Project updated: id={}", projectId);
        return ProjectResponse.from(project, ProjectRole.OWNER);
    }

    @Transactional
    public ProjectResponse updateSettings(Long projectId, ProjectSettingsRequest request) {
        Project project = getProjectOrThrow(projectId);
        projectAccessService.requireCanManageProject(project, currentUserService.getCurrentUser());
        applySettingsFlags(project, request.strictBusinessRules(), request.customWorkflowEnabled(),
                request.capacityLimitHours(), request.slaWarningDays(), request.slaEscalateDays(),
                request.dodTemplatesEnabled(), request.requireApprovalForDone(), request.priorityQueueRules(),
                request.maxUnfinishedSprintPercent(), request.autoArchiveDoneDays(),
                request.restrictHighPriorityToOwner(), request.timeInStatusAlertsDays(),
                request.blockDoneOnOpenRisks());
        return ProjectResponse.from(projectRepository.save(project), ProjectRole.OWNER);
    }

    @Transactional
    public void deleteProject(Long projectId) {
        Project project = getProjectOrThrow(projectId);
        projectAccessService.requireCanManageProject(project, currentUserService.getCurrentUser());
        projectRepository.delete(project);
        log.info("Project deleted: id={}", projectId);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long projectId) {
        Project project = getProjectOrThrow(projectId);
        User current = currentUserService.getCurrentUser();
        ProjectMember currentMember = projectAccessService.requireMembership(project, current);
        if (currentMember.isContractor()) {
            return List.of(toMemberResponse(currentMember));
        }
        return projectMemberRepository.findByProject(project).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public MemberResponse addMember(Long projectId, MemberRequest request) {
        Project project = getProjectOrThrow(projectId);
        User actor = currentUserService.getCurrentUser();
        projectAccessService.requireCanManageProject(project, actor);

        if (request.role() == ProjectRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot assign OWNER role to a member");
        }

        User user = userService.getUserById(request.userId());
        if (projectMemberRepository.existsByProjectAndUser(project, user)) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "User is already a project member");
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(request.role());
        projectMemberRepository.save(member);

        notificationService.notify(user, "ADDED_TO_PROJECT", "You were added to project: " + project.getName(),
                project.getId(), null);
        projectActivityService.record(project, actor, "MEMBER_ADDED", "Added " + user.getEmail());

        log.info("Member added to project {}: userId={}, role={}", projectId, user.getId(), request.role());
        return toMemberResponse(member);
    }

    @Transactional
    public MemberResponse updateMemberRole(Long projectId, Long userId, UpdateMemberRoleRequest request) {
        Project project = getProjectOrThrow(projectId);
        projectAccessService.requireCanManageProject(project, currentUserService.getCurrentUser());

        if (request.role() == ProjectRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot assign OWNER role to a member");
        }

        User user = userService.getUserById(userId);
        ProjectMember member = projectMemberRepository.findByProjectAndUser(project, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Member not found"));

        if (member.getRole() == ProjectRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot change owner's role");
        }

        member.setRole(request.role());
        return toMemberResponse(projectMemberRepository.save(member));
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        Project project = getProjectOrThrow(projectId);
        projectAccessService.requireCanManageProject(project, currentUserService.getCurrentUser());

        User user = userService.getUserById(userId);
        ProjectMember member = projectMemberRepository.findByProjectAndUser(project, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Member not found"));

        if (member.getRole() == ProjectRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot remove project owner");
        }

        projectMemberRepository.delete(member);
        log.info("Member removed from project {}: userId={}", projectId, userId);
    }

    @Transactional
    public ProjectResponse transferOwnership(Long projectId, TransferOwnershipRequest request) {
        Project project = getProjectOrThrow(projectId);
        User currentOwner = currentUserService.getCurrentUser();
        projectAccessService.requireCanManageProject(project, currentOwner);

        User newOwner = userService.getUserById(request.newOwnerUserId());
        if (newOwner.getId().equals(currentOwner.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "User is already the owner");
        }
        ProjectMember newOwnerMember = projectMemberRepository.findByProjectAndUser(project, newOwner)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST.value(), "New owner must be a project member"));

        ProjectMember oldOwnerMember = projectMemberRepository.findByProjectAndUser(project, currentOwner)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST.value(), "Current owner membership missing"));

        oldOwnerMember.setRole(ProjectRole.EDITOR);
        newOwnerMember.setRole(ProjectRole.OWNER);
        project.setOwner(newOwner);
        projectMemberRepository.save(oldOwnerMember);
        projectMemberRepository.save(newOwnerMember);
        projectRepository.save(project);

        notificationService.notify(newOwner, "OWNERSHIP_TRANSFERRED", "You are now owner of: " + project.getName(),
                project.getId(), null);
        projectActivityService.record(project, currentOwner, "OWNERSHIP_TRANSFERRED",
                "Ownership transferred to " + newOwner.getEmail());

        return ProjectResponse.from(project, ProjectRole.EDITOR);
    }

    public Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Project not found"));
    }

    private void createDefaultBoard(Project project, boolean withWipLimits) {
        Board board = new Board();
        board.setProject(project);
        board.setName("Main");
        board.setAccessMode(BoardAccessMode.OPEN);
        board.setCreatedBy(project.getOwner());
        board = boardRepository.save(board);

        createColumn(board, "Todo", 1, withWipLimits ? 5 : null, TaskStatus.BACKLOG);
        createColumn(board, "Doing", 2, withWipLimits ? 5 : null, TaskStatus.IN_PROGRESS);
        createColumn(board, "Done", 3, null, TaskStatus.DONE);
    }

    private void seedTemplate(Project project, ProjectTemplate template, boolean withBoard) {
        if (withBoard) {
            createDefaultBoard(project, template != ProjectTemplate.PERSONAL);
        }
        if (template == ProjectTemplate.SCRUM) {
            project.setDodTemplatesEnabled(true);
            project.setCustomWorkflowEnabled(true);
            seedDod(project, TaskType.FEATURE);
            seedWorkflow(project);
            Sprint sprint = new Sprint();
            sprint.setProject(project);
            sprint.setName("Sprint 1");
            sprint.setStatus(SprintStatus.ACTIVE);
            sprintRepository.save(sprint);
        } else if (template == ProjectTemplate.BUG_TRIAGE) {
            project.setDodTemplatesEnabled(true);
            seedDod(project, TaskType.BUG);
        }
        projectRepository.save(project);
    }

    private void seedDod(Project project, TaskType taskType) {
        String[] titles = {"Implementation complete", "Tests pass", "Documentation updated"};
        for (int i = 0; i < titles.length; i++) {
            DodTemplate template = new DodTemplate();
            template.setProject(project);
            template.setTaskType(taskType);
            template.setTitle(titles[i]);
            template.setPosition(i + 1);
            dodTemplateRepository.save(template);
        }
    }

    private void seedWorkflow(Project project) {
        addTransition(project, TaskStatus.BACKLOG, TaskStatus.IN_PROGRESS);
        addTransition(project, TaskStatus.IN_PROGRESS, TaskStatus.DONE);
        addTransition(project, TaskStatus.IN_PROGRESS, TaskStatus.BACKLOG);
        addTransition(project, TaskStatus.DONE, TaskStatus.ARCHIVED);
    }

    private void addTransition(Project project, TaskStatus from, TaskStatus to) {
        ProjectWorkflowTransition transition = new ProjectWorkflowTransition();
        transition.setProject(project);
        transition.setFromStatus(from);
        transition.setToStatus(to);
        workflowRepository.save(transition);
    }

    private void applySettingsFlags(
            Project project, Boolean strictBusinessRules, Boolean customWorkflowEnabled,
            java.math.BigDecimal capacityLimitHours, Integer slaWarningDays, Integer slaEscalateDays,
            Boolean dodTemplatesEnabled, Boolean requireApprovalForDone, Boolean priorityQueueRules,
            Integer maxUnfinishedSprintPercent, Integer autoArchiveDoneDays,
            Boolean restrictHighPriorityToOwner, Integer timeInStatusAlertsDays,
            Boolean blockDoneOnOpenRisks) {
        if (strictBusinessRules != null) project.setStrictBusinessRules(strictBusinessRules);
        if (customWorkflowEnabled != null) project.setCustomWorkflowEnabled(customWorkflowEnabled);
        if (capacityLimitHours != null) project.setCapacityLimitHours(capacityLimitHours);
        if (slaWarningDays != null) project.setSlaWarningDays(slaWarningDays);
        if (slaEscalateDays != null) project.setSlaEscalateDays(slaEscalateDays);
        if (dodTemplatesEnabled != null) project.setDodTemplatesEnabled(dodTemplatesEnabled);
        if (requireApprovalForDone != null) project.setRequireApprovalForDone(requireApprovalForDone);
        if (priorityQueueRules != null) project.setPriorityQueueRules(priorityQueueRules);
        if (maxUnfinishedSprintPercent != null) project.setMaxUnfinishedSprintPercent(maxUnfinishedSprintPercent);
        if (autoArchiveDoneDays != null) project.setAutoArchiveDoneDays(autoArchiveDoneDays);
        if (restrictHighPriorityToOwner != null) project.setRestrictHighPriorityToOwner(restrictHighPriorityToOwner);
        if (timeInStatusAlertsDays != null) project.setTimeInStatusAlertsDays(timeInStatusAlertsDays);
        if (blockDoneOnOpenRisks != null) project.setBlockDoneOnOpenRisks(blockDoneOnOpenRisks);
    }

    private void createColumn(Board board, String name, int position, Integer wipLimit, TaskStatus mappedStatus) {
        BoardColumn column = new BoardColumn();
        column.setBoard(board);
        column.setName(name);
        column.setPosition(position);
        column.setWipLimit(wipLimit);
        column.setMappedStatus(mappedStatus);
        boardColumnRepository.save(column);
    }

    private MemberResponse toMemberResponse(ProjectMember member) {
        return new MemberResponse(
                member.getUser().getId(),
                member.getUser().getEmail(),
                member.getUser().getName(),
                member.getRole()
        );
    }
}
