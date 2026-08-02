package com.taskmanager.repository;

import com.taskmanager.domain.Label;
import com.taskmanager.domain.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, Long> {
    List<Label> findByProjectOrderByNameAsc(Project project);

    Optional<Label> findByIdAndProject(Long id, Project project);
}
