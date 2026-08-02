package com.taskmanager.service;

import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskChecklistItem;
import com.taskmanager.repository.TaskChecklistItemRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.ChecklistItemRequest;
import com.taskmanager.web.api.dto.ChecklistItemResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskChecklistService {

    private final TaskChecklistItemRepository checklistItemRepository;
    private final TaskQueryService taskQueryService;
    private final ProjectAccessService projectAccessService;
    private final CurrentUserService currentUserService;
    private final BoardEventPublisher boardEventPublisher;

    public TaskChecklistService(
            TaskChecklistItemRepository checklistItemRepository,
            TaskQueryService taskQueryService,
            ProjectAccessService projectAccessService,
            CurrentUserService currentUserService,
            BoardEventPublisher boardEventPublisher) {
        this.checklistItemRepository = checklistItemRepository;
        this.taskQueryService = taskQueryService;
        this.projectAccessService = projectAccessService;
        this.currentUserService = currentUserService;
        this.boardEventPublisher = boardEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> list(Long taskId) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        projectAccessService.requireCanRead(taskQueryService.getProject(task), currentUserService.getCurrentUser());
        return checklistItemRepository.findByTaskOrderByPositionAsc(task).stream()
                .map(ChecklistItemResponse::from)
                .toList();
    }

    @Transactional
    public ChecklistItemResponse create(Long taskId, ChecklistItemRequest request) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        projectAccessService.requireCanWriteContent(taskQueryService.getProject(task), currentUserService.getCurrentUser());
        TaskChecklistItem item = new TaskChecklistItem();
        item.setTask(task);
        item.setTitle(request.title());
        item.setDone(Boolean.TRUE.equals(request.done()));
        item.setPosition(checklistItemRepository.findMaxPosition(task) + 1);
        ChecklistItemResponse response = ChecklistItemResponse.from(checklistItemRepository.save(item));
        boardEventPublisher.publishAfterCommit(
                "CHECKLIST_CREATED", task.getColumn().getBoard().getId(), task.getId());
        return response;
    }

    @Transactional
    public List<ChecklistItemResponse> createBulk(Long taskId, List<ChecklistItemRequest> requests) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        projectAccessService.requireCanWriteContent(taskQueryService.getProject(task), currentUserService.getCurrentUser());
        List<ChecklistItemResponse> created = new ArrayList<>();
        int position = checklistItemRepository.findMaxPosition(task);
        for (ChecklistItemRequest request : requests) {
            TaskChecklistItem item = new TaskChecklistItem();
            item.setTask(task);
            item.setTitle(request.title());
            item.setDone(Boolean.TRUE.equals(request.done()));
            item.setPosition(++position);
            created.add(ChecklistItemResponse.from(checklistItemRepository.save(item)));
        }
        boardEventPublisher.publishAfterCommit(
                "CHECKLIST_BULK_CREATED", task.getColumn().getBoard().getId(), task.getId());
        return created;
    }

    @Transactional
    public ChecklistItemResponse update(Long itemId, ChecklistItemRequest request) {
        TaskChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Checklist item not found"));
        projectAccessService.requireCanWriteContent(
                taskQueryService.getProject(item.getTask()), currentUserService.getCurrentUser());
        item.setTitle(request.title());
        if (request.done() != null) {
            item.setDone(request.done());
        }
        return ChecklistItemResponse.from(checklistItemRepository.save(item));
    }

    @Transactional
    public void delete(Long itemId) {
        TaskChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Checklist item not found"));
        projectAccessService.requireCanWriteContent(
                taskQueryService.getProject(item.getTask()), currentUserService.getCurrentUser());
        checklistItemRepository.delete(item);
    }
}
