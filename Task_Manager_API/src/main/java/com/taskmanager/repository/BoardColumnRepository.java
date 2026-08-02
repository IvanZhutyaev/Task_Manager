package com.taskmanager.repository;

import com.taskmanager.domain.Board;
import com.taskmanager.domain.BoardColumn;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, Long> {

    List<BoardColumn> findByBoardOrderByPositionAsc(Board board);

    @Query("SELECT COALESCE(MAX(c.position), 0) FROM BoardColumn c WHERE c.board = :board")
    int findMaxPositionByBoard(@Param("board") Board board);
}
