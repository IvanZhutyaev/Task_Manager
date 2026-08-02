package com.taskmanager.repository;

import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskDependency;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {
    List<TaskDependency> findByTask(Task task);

    boolean existsByTaskAndBlocker(Task task, Task blocker);

    void deleteByTaskAndBlocker(Task task, Task blocker);
}
