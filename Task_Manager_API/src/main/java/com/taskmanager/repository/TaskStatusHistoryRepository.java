package com.taskmanager.repository;

import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskStatusHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskStatusHistoryRepository extends JpaRepository<TaskStatusHistory, Long> {
    List<TaskStatusHistory> findByTaskOrderByEnteredAtAsc(Task task);

    Optional<TaskStatusHistory> findFirstByTaskAndLeftAtIsNullOrderByEnteredAtDesc(Task task);

    List<TaskStatusHistory> findByLeftAtIsNull();
}
