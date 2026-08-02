package com.taskmanager.service;

import com.taskmanager.domain.ApprovalStatus;
import com.taskmanager.domain.BoardColumn;
import com.taskmanager.domain.DodTemplate;
import com.taskmanager.domain.Label;
import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectRole;
import com.taskmanager.domain.RiskStatus;
import com.taskmanager.domain.Sprint;
import com.taskmanager.domain.SprintStatus;
import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskChangeHistory;
import com.taskmanager.domain.TaskChecklistItem;
import com.taskmanager.domain.TaskDependency;
import com.taskmanager.domain.TaskPriority;
import com.taskmanager.domain.TaskStatus;
import com.taskmanager.domain.TaskType;
import com.taskmanager.domain.User;
import com.taskmanager.repository.DodTemplateRepository;
import com.taskmanager.repository.LabelRepository;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.ProjectRiskRepository;
import com.taskmanager.repository.ProjectWorkflowTransitionRepository;
import com.taskmanager.repository.SprintRepository;
import com.taskmanager.repository.TaskApprovalRepository;
import com.taskmanager.repository.TaskChangeHistoryRepository;
import com.taskmanager.repository.TaskChecklistItemRepository;
import com.taskmanager.repository.TaskDependencyRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.MoveTaskRequest;
import com.taskmanager.web.api.dto.TaskRequest;
import com.taskmanager.web.api.dto.TaskResponse;
import com.taskmanager.web.exception.ApiException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskCommandService {

    private static final Logger log = LoggerFactory.getLogger(TaskCommandService.class);
    private static final List<TaskStatus> TERMINAL = List.of(TaskStatus.DONE, TaskStatus.ARCHIVED);

    private final TaskRepository taskRepository;
    private final ColumnService columnService;
    private final ProjectAccessService projectAccessService;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskChangeHistoryRepository taskChangeHistoryRepository;
    private final TaskChecklistItemRepository checklistItemRepository;
    private final TaskDependencyRepository dependencyRepository;
    private final LabelRepository labelRepository;
    private final SprintRepository sprintRepository;
    private final ProjectWorkflowTransitionRepository workflowRepository;
    private final DodTemplateRepository dodTemplateRepository;
    private final TaskApprovalRepository approvalRepository;
    private final ProjectRiskRepository riskRepository;
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final TaskQueryService taskQueryService;
    private final BusinessRules businessRules;
    private final NotificationService notificationService;
    private final ProjectActivityService projectActivityService;
    private final TaskStatusHistoryService statusHistoryService;
    private final BoardAccessService boardAccessService;
    private final BoardEventPublisher boardEventPublisher;

    public TaskCommandService(
            TaskRepository taskRepository,
            ColumnService columnService,
            ProjectAccessService projectAccessService,
            ProjectMemberRepository projectMemberRepository,
            TaskChangeHistoryRepository taskChangeHistoryRepository,
            TaskChecklistItemRepository checklistItemRepository,
            TaskDependencyRepository dependencyRepository,
            LabelRepository labelRepository,
            SprintRepository sprintRepository,
            ProjectWorkflowTransitionRepository workflowRepository,
            DodTemplateRepository dodTemplateRepository,
            TaskApprovalRepository approvalRepository,
            ProjectRiskRepository riskRepository,
            CurrentUserService currentUserService,
            UserService userService,
            TaskQueryService taskQueryService,
            BusinessRules businessRules,
            NotificationService notificationService,
            ProjectActivityService projectActivityService,
            TaskStatusHistoryService statusHistoryService,
            BoardAccessService boardAccessService,
            BoardEventPublisher boardEventPublisher) {
        this.taskRepository = taskRepository;
        this.columnService = columnService;
        this.projectAccessService = projectAccessService;
        this.projectMemberRepository = projectMemberRepository;
        this.taskChangeHistoryRepository = taskChangeHistoryRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.dependencyRepository = dependencyRepository;
        this.labelRepository = labelRepository;
        this.sprintRepository = sprintRepository;
        this.workflowRepository = workflowRepository;
        this.dodTemplateRepository = dodTemplateRepository;
        this.approvalRepository = approvalRepository;
        this.riskRepository = riskRepository;
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.taskQueryService = taskQueryService;
        this.businessRules = businessRules;
        this.notificationService = notificationService;
        this.projectActivityService = projectActivityService;
        this.statusHistoryService = statusHistoryService;
        this.boardAccessService = boardAccessService;
        this.boardEventPublisher = boardEventPublisher;
    }

    @Transactional
    public TaskResponse createTask(Long columnId, TaskRequest request) {
        BoardColumn column = columnService.getColumnOrThrow(columnId);
        Project project = column.getBoard().getProject();
        User current = currentUserService.getCurrentUser();
        projectAccessService.requireCanCreateTask(project, current);
        boardAccessService.requireCanWriteBoard(column.getBoard(), current);

        enforceWipLimit(column);
        validateCreateUpdatePolicies(project, current, request, null);

        Task task = new Task();
        task.setColumn(column);
        applyTaskFields(task, request, project);
        if (projectAccessService.isContractor(project, current) && task.getAssignee() == null) {
            task.setAssignee(current);
        }

        TaskStatus initial = request.status() != null ? request.status() : TaskStatus.BACKLOG;
        task.setStatus(TaskStatus.BACKLOG);
        if (initial != TaskStatus.BACKLOG) {
            enforceStatusTransition(project, TaskStatus.BACKLOG, initial);
        }
        if (initial == TaskStatus.DONE) {
            enforceDoneRules(project, task, request);
        }
        enforceCapacity(project, task.getAssignee(), request.estimateHours(), null);
        enforcePriorityQueue(project, task.getAssignee(), initial, request.priority(), null);

        task.setStatus(initial);
        task = taskRepository.save(task);
        statusHistoryService.recordStatus(task, initial);
        applyDodTemplates(project, task);
        recordHistory(task, "CREATED", "Task created in column " + columnId);
        projectActivityService.record(project, current, "TASK_CREATED", "Task #" + task.getId() + " created");
        if (task.getAssignee() != null) {
            notificationService.notify(task.getAssignee(), "ASSIGNED", "You were assigned to task: " + task.getTitle(),
                    project.getId(), task.getId());
        }
        log.info("Task created: id={}, columnId={}", task.getId(), columnId);
        boardEventPublisher.publishAfterCommit("TASK_CREATED", column.getBoard().getId(), task.getId());
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        Project project = taskQueryService.getProject(task);
        User current = currentUserService.getCurrentUser();
        projectAccessService.requireCanWriteTask(project, current, task);
        boardAccessService.requireCanWriteBoard(task.getColumn().getBoard(), current);

        validateCreateUpdatePolicies(project, current, request, task);
        User previousAssignee = task.getAssignee();
        TaskStatus previousStatus = task.getStatus();

        applyTaskFields(task, request, project);
        TaskStatus nextStatus = request.status() != null ? request.status() : task.getStatus();
        enforceCapacity(project, task.getAssignee(), request.estimateHours() != null ? request.estimateHours() : task.getEstimateHours(), task.getId());
        enforcePriorityQueue(project, task.getAssignee(), nextStatus,
                request.priority() != null ? request.priority() : task.getPriority(), task.getId());

        if (request.status() != null && request.status() != previousStatus) {
            enforceStatusTransition(project, previousStatus, request.status());
            if (request.status() == TaskStatus.DONE) {
                enforceDoneRules(project, task, request);
            }
            try {
                task.transitionTo(request.status());
            } catch (IllegalStateException ex) {
                if (project.isCustomWorkflowEnabled()) {
                    task.setStatus(request.status());
                } else {
                    throw new ApiException(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
                }
            }
        }
        task = taskRepository.save(task);
        if (previousStatus != task.getStatus()) {
            statusHistoryService.recordStatus(task, task.getStatus());
        }
        recordHistory(task, "UPDATED", "Task updated");
        if (task.getAssignee() != null && (previousAssignee == null || !previousAssignee.getId().equals(task.getAssignee().getId()))) {
            notificationService.notify(task.getAssignee(), "ASSIGNED", "You were assigned to task: " + task.getTitle(),
                    project.getId(), task.getId());
        }
        if (previousStatus != task.getStatus()) {
            notificationService.notifyProjectMembers(project, current, "STATUS_CHANGED",
                    "Task \"" + task.getTitle() + "\" status: " + task.getStatus(), project.getId(), task.getId());
        }
        boardEventPublisher.publishAfterCommit("TASK_UPDATED", task.getColumn().getBoard().getId(), task.getId());
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse moveTask(Long taskId, MoveTaskRequest request) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        Project project = taskQueryService.getProject(task);
        User current = currentUserService.getCurrentUser();
        projectAccessService.requireCanWriteTask(project, current, task);
        boardAccessService.requireCanWriteBoard(task.getColumn().getBoard(), current);

        if (task.getStatus() == TaskStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Archived tasks cannot be moved");
        }

        if (businessRules.isStrict(project)) {
            ProjectRole role = projectAccessService.requireMembership(project, current).getRole();
            boolean editorOrAbove = role == ProjectRole.OWNER || role == ProjectRole.EDITOR;
            if (!editorOrAbove && !task.canBeMovedBy(current) && role != ProjectRole.CONTRACTOR) {
                throw new ApiException(HttpStatus.FORBIDDEN.value(), "Only assignee or editors can move this task");
            }
        }

        BoardColumn targetColumn = columnService.getColumnOrThrow(request.columnId());
        if (!targetColumn.getBoard().getId().equals(task.getColumn().getBoard().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Task can only be moved within the same board");
        }

        if (!targetColumn.getId().equals(task.getColumn().getId())) {
            enforceWipLimit(targetColumn);
        }

        TaskStatus previousStatus = task.getStatus();
        TaskStatus targetStatus = targetColumn.getMappedStatus() != null
                ? targetColumn.getMappedStatus()
                : TaskStatus.IN_PROGRESS;

        if (targetStatus != previousStatus) {
            enforceStatusTransition(project, previousStatus, targetStatus);
        }
        if (targetStatus == TaskStatus.DONE) {
            enforceDoneRules(project, task, null);
        }
        enforcePriorityQueue(project, task.getAssignee(), targetStatus, task.getPriority(), task.getId());

        try {
            task.transitionTo(targetStatus);
        } catch (IllegalStateException ex) {
            if (project.isCustomWorkflowEnabled()) {
                task.setStatus(targetStatus);
            } else {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
            }
        }

        if (businessRules.isStrict(project)
                && targetStatus == TaskStatus.IN_PROGRESS
                && task.getAssignee() == null) {
            task.setAssignee(current);
        }

        task.setColumn(targetColumn);
        task = taskRepository.save(task);
        if (previousStatus != task.getStatus()) {
            statusHistoryService.recordStatus(task, task.getStatus());
        }
        recordHistory(task, "MOVED", "Task moved to column " + request.columnId());
        log.info("Task moved: id={}, columnId={}", taskId, request.columnId());
        boardEventPublisher.publishAfterCommit("TASK_MOVED", task.getColumn().getBoard().getId(), task.getId());
        return TaskResponse.from(task);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        Project project = taskQueryService.getProject(task);
        User current = currentUserService.getCurrentUser();
        projectAccessService.requireCanWriteTask(project, current, task);
        boardAccessService.requireCanWriteBoard(task.getColumn().getBoard(), current);
        task.softDelete();
        taskRepository.save(task);
        recordHistory(task, "DELETED", "Task soft deleted");
        log.info("Task deleted: id={}", taskId);
        boardEventPublisher.publishAfterCommit("TASK_DELETED", task.getColumn().getBoard().getId(), taskId);
    }

    private void applyTaskFields(Task task, TaskRequest request, Project project) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDeadline(request.deadline());
        task.setEstimateHours(request.estimateHours());
        task.setSpentHours(request.spentHours());
        task.setTaskType(request.taskType());
        task.setAssignee(resolveAssignee(request.assigneeId(), project));
        task.setSprint(resolveSprint(request.sprintId(), project));
        task.setLabels(resolveLabels(request.labelIds(), project));
    }

    private void validateCreateUpdatePolicies(Project project, User current, TaskRequest request, Task existing) {
        if (project.isRestrictHighPriorityToOwner()
                && request.priority() == TaskPriority.HIGH) {
            ProjectRole role = projectAccessService.requireMembership(project, current).getRole();
            if (role != ProjectRole.OWNER) {
                throw new ApiException(HttpStatus.FORBIDDEN.value(), "Only owner can set HIGH priority");
            }
        }
        if (businessRules.isStrict(project)) {
            if (request.deadline() != null && request.deadline().isBefore(LocalDate.now())) {
                ProjectRole role = projectAccessService.requireMembership(project, current).getRole();
                if (role != ProjectRole.OWNER) {
                    throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Only owner can set a past deadline");
                }
            }
            if (request.taskType() == TaskType.BUG && request.priority() != null && request.priority() != TaskPriority.HIGH) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Bug tasks must have HIGH priority");
            }
            if (existing != null
                    && existing.getStatus() == TaskStatus.IN_PROGRESS
                    && existing.getAssignee() != null
                    && request.assigneeId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot remove assignee from an in-progress task");
            }
        }
    }

    private void enforceStatusTransition(Project project, TaskStatus from, TaskStatus to) {
        if (from == to) {
            return;
        }
        if (!project.isCustomWorkflowEnabled()) {
            return;
        }
        if (!workflowRepository.existsByProjectAndFromStatusAndToStatus(project, from, to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                    "Transition " + from + " → " + to + " is not allowed by project workflow");
        }
    }

    private void enforceCapacity(Project project, User assignee, BigDecimal newEstimate, Long excludeTaskId) {
        if (project.getCapacityLimitHours() == null || assignee == null) {
            return;
        }
        BigDecimal estimate = newEstimate != null ? newEstimate : BigDecimal.ZERO;
        BigDecimal current = taskRepository.sumActiveEstimateHours(project, assignee, excludeTaskId, TERMINAL);
        if (current == null) {
            current = BigDecimal.ZERO;
        }
        if (current.add(estimate).compareTo(project.getCapacityLimitHours()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                    "Assignee capacity exceeded (limit " + project.getCapacityLimitHours() + "h)");
        }
    }

    private void enforcePriorityQueue(
            Project project, User assignee, TaskStatus status, TaskPriority priority, Long excludeTaskId) {
        if (!project.isPriorityQueueRules() || assignee == null) {
            return;
        }
        if (status == TaskStatus.IN_PROGRESS && priority == TaskPriority.HIGH) {
            long count = taskRepository.countByAssigneeStatusPriority(
                    project, assignee, TaskStatus.IN_PROGRESS, TaskPriority.HIGH, excludeTaskId);
            if (count >= 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                        "Priority queue: assignee already has a HIGH task in progress");
            }
        }
    }

    private void applyDodTemplates(Project project, Task task) {
        if (!project.isDodTemplatesEnabled() || task.getTaskType() == null) {
            return;
        }
        List<DodTemplate> templates = dodTemplateRepository.findByProjectAndTaskTypeOrderByPositionAsc(
                project, task.getTaskType());
        int pos = 1;
        for (DodTemplate template : templates) {
            TaskChecklistItem item = new TaskChecklistItem();
            item.setTask(task);
            item.setTitle(template.getTitle());
            item.setDone(false);
            item.setPosition(pos++);
            checklistItemRepository.save(item);
        }
    }

    private void enforceDoneRules(Project project, Task task, TaskRequest request) {
        if (task.getId() != null) {
            List<TaskDependency> deps = dependencyRepository.findByTask(task);
            for (TaskDependency dep : deps) {
                TaskStatus blockerStatus = dep.getBlocker().getStatus();
                if (blockerStatus != TaskStatus.DONE && blockerStatus != TaskStatus.ARCHIVED) {
                    throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                            "Cannot complete task while blocker #" + dep.getBlocker().getId() + " is open");
                }
            }
            if (businessRules.isStrict(project) && checklistItemRepository.countIncomplete(task) > 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot complete task with incomplete checklist items");
            }
            if (project.isRequireApprovalForDone()
                    && !approvalRepository.existsByTaskAndStatus(task, ApprovalStatus.APPROVED)) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Task requires approval before DONE");
            }
            boolean blockRisks = project.isBlockDoneOnOpenRisks() || businessRules.isStrict(project);
            if (blockRisks && riskRepository.existsByTaskAndStatus(task, RiskStatus.OPEN)) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot complete task with open risks");
            }
        }

        if (!businessRules.isStrict(project)) {
            return;
        }

        LocalDate deadline = request != null && request.deadline() != null ? request.deadline() : task.getDeadline();
        if (deadline == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot complete task without a deadline");
        }
        if (deadline.isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot complete an overdue task");
        }

        BigDecimal spent = request != null && request.spentHours() != null ? request.spentHours() : task.getSpentHours();
        if (spent == null || spent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot complete task without spent hours");
        }
    }

    private void enforceWipLimit(BoardColumn column) {
        if (column.getWipLimit() == null) {
            return;
        }
        long count = taskRepository.countByColumnAndDeletedAtIsNull(column);
        if (count >= column.getWipLimit()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                    "WIP limit reached for column (" + column.getWipLimit() + ")");
        }
    }

    private void recordHistory(Task task, String action, String details) {
        TaskChangeHistory history = new TaskChangeHistory();
        history.setTask(task);
        history.setChangedBy(currentUserService.getCurrentUser());
        history.setAction(action);
        history.setDetails(details);
        taskChangeHistoryRepository.save(history);
    }

    private User resolveAssignee(Long assigneeId, Project project) {
        if (assigneeId == null) {
            return null;
        }
        User assignee = userService.getUserById(assigneeId);
        if (!projectMemberRepository.existsByProjectAndUser(project, assignee)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Assignee must be a project member");
        }
        return assignee;
    }

    private Sprint resolveSprint(Long sprintId, Project project) {
        if (sprintId == null) {
            return null;
        }
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Sprint not found"));
        if (!sprint.getProject().getId().equals(project.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Sprint must belong to the same project");
        }
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot assign task to a closed sprint");
        }
        return sprint;
    }

    private Set<Label> resolveLabels(List<Long> labelIds, Project project) {
        if (labelIds == null || labelIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Label> labels = new HashSet<>();
        for (Long labelId : labelIds) {
            Label label = labelRepository.findByIdAndProject(labelId, project)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST.value(), "Label not found in project"));
            labels.add(label);
        }
        return labels;
    }
}
