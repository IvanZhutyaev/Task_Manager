package com.taskmanager.repository;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.ProjectActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectActivityRepository extends JpaRepository<ProjectActivity, Long> {
    Page<ProjectActivity> findByProjectOrderByCreatedAtDesc(Project project, Pageable pageable);
}
