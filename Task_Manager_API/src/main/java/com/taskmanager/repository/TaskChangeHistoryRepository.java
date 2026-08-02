package com.taskmanager.repository;

import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskChangeHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskChangeHistoryRepository extends JpaRepository<TaskChangeHistory, Long> {
    List<TaskChangeHistory> findByTaskOrderByCreatedAtDesc(Task task);
}
