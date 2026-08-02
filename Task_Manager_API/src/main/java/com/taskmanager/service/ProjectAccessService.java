package com.taskmanager.service;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectMember;
import com.taskmanager.domain.ProjectRole;
import com.taskmanager.domain.Task;
import com.taskmanager.domain.User;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.web.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProjectAccessService {

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectAccessService(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
    }

    public ProjectMember requireMembership(Project project, User user) {
        return projectMemberRepository.findByProjectAndUser(project, user)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN.value(), "Access denied"));
    }

    public void requireCanManageProject(Project project, User user) {
        ProjectMember member = requireMembership(project, user);
        if (!member.canManageProject()) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Only project owner can manage this project");
        }
    }

    public void requireCanWriteContent(Project project, User user) {
        ProjectMember member = requireMembership(project, user);
        if (!member.canWriteContent()) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Insufficient permissions");
        }
    }

    /**
     * EDITOR/OWNER: full write. CONTRACTOR: only tasks assigned to them (or unassigned create later assigned to self).
     * VIEWER: denied.
     */
    public void requireCanWriteTask(Project project, User user, Task task) {
        ProjectMember member = requireMembership(project, user);
        if (member.canWriteContent()) {
            return;
        }
        if (member.isContractor()) {
            if (task.getAssignee() != null && task.getAssignee().getId().equals(user.getId())) {
                return;
            }
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Contractors can only modify their assigned tasks");
        }
        throw new ApiException(HttpStatus.FORBIDDEN.value(), "Insufficient permissions");
    }

    public void requireCanCreateTask(Project project, User user) {
        ProjectMember member = requireMembership(project, user);
        if (member.canWriteContent() || member.isContractor()) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN.value(), "Insufficient permissions");
    }

    public void requireCanRead(Project project, User user) {
        ProjectMember member = requireMembership(project, user);
        if (!member.canRead()) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Access denied");
        }
    }

    public boolean isContractor(Project project, User user) {
        return projectMemberRepository.findByProjectAndUser(project, user)
                .map(ProjectMember::isContractor)
                .orElse(false);
    }

    public ProjectRole requireRole(Project project, User user, ProjectRole minimumRole) {
        ProjectMember member = requireMembership(project, user);
        if (member.getRole() == ProjectRole.CONTRACTOR) {
            if (minimumRole == ProjectRole.CONTRACTOR || minimumRole == ProjectRole.VIEWER) {
                return member.getRole();
            }
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Access denied");
        }
        if (!hasAtLeastRole(member.getRole(), minimumRole)) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Access denied");
        }
        return member.getRole();
    }

    public boolean hasAtLeastRole(ProjectRole actual, ProjectRole required) {
        if (actual == ProjectRole.CONTRACTOR) {
            return required == ProjectRole.CONTRACTOR || required == ProjectRole.VIEWER;
        }
        if (required == ProjectRole.CONTRACTOR) {
            return actual.ordinal() <= ProjectRole.VIEWER.ordinal();
        }
        return actual.ordinal() <= required.ordinal();
    }
}
