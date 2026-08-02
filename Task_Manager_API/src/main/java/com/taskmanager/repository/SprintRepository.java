package com.taskmanager.repository;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.Sprint;
import com.taskmanager.domain.SprintStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProjectOrderByCreatedAtDesc(Project project);

    List<Sprint> findByProjectAndStatus(Project project, SprintStatus status);
}
