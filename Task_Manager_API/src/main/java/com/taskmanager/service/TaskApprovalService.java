package com.taskmanager.service;

import com.taskmanager.domain.ApprovalStatus;
import com.taskmanager.domain.Project;
import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskApproval;
import com.taskmanager.domain.User;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.TaskApprovalRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.ApprovalDecisionRequest;
import com.taskmanager.web.api.dto.ApprovalRequest;
import com.taskmanager.web.api.dto.ApprovalResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskApprovalService {
    private final TaskApprovalRepository repository;
    private final TaskQueryService taskQueryService;
    private final UserService userService;
    private final ProjectMemberRepository memberRepository;
    private final ProjectAccessService accessService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public TaskApprovalService(TaskApprovalRepository repository, TaskQueryService taskQueryService,
            UserService userService, ProjectMemberRepository memberRepository, ProjectAccessService accessService,
            CurrentUserService currentUserService, NotificationService notificationService) {
        this.repository = repository;
        this.taskQueryService = taskQueryService;
        this.userService = userService;
        this.memberRepository = memberRepository;
        this.accessService = accessService;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> list(Long taskId) {
        Task task = readableTask(taskId);
        return repository.findByTaskOrderByCreatedAtDesc(task).stream().map(ApprovalResponse::from).toList();
    }

    @Transactional
    public ApprovalResponse requestApproval(Long taskId, ApprovalRequest request) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        Project project = taskQueryService.getProject(task);
        accessService.requireCanWriteTask(project, currentUserService.getCurrentUser(), task);
        User approver = userService.getUserById(request.approverId());
        if (!memberRepository.existsByProjectAndUser(project, approver)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Approver must be a project member");
        }
        TaskApproval approval = new TaskApproval();
        approval.setTask(task);
        approval.setApprover(approver);
        approval.setStatus(ApprovalStatus.PENDING);
        approval.setComment(request.comment());
        approval = repository.save(approval);
        notificationService.notify(approver, "APPROVAL_REQUESTED",
                "Approval requested for task: " + task.getTitle(), project.getId(), task.getId());
        return ApprovalResponse.from(approval);
    }

    @Transactional
    public ApprovalResponse approve(Long taskId, Long approvalId, ApprovalDecisionRequest request) {
        return decide(taskId, approvalId, ApprovalStatus.APPROVED, request);
    }

    @Transactional
    public ApprovalResponse reject(Long taskId, Long approvalId, ApprovalDecisionRequest request) {
        return decide(taskId, approvalId, ApprovalStatus.REJECTED, request);
    }

    private ApprovalResponse decide(Long taskId, Long approvalId, ApprovalStatus status,
            ApprovalDecisionRequest request) {
        TaskApproval approval = repository.findById(approvalId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Approval not found"));
        if (!approval.getTask().getId().equals(taskId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Approval does not belong to task");
        }
        User current = currentUserService.getCurrentUser();
        if (!approval.getApprover().getId().equals(current.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Only the assigned approver can decide");
        }
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "Approval is already decided");
        }
        approval.setStatus(status);
        if (request != null && request.comment() != null) {
            approval.setComment(request.comment());
        }
        return ApprovalResponse.from(repository.save(approval));
    }

    private Task readableTask(Long taskId) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        Project project = taskQueryService.getProject(task);
        accessService.requireCanRead(project, currentUserService.getCurrentUser());
        return task;
    }
}
