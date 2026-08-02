package com.taskmanager.service;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectGoal;
import com.taskmanager.domain.Task;
import com.taskmanager.repository.ProjectGoalRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.GoalRequest;
import com.taskmanager.web.api.dto.GoalResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectGoalService {
    private final ProjectGoalRepository repository;
    private final ProjectService projectService;
    private final TaskQueryService taskQueryService;
    private final ProjectAccessService accessService;
    private final CurrentUserService currentUserService;

    public ProjectGoalService(ProjectGoalRepository repository, ProjectService projectService,
            TaskQueryService taskQueryService, ProjectAccessService accessService,
            CurrentUserService currentUserService) {
        this.repository = repository;
        this.projectService = projectService;
        this.taskQueryService = taskQueryService;
        this.accessService = accessService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(Long projectId) {
        Project project = readableProject(projectId);
        return repository.findByProjectOrderByCreatedAtDesc(project).stream().map(GoalResponse::from).toList();
    }

    @Transactional
    public GoalResponse create(Long projectId, GoalRequest request) {
        Project project = writableProject(projectId);
        ProjectGoal goal = new ProjectGoal();
        goal.setProject(project);
        apply(goal, request);
        return GoalResponse.from(repository.save(goal));
    }

    @Transactional
    public GoalResponse update(Long projectId, Long goalId, GoalRequest request) {
        ProjectGoal goal = goal(projectId, goalId, true);
        apply(goal, request);
        return GoalResponse.from(repository.save(goal));
    }

    @Transactional
    public void delete(Long projectId, Long goalId) {
        repository.delete(goal(projectId, goalId, true));
    }

    @Transactional
    public GoalResponse linkTask(Long projectId, Long goalId, Long taskId) {
        ProjectGoal goal = goal(projectId, goalId, true);
        Task task = taskQueryService.getTaskOrThrow(taskId);
        requireSameProject(goal.getProject(), task);
        goal.getTasks().add(task);
        return GoalResponse.from(repository.save(goal));
    }

    @Transactional
    public GoalResponse unlinkTask(Long projectId, Long goalId, Long taskId) {
        ProjectGoal goal = goal(projectId, goalId, true);
        goal.getTasks().removeIf(task -> task.getId().equals(taskId));
        return GoalResponse.from(repository.save(goal));
    }

    private ProjectGoal goal(Long projectId, Long goalId, boolean write) {
        Project project = write ? writableProject(projectId) : readableProject(projectId);
        ProjectGoal goal = repository.findById(goalId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Goal not found"));
        if (!goal.getProject().getId().equals(project.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND.value(), "Goal not found");
        }
        return goal;
    }

    private Project readableProject(Long id) {
        Project project = projectService.getProjectOrThrow(id);
        accessService.requireCanRead(project, currentUserService.getCurrentUser());
        return project;
    }

    private Project writableProject(Long id) {
        Project project = projectService.getProjectOrThrow(id);
        accessService.requireCanWriteContent(project, currentUserService.getCurrentUser());
        return project;
    }

    private void requireSameProject(Project project, Task task) {
        if (!taskQueryService.getProject(task).getId().equals(project.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Task does not belong to project");
        }
    }

    private void apply(ProjectGoal goal, GoalRequest request) {
        goal.setTitle(request.title());
        goal.setDescription(request.description());
    }
}
