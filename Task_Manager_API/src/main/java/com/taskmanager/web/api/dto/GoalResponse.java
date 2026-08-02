package com.taskmanager.web.api.dto;

import com.taskmanager.domain.ProjectGoal;
import com.taskmanager.domain.TaskStatus;
import java.time.Instant;
import java.util.List;

public record GoalResponse(
        Long id, String title, String description, Instant createdAt,
        List<Long> taskIds, int totalTasks, int completedTasks, int progressPercent
) {
    public static GoalResponse from(ProjectGoal goal) {
        int total = goal.getTasks().size();
        int completed = (int) goal.getTasks().stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.ARCHIVED)
                .count();
        return new GoalResponse(goal.getId(), goal.getTitle(), goal.getDescription(), goal.getCreatedAt(),
                goal.getTasks().stream().map(task -> task.getId()).sorted().toList(),
                total, completed, total == 0 ? 0 : completed * 100 / total);
    }
}
