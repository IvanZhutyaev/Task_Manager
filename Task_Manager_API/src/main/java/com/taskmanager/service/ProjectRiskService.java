package com.taskmanager.service;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectRisk;
import com.taskmanager.domain.RiskStatus;
import com.taskmanager.domain.Task;
import com.taskmanager.repository.ProjectRiskRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.RiskRequest;
import com.taskmanager.web.api.dto.RiskResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectRiskService {
    private final ProjectRiskRepository repository;
    private final ProjectService projectService;
    private final TaskQueryService taskQueryService;
    private final ProjectAccessService accessService;
    private final CurrentUserService currentUserService;

    public ProjectRiskService(ProjectRiskRepository repository, ProjectService projectService,
            TaskQueryService taskQueryService, ProjectAccessService accessService,
            CurrentUserService currentUserService) {
        this.repository = repository;
        this.projectService = projectService;
        this.taskQueryService = taskQueryService;
        this.accessService = accessService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<RiskResponse> list(Long projectId) {
        Project project = project(projectId, false);
        return repository.findByProjectOrderByCreatedAtDesc(project).stream().map(RiskResponse::from).toList();
    }

    @Transactional
    public RiskResponse create(Long projectId, RiskRequest request) {
        Project project = project(projectId, true);
        ProjectRisk risk = new ProjectRisk();
        risk.setProject(project);
        apply(risk, request, project);
        return RiskResponse.from(repository.save(risk));
    }

    @Transactional
    public RiskResponse update(Long projectId, Long riskId, RiskRequest request) {
        Project project = project(projectId, true);
        ProjectRisk risk = risk(project, riskId);
        apply(risk, request, project);
        return RiskResponse.from(repository.save(risk));
    }

    @Transactional
    public void delete(Long projectId, Long riskId) {
        repository.delete(risk(project(projectId, true), riskId));
    }

    private Project project(Long id, boolean write) {
        Project project = projectService.getProjectOrThrow(id);
        if (write) accessService.requireCanWriteContent(project, currentUserService.getCurrentUser());
        else accessService.requireCanRead(project, currentUserService.getCurrentUser());
        return project;
    }

    private ProjectRisk risk(Project project, Long id) {
        ProjectRisk risk = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Risk not found"));
        if (!risk.getProject().getId().equals(project.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND.value(), "Risk not found");
        }
        return risk;
    }

    private void apply(ProjectRisk risk, RiskRequest request, Project project) {
        risk.setTitle(request.title());
        risk.setDescription(request.description());
        risk.setStatus(request.status() == null ? RiskStatus.OPEN : request.status());
        risk.setUnblockAt(request.unblockAt());
        Task task = request.taskId() == null ? null : taskQueryService.getTaskOrThrow(request.taskId());
        if (task != null && !taskQueryService.getProject(task).getId().equals(project.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Task does not belong to project");
        }
        risk.setTask(task);
    }
}
