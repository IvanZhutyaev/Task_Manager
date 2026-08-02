package com.taskmanager.repository;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectRisk;
import com.taskmanager.domain.RiskStatus;
import com.taskmanager.domain.Task;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRiskRepository extends JpaRepository<ProjectRisk, Long> {
    List<ProjectRisk> findByProjectOrderByCreatedAtDesc(Project project);

    boolean existsByTaskAndStatus(Task task, RiskStatus status);

    List<ProjectRisk> findByStatus(RiskStatus status);
}
