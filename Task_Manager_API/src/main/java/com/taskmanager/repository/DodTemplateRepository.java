package com.taskmanager.repository;

import com.taskmanager.domain.DodTemplate;
import com.taskmanager.domain.Project;
import com.taskmanager.domain.TaskType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DodTemplateRepository extends JpaRepository<DodTemplate, Long> {
    List<DodTemplate> findByProjectAndTaskTypeOrderByPositionAsc(Project project, TaskType taskType);

    List<DodTemplate> findByProjectOrderByTaskTypeAscPositionAsc(Project project);

    void deleteByProject(Project project);
}
