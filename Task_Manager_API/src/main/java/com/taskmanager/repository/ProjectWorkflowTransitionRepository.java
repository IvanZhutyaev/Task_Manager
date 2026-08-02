package com.taskmanager.repository;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectWorkflowTransition;
import com.taskmanager.domain.TaskStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectWorkflowTransitionRepository extends JpaRepository<ProjectWorkflowTransition, Long> {
    List<ProjectWorkflowTransition> findByProject(Project project);

    boolean existsByProjectAndFromStatusAndToStatus(Project project, TaskStatus from, TaskStatus to);

    void deleteByProject(Project project);
}
