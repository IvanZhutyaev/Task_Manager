package com.taskmanager.service;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.Sprint;
import com.taskmanager.domain.SprintCloseAction;
import com.taskmanager.domain.SprintStatus;
import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskStatus;
import com.taskmanager.repository.SprintRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.CloseSprintRequest;
import com.taskmanager.web.api.dto.SprintRequest;
import com.taskmanager.web.api.dto.SprintMetricsResponse;
import com.taskmanager.web.api.dto.SprintResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final ProjectAccessService projectAccessService;
    private final CurrentUserService currentUserService;
    private final ProjectActivityService projectActivityService;

    public SprintService(
            SprintRepository sprintRepository,
            TaskRepository taskRepository,
            ProjectService projectService,
            ProjectAccessService projectAccessService,
            CurrentUserService currentUserService,
            ProjectActivityService projectActivityService) {
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.projectService = projectService;
        this.projectAccessService = projectAccessService;
        this.currentUserService = currentUserService;
        this.projectActivityService = projectActivityService;
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> list(Long projectId) {
        Project project = projectService.getProjectOrThrow(projectId);
        projectAccessService.requireCanRead(project, currentUserService.getCurrentUser());
        return sprintRepository.findByProjectOrderByCreatedAtDesc(project).stream()
                .map(SprintResponse::from)
                .toList();
    }

    @Transactional
    public SprintResponse create(Long projectId, SprintRequest request) {
        Project project = projectService.getProjectOrThrow(projectId);
        projectAccessService.requireCanWriteContent(project, currentUserService.getCurrentUser());
        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName(request.name());
        sprint.setStartDate(request.startDate());
        sprint.setEndDate(request.endDate());
        sprint.setStatus(SprintStatus.ACTIVE);
        sprint = sprintRepository.save(sprint);
        projectActivityService.record(project, currentUserService.getCurrentUser(), "SPRINT_CREATED", sprint.getName());
        return SprintResponse.from(sprint);
    }

    @Transactional
    public SprintResponse close(Long projectId, Long sprintId, CloseSprintRequest request) {
        Project project = projectService.getProjectOrThrow(projectId);
        projectAccessService.requireCanWriteContent(project, currentUserService.getCurrentUser());
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Sprint not found"));
        if (!sprint.getProject().getId().equals(project.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Sprint does not belong to project");
        }
        if (sprint.getStatus() == SprintStatus.CLOSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Sprint already closed");
        }
        List<Task> tasks = taskRepository.findBySprintAndDeletedAtIsNull(sprint);
        int unfinished = (int) tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE && task.getStatus() != TaskStatus.ARCHIVED)
                .count();
        int unfinishedPercent = tasks.isEmpty() ? 0 : unfinished * 100 / tasks.size();
        if (project.getMaxUnfinishedSprintPercent() != null
                && unfinishedPercent > project.getMaxUnfinishedSprintPercent()) {
            throw new ApiException(HttpStatus.CONFLICT.value(),
                    "Cannot close sprint: unfinished task percentage exceeds project limit");
        }
        SprintCloseAction action = request.unfinishedAction() != null
                ? request.unfinishedAction()
                : SprintCloseAction.MOVE_TO_BACKLOG;
        for (Task task : tasks) {
            if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.ARCHIVED) {
                continue;
            }
            if (action == SprintCloseAction.ARCHIVE) {
                try {
                    task.transitionTo(TaskStatus.ARCHIVED);
                } catch (IllegalStateException ex) {
                    task.setStatus(TaskStatus.ARCHIVED);
                }
            } else {
                try {
                    task.transitionTo(TaskStatus.BACKLOG);
                } catch (IllegalStateException ex) {
                    task.setStatus(TaskStatus.BACKLOG);
                }
            }
            task.setSprint(null);
            taskRepository.save(task);
        }
        sprint.setStatus(SprintStatus.CLOSED);
        sprint = sprintRepository.save(sprint);
        projectActivityService.record(project, currentUserService.getCurrentUser(), "SPRINT_CLOSED", sprint.getName());
        return SprintResponse.from(sprint);
    }

    @Transactional(readOnly = true)
    public SprintMetricsResponse metrics(Long projectId, Long sprintId) {
        Project project = projectService.getProjectOrThrow(projectId);
        projectAccessService.requireCanRead(project, currentUserService.getCurrentUser());
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Sprint not found"));
        if (!sprint.getProject().getId().equals(project.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND.value(), "Sprint not found");
        }
        List<Task> tasks = taskRepository.findBySprintAndDeletedAtIsNull(sprint);
        int completed = (int) tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.ARCHIVED)
                .count();
        int unfinished = tasks.size() - completed;
        BigDecimal estimated = tasks.stream().map(Task::getEstimateHours).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal spent = tasks.stream().map(Task::getSpentHours).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SprintMetricsResponse(sprintId, tasks.size(), completed, unfinished,
                tasks.isEmpty() ? 0 : unfinished * 100 / tasks.size(), estimated, spent);
    }
}
