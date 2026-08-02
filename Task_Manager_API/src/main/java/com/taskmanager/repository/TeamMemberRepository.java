package com.taskmanager.repository;

import com.taskmanager.domain.Team;
import com.taskmanager.domain.TeamMember;
import com.taskmanager.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByTeam(Team team);

    Optional<TeamMember> findByTeamAndUser(Team team, User user);

    boolean existsByTeamAndUser(Team team, User user);

    void deleteByTeamAndUser(Team team, User user);

    @Query("""
            select count(tm) > 0 from TeamMember tm
            join BoardTeam bt on bt.team = tm.team
            where bt.board.id = :boardId and tm.user.id = :userId
            """)
    boolean existsOnBoardTeams(@Param("boardId") Long boardId, @Param("userId") Long userId);
}
