package com.taskmanager.repository;

import com.taskmanager.domain.Board;
import com.taskmanager.domain.BoardTeam;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardTeamRepository extends JpaRepository<BoardTeam, Long> {

    List<BoardTeam> findByBoard(Board board);

    void deleteByBoard(Board board);
}
