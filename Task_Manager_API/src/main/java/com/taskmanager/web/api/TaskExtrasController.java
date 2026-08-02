package com.taskmanager.web.api;

import com.taskmanager.config.ApiConstants;
import com.taskmanager.service.DodSuggestionService;
import com.taskmanager.service.TaskChecklistService;
import com.taskmanager.service.TaskCommentService;
import com.taskmanager.service.TaskDependencyService;
import com.taskmanager.service.TaskApprovalService;
import com.taskmanager.service.TaskStatusHistoryService;
import com.taskmanager.service.TaskQueryService;
import com.taskmanager.web.api.dto.ChecklistItemRequest;
import com.taskmanager.web.api.dto.ChecklistItemResponse;
import com.taskmanager.web.api.dto.CommentRequest;
import com.taskmanager.web.api.dto.CommentResponse;
import com.taskmanager.web.api.dto.DependencyRequest;
import com.taskmanager.web.api.dto.DependencyResponse;
import com.taskmanager.web.api.dto.DodSuggestionResponse;
import com.taskmanager.web.api.dto.ApprovalRequest;
import com.taskmanager.web.api.dto.ApprovalDecisionRequest;
import com.taskmanager.web.api.dto.ApprovalResponse;
import com.taskmanager.web.api.dto.StatusHistoryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = ApiConstants.API_V1 + "/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
public class TaskExtrasController {

    private final TaskCommentService commentService;
    private final TaskChecklistService checklistService;
    private final TaskDependencyService dependencyService;
    private final TaskApprovalService approvalService;
    private final TaskStatusHistoryService statusHistoryService;
    private final TaskQueryService taskQueryService;
    private final DodSuggestionService dodSuggestionService;

    public TaskExtrasController(
            TaskCommentService commentService,
            TaskChecklistService checklistService,
            TaskDependencyService dependencyService,
            TaskApprovalService approvalService,
            TaskStatusHistoryService statusHistoryService,
            TaskQueryService taskQueryService,
            DodSuggestionService dodSuggestionService) {
        this.commentService = commentService;
        this.checklistService = checklistService;
        this.dependencyService = dependencyService;
        this.approvalService = approvalService;
        this.statusHistoryService = statusHistoryService;
        this.taskQueryService = taskQueryService;
        this.dodSuggestionService = dodSuggestionService;
    }

    @GetMapping("/{taskId}/comments")
    public List<CommentResponse> listComments(@PathVariable Long taskId) {
        return commentService.list(taskId);
    }

    @PostMapping("/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CommentRequest request) {
        return commentService.create(taskId, request);
    }

    @PutMapping("/comments/{commentId}")
    public CommentResponse updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) {
        return commentService.update(commentId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        commentService.delete(commentId);
    }

    @GetMapping("/{taskId}/checklist")
    public List<ChecklistItemResponse> listChecklist(@PathVariable Long taskId) {
        return checklistService.list(taskId);
    }

    @PostMapping("/{taskId}/checklist")
    @ResponseStatus(HttpStatus.CREATED)
    public ChecklistItemResponse createChecklistItem(
            @PathVariable Long taskId,
            @Valid @RequestBody ChecklistItemRequest request) {
        return checklistService.create(taskId, request);
    }

    @PostMapping("/{taskId}/checklist/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ChecklistItemResponse> createChecklistBulk(
            @PathVariable Long taskId,
            @Valid @RequestBody List<ChecklistItemRequest> requests) {
        return checklistService.createBulk(taskId, requests);
    }

    @PostMapping("/{taskId}/ai/suggest-dod")
    public DodSuggestionResponse suggestDod(@PathVariable Long taskId) {
        return dodSuggestionService.suggest(taskId);
    }

    @PutMapping("/checklist/{itemId}")
    public ChecklistItemResponse updateChecklistItem(
            @PathVariable Long itemId,
            @Valid @RequestBody ChecklistItemRequest request) {
        return checklistService.update(itemId, request);
    }

    @DeleteMapping("/checklist/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChecklistItem(@PathVariable Long itemId) {
        checklistService.delete(itemId);
    }

    @GetMapping("/{taskId}/dependencies")
    public List<DependencyResponse> listDependencies(@PathVariable Long taskId) {
        return dependencyService.list(taskId);
    }

    @PostMapping("/{taskId}/dependencies")
    @ResponseStatus(HttpStatus.CREATED)
    public DependencyResponse addDependency(
            @PathVariable Long taskId,
            @Valid @RequestBody DependencyRequest request) {
        return dependencyService.add(taskId, request);
    }

    @DeleteMapping("/{taskId}/dependencies/{blockerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeDependency(@PathVariable Long taskId, @PathVariable Long blockerId) {
        dependencyService.remove(taskId, blockerId);
    }

    @GetMapping("/{taskId}/status-history")
    public List<StatusHistoryResponse> statusHistory(@PathVariable Long taskId) {
        taskQueryService.getTask(taskId);
        return statusHistoryService.list(taskQueryService.getTaskOrThrow(taskId));
    }

    @GetMapping("/{taskId}/approvals")
    public List<ApprovalResponse> approvals(@PathVariable Long taskId) {
        return approvalService.list(taskId);
    }

    @PostMapping("/{taskId}/approvals")
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovalResponse requestApproval(
            @PathVariable Long taskId, @Valid @RequestBody ApprovalRequest request) {
        return approvalService.requestApproval(taskId, request);
    }

    @PostMapping("/{taskId}/approvals/{approvalId}/approve")
    public ApprovalResponse approve(@PathVariable Long taskId, @PathVariable Long approvalId,
            @RequestBody(required = false) ApprovalDecisionRequest request) {
        return approvalService.approve(taskId, approvalId,
                request == null ? new ApprovalDecisionRequest(null) : request);
    }

    @PostMapping("/{taskId}/approvals/{approvalId}/reject")
    public ApprovalResponse reject(@PathVariable Long taskId, @PathVariable Long approvalId,
            @RequestBody(required = false) ApprovalDecisionRequest request) {
        return approvalService.reject(taskId, approvalId,
                request == null ? new ApprovalDecisionRequest(null) : request);
    }
}
