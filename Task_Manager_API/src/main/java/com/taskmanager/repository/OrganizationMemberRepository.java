package com.taskmanager.repository;

import com.taskmanager.domain.Organization;
import com.taskmanager.domain.OrganizationMember;
import com.taskmanager.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    Optional<OrganizationMember> findByOrganizationAndUser(Organization organization, User user);

    List<OrganizationMember> findByOrganization(Organization organization);

    long countByOrganization(Organization organization);

    boolean existsByOrganizationAndUser(Organization organization, User user);
}
