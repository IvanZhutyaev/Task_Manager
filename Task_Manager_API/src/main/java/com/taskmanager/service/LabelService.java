package com.taskmanager.service;

import com.taskmanager.domain.Label;
import com.taskmanager.domain.Project;
import com.taskmanager.repository.LabelRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.LabelRequest;
import com.taskmanager.web.api.dto.LabelResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LabelService {

    private final LabelRepository labelRepository;
    private final ProjectService projectService;
    private final ProjectAccessService projectAccessService;
    private final CurrentUserService currentUserService;

    public LabelService(
            LabelRepository labelRepository,
            ProjectService projectService,
            ProjectAccessService projectAccessService,
            CurrentUserService currentUserService) {
        this.labelRepository = labelRepository;
        this.projectService = projectService;
        this.projectAccessService = projectAccessService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> list(Long projectId) {
        Project project = projectService.getProjectOrThrow(projectId);
        projectAccessService.requireCanRead(project, currentUserService.getCurrentUser());
        return labelRepository.findByProjectOrderByNameAsc(project).stream().map(LabelResponse::from).toList();
    }

    @Transactional
    public LabelResponse create(Long projectId, LabelRequest request) {
        Project project = projectService.getProjectOrThrow(projectId);
        projectAccessService.requireCanWriteContent(project, currentUserService.getCurrentUser());
        Label label = new Label();
        label.setProject(project);
        label.setName(request.name());
        label.setColor(request.color() != null ? request.color() : "#888888");
        return LabelResponse.from(labelRepository.save(label));
    }

    @Transactional
    public void delete(Long projectId, Long labelId) {
        Project project = projectService.getProjectOrThrow(projectId);
        projectAccessService.requireCanWriteContent(project, currentUserService.getCurrentUser());
        Label label = labelRepository.findByIdAndProject(labelId, project)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Label not found"));
        labelRepository.delete(label);
    }
}
