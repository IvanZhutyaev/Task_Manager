package com.taskmanager.web.api;

import com.taskmanager.config.ApiConstants;
import com.taskmanager.domain.TaskPriority;
import com.taskmanager.domain.TaskStatus;
import com.taskmanager.domain.TaskType;
import com.taskmanager.service.TaskService;
import com.taskmanager.web.api.dto.MoveTaskRequest;
import com.taskmanager.web.api.dto.PageResponse;
import com.taskmanager.web.api.dto.TaskHistoryEntryResponse;
import com.taskmanager.web.api.dto.TaskRequest;
import com.taskmanager.web.api.dto.TaskResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = ApiConstants.API_V1, produces = MediaType.APPLICATION_JSON_VALUE)
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/columns/{columnId}/tasks")
    public PageResponse<TaskResponse> listTasks(
            @PathVariable Long columnId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskType type,
            @RequestParam(required = false) Long labelId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return taskService.listTasks(columnId, assigneeId, priority, status, type, labelId, q, page, size, sortBy, sortDir);
    }

    @GetMapping("/projects/{projectId}/tasks")
    public PageResponse<TaskResponse> searchProjectTasks(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskType type,
            @RequestParam(required = false) Long labelId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return taskService.searchInProject(
                projectId, assigneeId, priority, status, type, labelId, q, page, size, sortBy, sortDir);
    }

    @PostMapping("/columns/{columnId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @PathVariable Long columnId,
            @Valid @RequestBody TaskRequest request) {
        return taskService.createTask(columnId, request);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse getTask(@PathVariable Long taskId) {
        return taskService.getTask(taskId);
    }

    @GetMapping("/tasks/{taskId}/history")
    public List<TaskHistoryEntryResponse> getTaskHistory(@PathVariable Long taskId) {
        return taskService.getTaskHistory(taskId);
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponse updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequest request) {
        return taskService.updateTask(taskId, request);
    }

    @PatchMapping("/tasks/{taskId}/move")
    public TaskResponse moveTask(
            @PathVariable Long taskId,
            @Valid @RequestBody MoveTaskRequest request) {
        return taskService.moveTask(taskId, request);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
    }
}
