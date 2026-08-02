package com.taskmanager.repository;

import com.taskmanager.domain.InvitationStatus;
import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectInvitation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, Long> {
    List<ProjectInvitation> findByProjectOrderByCreatedAtDesc(Project project);

    Optional<ProjectInvitation> findByProjectAndEmailIgnoreCaseAndStatus(
            Project project, String email, InvitationStatus status);

    List<ProjectInvitation> findByEmailIgnoreCaseAndStatus(String email, InvitationStatus status);
}
