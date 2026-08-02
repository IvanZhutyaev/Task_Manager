package com.taskmanager.service;

import com.taskmanager.domain.BoardColumn;
import com.taskmanager.domain.Label;
import com.taskmanager.domain.Project;
import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskPriority;
import com.taskmanager.domain.TaskStatus;
import com.taskmanager.domain.TaskType;
import com.taskmanager.domain.User;
import com.taskmanager.repository.LabelRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.PageResponse;
import com.taskmanager.web.api.dto.TaskResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TaskQueryService {

    private final TaskRepository taskRepository;
    private final ColumnService columnService;
    private final ProjectAccessService projectAccessService;
    private final BoardAccessService boardAccessService;
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final ProjectService projectService;
    private final LabelRepository labelRepository;

    public TaskQueryService(
            TaskRepository taskRepository,
            ColumnService columnService,
            ProjectAccessService projectAccessService,
            BoardAccessService boardAccessService,
            CurrentUserService currentUserService,
            UserService userService,
            ProjectService projectService,
            LabelRepository labelRepository) {
        this.taskRepository = taskRepository;
        this.columnService = columnService;
        this.projectAccessService = projectAccessService;
        this.boardAccessService = boardAccessService;
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.projectService = projectService;
        this.labelRepository = labelRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listTasks(
            Long columnId,
            Long assigneeId,
            TaskPriority priority,
            TaskStatus status,
            TaskType taskType,
            Long labelId,
            String q,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        BoardColumn column = columnService.getColumnOrThrow(columnId);
        Project project = column.getBoard().getProject();
        User current = currentUserService.getCurrentUser();
        boardAccessService.requireCanViewBoard(column.getBoard(), current);
        Long effectiveAssigneeId = forceContractorAssignee(project, current, assigneeId);

        Page<Task> taskPage = taskRepository.findFilteredPageable(
                column,
                resolveAssignee(effectiveAssigneeId),
                priority,
                status,
                taskType,
                resolveLabel(labelId, project),
                // Empty string (not null): PostgreSQL binds null String as bytea and breaks LOWER(:q).
                StringUtils.hasText(q) ? q.trim() : "",
                pageable(page, size, sortBy, sortDir));
        return toPage(taskPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> searchInProject(
            Long projectId,
            Long assigneeId,
            TaskPriority priority,
            TaskStatus status,
            TaskType taskType,
            Long labelId,
            String q,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        Project project = projectService.getProjectOrThrow(projectId);
        User current = currentUserService.getCurrentUser();
        projectAccessService.requireCanRead(project, current);
        Long effectiveAssigneeId = forceContractorAssignee(project, current, assigneeId);
        Page<Task> taskPage = taskRepository.findInProject(
                project,
                resolveAssignee(effectiveAssigneeId),
                priority,
                status,
                taskType,
                resolveLabel(labelId, project),
                StringUtils.hasText(q) ? q.trim() : "",
                pageable(page, size, sortBy, sortDir));
        return toPage(taskPage);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId) {
        Task task = getTaskOrThrow(taskId);
        Project project = getProject(task);
        User current = currentUserService.getCurrentUser();
        boardAccessService.requireCanViewBoard(task.getColumn().getBoard(), current);
        if (projectAccessService.isContractor(project, current)
                && (task.getAssignee() == null || !task.getAssignee().getId().equals(current.getId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Access denied");
        }
        return TaskResponse.from(task);
    }

    private Long forceContractorAssignee(Project project, User current, Long assigneeId) {
        if (projectAccessService.isContractor(project, current)) {
            return current.getId();
        }
        return assigneeId;
    }

    public Task getTaskOrThrow(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Task not found"));
        if (task.isDeleted()) {
            throw new ApiException(HttpStatus.NOT_FOUND.value(), "Task not found");
        }
        return task;
    }

    public Project getProject(Task task) {
        return task.getColumn().getBoard().getProject();
    }

    private User resolveAssignee(Long assigneeId) {
        return assigneeId == null ? null : userService.getUserById(assigneeId);
    }

    private Label resolveLabel(Long labelId, Project project) {
        if (labelId == null) {
            return null;
        }
        return labelRepository.findByIdAndProject(labelId, project)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST.value(), "Label not found"));
    }

    private Pageable pageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy == null ? "createdAt" : sortBy);
        return PageRequest.of(page, size, sort);
    }

    private PageResponse<TaskResponse> toPage(Page<Task> taskPage) {
        List<TaskResponse> content = taskPage.getContent().stream().map(TaskResponse::from).toList();
        return new PageResponse<>(content, taskPage.getNumber(), taskPage.getSize(),
                taskPage.getTotalElements(), taskPage.getTotalPages());
    }
}
