package com.taskmanager.repository;

import com.taskmanager.domain.Organization;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    @Query("""
            select o from Organization o
            join OrganizationMember m on m.organization = o
            where m.user.id = :userId
            order by o.createdAt asc
            """)
    List<Organization> findAllByMemberUserId(@Param("userId") Long userId);
}
