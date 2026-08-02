package com.taskmanager.repository;

import com.taskmanager.domain.Board;
import com.taskmanager.domain.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findByProjectOrderByCreatedAtAsc(Project project);
}
