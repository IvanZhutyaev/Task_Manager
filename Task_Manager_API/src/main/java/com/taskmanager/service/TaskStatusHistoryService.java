package com.taskmanager.service;

import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskStatus;
import com.taskmanager.domain.TaskStatusHistory;
import com.taskmanager.repository.TaskStatusHistoryRepository;
import com.taskmanager.web.api.dto.StatusHistoryResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskStatusHistoryService {

    private final TaskStatusHistoryRepository historyRepository;

    public TaskStatusHistoryService(TaskStatusHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional
    public void recordStatus(Task task, TaskStatus newStatus) {
        Instant now = Instant.now();
        historyRepository.findFirstByTaskAndLeftAtIsNullOrderByEnteredAtDesc(task).ifPresent(open -> {
            open.setLeftAt(now);
            historyRepository.save(open);
        });
        TaskStatusHistory entry = new TaskStatusHistory();
        entry.setTask(task);
        entry.setStatus(newStatus);
        entry.setEnteredAt(now);
        historyRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> list(Task task) {
        return historyRepository.findByTaskOrderByEnteredAtAsc(task).stream()
                .map(StatusHistoryResponse::from)
                .toList();
    }
}
