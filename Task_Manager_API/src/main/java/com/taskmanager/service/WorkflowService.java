package com.taskmanager.service;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectWorkflowTransition;
import com.taskmanager.repository.ProjectWorkflowTransitionRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.WorkflowTransitionRequest;
import com.taskmanager.web.api.dto.WorkflowTransitionResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {
    private final ProjectWorkflowTransitionRepository repository;
    private final ProjectService projectService;
    private final ProjectAccessService accessService;
    private final CurrentUserService currentUserService;

    public WorkflowService(ProjectWorkflowTransitionRepository repository, ProjectService projectService,
            ProjectAccessService accessService, CurrentUserService currentUserService) {
        this.repository = repository;
        this.projectService = projectService;
        this.accessService = accessService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<WorkflowTransitionResponse> list(Long projectId) {
        Project project = projectService.getProjectOrThrow(projectId);
        accessService.requireCanManageProject(project, currentUserService.getCurrentUser());
        return repository.findByProject(project).stream().map(WorkflowTransitionResponse::from).toList();
    }

    @Transactional
    public List<WorkflowTransitionResponse> replace(Long projectId, List<WorkflowTransitionRequest> requests) {
        Project project = projectService.getProjectOrThrow(projectId);
        accessService.requireCanManageProject(project, currentUserService.getCurrentUser());
        Set<String> unique = new HashSet<>();
        for (WorkflowTransitionRequest request : requests) {
            if (request.fromStatus() == request.toStatus()
                    || !unique.add(request.fromStatus() + ":" + request.toStatus())) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Invalid or duplicate workflow transition");
            }
        }
        repository.deleteByProject(project);
        repository.flush();
        for (WorkflowTransitionRequest request : requests) {
            ProjectWorkflowTransition transition = new ProjectWorkflowTransition();
            transition.setProject(project);
            transition.setFromStatus(request.fromStatus());
            transition.setToStatus(request.toStatus());
            repository.save(transition);
        }
        project.setCustomWorkflowEnabled(!requests.isEmpty());
        return repository.findByProject(project).stream().map(WorkflowTransitionResponse::from).toList();
    }
}
