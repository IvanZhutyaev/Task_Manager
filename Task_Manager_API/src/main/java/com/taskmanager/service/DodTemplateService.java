package com.taskmanager.service;

import com.taskmanager.domain.DodTemplate;
import com.taskmanager.domain.Project;
import com.taskmanager.repository.DodTemplateRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.DodTemplateRequest;
import com.taskmanager.web.api.dto.DodTemplateResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DodTemplateService {
    private final DodTemplateRepository repository;
    private final ProjectService projectService;
    private final ProjectAccessService accessService;
    private final CurrentUserService currentUserService;

    public DodTemplateService(DodTemplateRepository repository, ProjectService projectService,
            ProjectAccessService accessService, CurrentUserService currentUserService) {
        this.repository = repository;
        this.projectService = projectService;
        this.accessService = accessService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<DodTemplateResponse> list(Long projectId) {
        Project project = projectService.getProjectOrThrow(projectId);
        accessService.requireCanManageProject(project, currentUserService.getCurrentUser());
        return repository.findByProjectOrderByTaskTypeAscPositionAsc(project).stream()
                .map(DodTemplateResponse::from).toList();
    }

    @Transactional
    public List<DodTemplateResponse> replace(Long projectId, List<DodTemplateRequest> requests) {
        Project project = projectService.getProjectOrThrow(projectId);
        accessService.requireCanManageProject(project, currentUserService.getCurrentUser());
        repository.deleteByProject(project);
        repository.flush();
        int fallbackPosition = 1;
        for (DodTemplateRequest request : requests) {
            DodTemplate template = new DodTemplate();
            template.setProject(project);
            template.setTaskType(request.taskType());
            template.setTitle(request.title());
            template.setPosition(request.position() == null ? fallbackPosition++ : request.position());
            repository.save(template);
        }
        project.setDodTemplatesEnabled(!requests.isEmpty());
        return repository.findByProjectOrderByTaskTypeAscPositionAsc(project).stream()
                .map(DodTemplateResponse::from).toList();
    }
}
