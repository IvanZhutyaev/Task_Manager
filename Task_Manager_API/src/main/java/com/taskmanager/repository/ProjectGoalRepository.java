package com.taskmanager.repository;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectGoal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectGoalRepository extends JpaRepository<ProjectGoal, Long> {
    List<ProjectGoal> findByProjectOrderByCreatedAtDesc(Project project);
}
