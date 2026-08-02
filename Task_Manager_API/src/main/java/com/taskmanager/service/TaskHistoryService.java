package com.taskmanager.service;

import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskChangeHistory;
import com.taskmanager.repository.TaskChangeHistoryRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.TaskHistoryEntryResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskHistoryService {

    private final TaskChangeHistoryRepository taskChangeHistoryRepository;
    private final TaskQueryService taskQueryService;
    private final ProjectAccessService projectAccessService;
    private final CurrentUserService currentUserService;

    public TaskHistoryService(
            TaskChangeHistoryRepository taskChangeHistoryRepository,
            TaskQueryService taskQueryService,
            ProjectAccessService projectAccessService,
            CurrentUserService currentUserService) {
        this.taskChangeHistoryRepository = taskChangeHistoryRepository;
        this.taskQueryService = taskQueryService;
        this.projectAccessService = projectAccessService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<TaskHistoryEntryResponse> getTaskHistory(Long taskId) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        projectAccessService.requireCanRead(taskQueryService.getProject(task), currentUserService.getCurrentUser());
        return taskChangeHistoryRepository.findByTaskOrderByCreatedAtDesc(task).stream()
                .map(TaskHistoryEntryResponse::from)
                .toList();
    }
}
