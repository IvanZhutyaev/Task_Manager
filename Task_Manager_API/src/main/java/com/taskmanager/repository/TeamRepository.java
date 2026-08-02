package com.taskmanager.repository;

import com.taskmanager.domain.Organization;
import com.taskmanager.domain.Team;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByOrganizationOrderByCreatedAtAsc(Organization organization);

    Optional<Team> findByIdAndOrganization(Long id, Organization organization);

    long countByOrganization(Organization organization);
}
