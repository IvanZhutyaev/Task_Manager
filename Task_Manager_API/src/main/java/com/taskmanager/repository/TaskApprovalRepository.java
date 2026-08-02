package com.taskmanager.repository;

import com.taskmanager.domain.ApprovalStatus;
import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskApproval;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskApprovalRepository extends JpaRepository<TaskApproval, Long> {
    List<TaskApproval> findByTaskOrderByCreatedAtDesc(Task task);

    boolean existsByTaskAndStatus(Task task, ApprovalStatus status);
}
