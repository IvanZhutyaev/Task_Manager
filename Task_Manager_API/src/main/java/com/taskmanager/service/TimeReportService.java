package com.taskmanager.service;

import com.taskmanager.domain.Project;
import com.taskmanager.domain.Task;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.TimeReportResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimeReportService {
    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final ProjectAccessService accessService;
    private final CurrentUserService currentUserService;

    public TimeReportService(TaskRepository taskRepository, ProjectService projectService,
            ProjectAccessService accessService, CurrentUserService currentUserService) {
        this.taskRepository = taskRepository;
        this.projectService = projectService;
        this.accessService = accessService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public TimeReportResponse report(Long projectId) {
        Project project = projectService.getProjectOrThrow(projectId);
        accessService.requireCanRead(project, currentUserService.getCurrentUser());
        List<TimeReportResponse.Row> rows = taskRepository
                .findByColumn_Board_ProjectAndDeletedAtIsNull(project).stream()
                .filter(task -> task.getSpentHours() != null)
                .map(this::row)
                .toList();
        BigDecimal total = rows.stream().map(TimeReportResponse.Row::spentHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TimeReportResponse(projectId, total, rows);
    }

    @Transactional(readOnly = true)
    public String csv(Long projectId) {
        TimeReportResponse report = report(projectId);
        StringBuilder csv = new StringBuilder("taskId,taskTitle,assigneeId,assigneeName,spentHours\n");
        for (TimeReportResponse.Row row : report.rows()) {
            csv.append(row.taskId()).append(',').append(quote(row.taskTitle())).append(',')
                    .append(row.assigneeId() == null ? "" : row.assigneeId()).append(',')
                    .append(quote(row.assigneeName())).append(',').append(row.spentHours()).append('\n');
        }
        return csv.toString();
    }

    private TimeReportResponse.Row row(Task task) {
        return new TimeReportResponse.Row(task.getId(), task.getTitle(),
                task.getAssignee() == null ? null : task.getAssignee().getId(),
                task.getAssignee() == null ? null : task.getAssignee().getName(), task.getSpentHours());
    }

    private String quote(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }
}
