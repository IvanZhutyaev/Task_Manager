package com.taskmanager.repository;

import com.taskmanager.domain.Notification;
import com.taskmanager.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndTypeAndTaskId(User user, String type, Long taskId);
}
