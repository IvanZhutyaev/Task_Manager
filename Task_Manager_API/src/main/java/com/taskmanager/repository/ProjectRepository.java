package com.taskmanager.repository;

import com.taskmanager.domain.Organization;
import com.taskmanager.domain.Project;
import com.taskmanager.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("""
            SELECT DISTINCT p FROM Project p
            JOIN ProjectMember pm ON pm.project = p
            WHERE pm.user = :user
            ORDER BY p.createdAt DESC
            """)
    List<Project> findAllByMember(@Param("user") User user);

    long countByOrganization(Organization organization);
}
