package com.taskmanager.repository;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectMember;
import com.taskmanager.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectAndUser(Project project, User user);

    List<ProjectMember> findByProject(Project project);

    void deleteByProjectAndUser(Project project, User user);

    boolean existsByProjectAndUser(Project project, User user);
}
