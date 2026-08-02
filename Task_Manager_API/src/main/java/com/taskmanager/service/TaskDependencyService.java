package com.taskmanager.service;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskDependency;
import com.taskmanager.repository.TaskDependencyRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.DependencyRequest;
import com.taskmanager.web.api.dto.DependencyResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskDependencyService {

    private final TaskDependencyRepository dependencyRepository;
    private final TaskQueryService taskQueryService;
    private final ProjectAccessService projectAccessService;
    private final CurrentUserService currentUserService;

    public TaskDependencyService(
            TaskDependencyRepository dependencyRepository,
            TaskQueryService taskQueryService,
            ProjectAccessService projectAccessService,
            CurrentUserService currentUserService) {
        this.dependencyRepository = dependencyRepository;
        this.taskQueryService = taskQueryService;
        this.projectAccessService = projectAccessService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<DependencyResponse> list(Long taskId) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        projectAccessService.requireCanRead(taskQueryService.getProject(task), currentUserService.getCurrentUser());
        return dependencyRepository.findByTask(task).stream().map(DependencyResponse::from).toList();
    }

    @Transactional
    public DependencyResponse add(Long taskId, DependencyRequest request) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        Project project = taskQueryService.getProject(task);
        projectAccessService.requireCanWriteContent(project, currentUserService.getCurrentUser());
        Task blocker = taskQueryService.getTaskOrThrow(request.blockerId());
        if (!taskQueryService.getProject(blocker).getId().equals(project.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Blocker must be in the same project");
        }
        if (task.getId().equals(blocker.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Task cannot block itself");
        }
        if (dependencyRepository.existsByTaskAndBlocker(task, blocker)) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "Dependency already exists");
        }
        TaskDependency dependency = new TaskDependency();
        dependency.setTask(task);
        dependency.setBlocker(blocker);
        return DependencyResponse.from(dependencyRepository.save(dependency));
    }

    @Transactional
    public void remove(Long taskId, Long blockerId) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        projectAccessService.requireCanWriteContent(taskQueryService.getProject(task), currentUserService.getCurrentUser());
        Task blocker = taskQueryService.getTaskOrThrow(blockerId);
        dependencyRepository.deleteByTaskAndBlocker(task, blocker);
    }
}
