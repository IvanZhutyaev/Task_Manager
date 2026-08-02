package com.taskmanager.service;

import com.taskmanager.domain.TaskPriority;
import com.taskmanager.domain.TaskStatus;
import com.taskmanager.domain.TaskType;
import com.taskmanager.web.api.dto.MoveTaskRequest;
import com.taskmanager.web.api.dto.PageResponse;
import com.taskmanager.web.api.dto.TaskHistoryEntryResponse;
import com.taskmanager.web.api.dto.TaskRequest;
import com.taskmanager.web.api.dto.TaskResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskQueryService taskQueryService;
    private final TaskCommandService taskCommandService;
    private final TaskHistoryService taskHistoryService;

    public TaskService(
            TaskQueryService taskQueryService,
            TaskCommandService taskCommandService,
            TaskHistoryService taskHistoryService) {
        this.taskQueryService = taskQueryService;
        this.taskCommandService = taskCommandService;
        this.taskHistoryService = taskHistoryService;
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
        return taskQueryService.listTasks(
                columnId, assigneeId, priority, status, taskType, labelId, q, page, size, sortBy, sortDir);
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
        return taskQueryService.searchInProject(
                projectId, assigneeId, priority, status, taskType, labelId, q, page, size, sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId) {
        return taskQueryService.getTask(taskId);
    }

    @Transactional(readOnly = true)
    public List<TaskHistoryEntryResponse> getTaskHistory(Long taskId) {
        return taskHistoryService.getTaskHistory(taskId);
    }

    @Transactional
    public TaskResponse createTask(Long columnId, TaskRequest request) {
        return taskCommandService.createTask(columnId, request);
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request) {
        return taskCommandService.updateTask(taskId, request);
    }

    @Transactional
    public TaskResponse moveTask(Long taskId, MoveTaskRequest request) {
        return taskCommandService.moveTask(taskId, request);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        taskCommandService.deleteTask(taskId);
    }
}
