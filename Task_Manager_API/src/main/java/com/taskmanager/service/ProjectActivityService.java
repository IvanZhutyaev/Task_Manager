package com.taskmanager.service;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectActivity;
import com.taskmanager.domain.User;
import com.taskmanager.repository.ProjectActivityRepository;
import com.taskmanager.web.api.dto.ActivityResponse;
import com.taskmanager.web.api.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectActivityService {

    private final ProjectActivityRepository projectActivityRepository;

    public ProjectActivityService(ProjectActivityRepository projectActivityRepository) {
        this.projectActivityRepository = projectActivityRepository;
    }

    @Transactional
    public void record(Project project, User actor, String action, String details) {
        ProjectActivity activity = new ProjectActivity();
        activity.setProject(project);
        activity.setActor(actor);
        activity.setAction(action);
        activity.setDetails(details);
        projectActivityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> list(Project project, int page, int size) {
        Page<ProjectActivity> result = projectActivityRepository.findByProjectOrderByCreatedAtDesc(
                project, PageRequest.of(page, size));
        return new PageResponse<>(
                result.getContent().stream().map(ActivityResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
