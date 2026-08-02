package com.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectMember;
import com.taskmanager.domain.ProjectRole;
import com.taskmanager.domain.User;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.web.exception.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectAccessService projectAccessService;

    private Project project;
    private User user;
    private ProjectMember ownerMember;
    private ProjectMember editorMember;
    private ProjectMember viewerMember;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);

        user = new User();
        user.setId(10L);

        ownerMember = member(ProjectRole.OWNER);
        editorMember = member(ProjectRole.EDITOR);
        viewerMember = member(ProjectRole.VIEWER);
    }

    private ProjectMember member(ProjectRole role) {
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    @Test
    void ownerCanManageProject() {
        when(projectMemberRepository.findByProjectAndUser(project, user)).thenReturn(Optional.of(ownerMember));
        projectAccessService.requireCanManageProject(project, user);
    }

    @Test
    void editorCannotManageProject() {
        when(projectMemberRepository.findByProjectAndUser(project, user)).thenReturn(Optional.of(editorMember));
        ApiException ex = assertThrows(ApiException.class,
                () -> projectAccessService.requireCanManageProject(project, user));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void editorCanWriteContent() {
        when(projectMemberRepository.findByProjectAndUser(project, user)).thenReturn(Optional.of(editorMember));
        projectAccessService.requireCanWriteContent(project, user);
    }

    @Test
    void viewerCannotWriteContent() {
        when(projectMemberRepository.findByProjectAndUser(project, user)).thenReturn(Optional.of(viewerMember));
        ApiException ex = assertThrows(ApiException.class,
                () -> projectAccessService.requireCanWriteContent(project, user));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void nonMemberHasNoAccess() {
        when(projectMemberRepository.findByProjectAndUser(project, user)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class,
                () -> projectAccessService.requireCanRead(project, user));
        assertEquals(403, ex.getStatus());
    }
}
