package com.taskmanager.service;

import com.taskmanager.domain.Notification;
import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectMember;
import com.taskmanager.domain.User;
import com.taskmanager.repository.NotificationRepository;
import com.taskmanager.repository.ProjectMemberRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            ProjectMemberRepository projectMemberRepository) {
        this.notificationRepository = notificationRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional
    public void notify(User user, String type, String message, Long projectId, Long taskId) {
        if (user == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notification.setProjectId(projectId);
        notification.setTaskId(taskId);
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyProjectMembers(Project project, User except, String type, String message, Long projectId, Long taskId) {
        List<ProjectMember> members = projectMemberRepository.findByProject(project);
        for (ProjectMember member : members) {
            if (except != null && member.getUser().getId().equals(except.getId())) {
                continue;
            }
            notify(member.getUser(), type, message, projectId, taskId);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> listForUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public Notification markRead(Long id, User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new com.taskmanager.web.exception.ApiException(404, "Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new com.taskmanager.web.exception.ApiException(403, "Access denied");
        }
        notification.setRead(true);
        return notificationRepository.save(notification);
    }
}
