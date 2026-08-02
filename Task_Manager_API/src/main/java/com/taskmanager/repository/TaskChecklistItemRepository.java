package com.taskmanager.repository;

import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskChecklistItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskChecklistItemRepository extends JpaRepository<TaskChecklistItem, Long> {
    List<TaskChecklistItem> findByTaskOrderByPositionAsc(Task task);

    @Query("SELECT COUNT(i) FROM TaskChecklistItem i WHERE i.task = :task AND i.done = false")
    long countIncomplete(@Param("task") Task task);

    @Query("SELECT COALESCE(MAX(i.position), 0) FROM TaskChecklistItem i WHERE i.task = :task")
    int findMaxPosition(@Param("task") Task task);
}
