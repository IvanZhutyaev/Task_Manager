package com.taskmanager.service;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectRole;
import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskComment;
import com.taskmanager.domain.User;
import com.taskmanager.repository.TaskCommentRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.CommentRequest;
import com.taskmanager.web.api.dto.CommentResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskCommentService {

    private final TaskCommentRepository commentRepository;
    private final TaskQueryService taskQueryService;
    private final ProjectAccessService projectAccessService;
    private final CurrentUserService currentUserService;

    public TaskCommentService(
            TaskCommentRepository commentRepository,
            TaskQueryService taskQueryService,
            ProjectAccessService projectAccessService,
            CurrentUserService currentUserService) {
        this.commentRepository = commentRepository;
        this.taskQueryService = taskQueryService;
        this.projectAccessService = projectAccessService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long taskId) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        projectAccessService.requireCanRead(taskQueryService.getProject(task), currentUserService.getCurrentUser());
        return commentRepository.findByTaskOrderByCreatedAtAsc(task).stream().map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse create(Long taskId, CommentRequest request) {
        Task task = taskQueryService.getTaskOrThrow(taskId);
        User user = currentUserService.getCurrentUser();
        projectAccessService.requireCanRead(taskQueryService.getProject(task), user);
        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setAuthor(user);
        comment.setBody(request.body());
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse update(Long commentId, CommentRequest request) {
        TaskComment comment = getOrThrow(commentId);
        User user = currentUserService.getCurrentUser();
        Project project = taskQueryService.getProject(comment.getTask());
        ProjectRole role = projectAccessService.requireMembership(project, user).getRole();
        if (!comment.getAuthor().getId().equals(user.getId()) && role != ProjectRole.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Only author or owner can edit comment");
        }
        comment.setBody(request.body());
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public void delete(Long commentId) {
        TaskComment comment = getOrThrow(commentId);
        User user = currentUserService.getCurrentUser();
        Project project = taskQueryService.getProject(comment.getTask());
        ProjectRole role = projectAccessService.requireMembership(project, user).getRole();
        if (!comment.getAuthor().getId().equals(user.getId()) && role != ProjectRole.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Only author or owner can delete comment");
        }
        commentRepository.delete(comment);
    }

    private TaskComment getOrThrow(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Comment not found"));
    }
}
