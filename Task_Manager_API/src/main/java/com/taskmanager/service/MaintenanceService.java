package com.taskmanager.service;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskStatus;
import com.taskmanager.domain.TaskStatusHistory;
import com.taskmanager.domain.User;
import com.taskmanager.repository.NotificationRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.TaskStatusHistoryRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceService {
    private final TaskRepository taskRepository;
    private final TaskStatusHistoryRepository historyRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final TaskStatusHistoryService historyService;

    public MaintenanceService(TaskRepository taskRepository, TaskStatusHistoryRepository historyRepository,
            NotificationRepository notificationRepository, NotificationService notificationService,
            TaskStatusHistoryService historyService) {
        this.taskRepository = taskRepository;
        this.historyRepository = historyRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.historyService = historyService;
    }

    @Transactional
    public void runOnce() {
        processSla();
        autoArchiveDone();
        processTimeInStatus();
    }

    private void processSla() {
        for (Task task : taskRepository.findSlaCandidates(List.of(TaskStatus.DONE, TaskStatus.ARCHIVED))) {
            Project project = project(task);
            LocalDate today = LocalDate.now();
            if (project.getSlaEscalateDays() != null
                    && !today.isBefore(task.getDeadline().minusDays(project.getSlaEscalateDays()))) {
                notifyOnce(project.getOwner(), "SLA_ESCALATED", task, "SLA escalated: " + task.getTitle());
            } else if (project.getSlaWarningDays() != null
                    && !today.isBefore(task.getDeadline().minusDays(project.getSlaWarningDays()))) {
                notifyOnce(task.getAssignee(), "SLA_WARNING", task, "SLA warning: " + task.getTitle());
            }
        }
    }

    private void autoArchiveDone() {
        Instant now = Instant.now();
        for (Task task : taskRepository.findDoneCandidatesForAutoArchive(TaskStatus.DONE)) {
            Integer days = project(task).getAutoArchiveDoneDays();
            if (days == null) continue;
            historyRepository.findFirstByTaskAndLeftAtIsNullOrderByEnteredAtDesc(task)
                    .filter(entry -> entry.getStatus() == TaskStatus.DONE)
                    .filter(entry -> entry.getEnteredAt().plus(days, ChronoUnit.DAYS).isBefore(now))
                    .ifPresent(entry -> {
                        task.setStatus(TaskStatus.ARCHIVED);
                        taskRepository.save(task);
                        historyService.recordStatus(task, TaskStatus.ARCHIVED);
                    });
        }
    }

    private void processTimeInStatus() {
        Instant now = Instant.now();
        for (TaskStatusHistory entry : historyRepository.findByLeftAtIsNull()) {
            Task task = entry.getTask();
            Integer days = project(task).getTimeInStatusAlertsDays();
            if (days == null || entry.getStatus() == TaskStatus.ARCHIVED) continue;
            if (entry.getEnteredAt().plus(days, ChronoUnit.DAYS).isBefore(now)) {
                notifyOnce(task.getAssignee(), "TIME_IN_STATUS_ALERT", task,
                        "Task has remained in " + entry.getStatus() + ": " + task.getTitle());
            }
        }
    }

    private void notifyOnce(User user, String type, Task task, String message) {
        if (user != null && !notificationRepository.existsByUserAndTypeAndTaskId(user, type, task.getId())) {
            notificationService.notify(user, type, message, project(task).getId(), task.getId());
        }
    }

    private Project project(Task task) {
        return task.getColumn().getBoard().getProject();
    }
}
