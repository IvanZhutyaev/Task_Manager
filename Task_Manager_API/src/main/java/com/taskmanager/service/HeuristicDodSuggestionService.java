package com.taskmanager.service;

import com.taskmanager.domain.DodTemplate;
import com.taskmanager.domain.Project;
import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskType;
import com.taskmanager.repository.DodTemplateRepository;
import com.taskmanager.web.api.dto.DodSuggestionResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class HeuristicDodSuggestionService implements DodSuggestionPort {

    private final DodTemplateRepository dodTemplateRepository;

    public HeuristicDodSuggestionService(DodTemplateRepository dodTemplateRepository) {
        this.dodTemplateRepository = dodTemplateRepository;
    }

    @Override
    public DodSuggestionResponse suggest(Task task) {
        Project project = task.getColumn().getBoard().getProject();
        TaskType type = task.getTaskType() != null ? task.getTaskType() : inferType(task.getTitle());
        Set<String> titles = new LinkedHashSet<>();

        if (project.isDodTemplatesEnabled()) {
            for (DodTemplate template : dodTemplateRepository.findByProjectAndTaskTypeOrderByPositionAsc(project, type)) {
                titles.add(template.getTitle());
            }
        }

        titles.addAll(heuristicFor(type, task.getTitle()));

        List<DodSuggestionResponse.SuggestedItem> items = new ArrayList<>();
        int position = 1;
        for (String title : titles) {
            items.add(new DodSuggestionResponse.SuggestedItem(title, position++));
        }
        return new DodSuggestionResponse(items);
    }

    private TaskType inferType(String title) {
        String lower = title == null ? "" : title.toLowerCase(Locale.ROOT);
        if (lower.contains("bug") || lower.contains("fix") || lower.contains("ошибк") || lower.contains("баг")) {
            return TaskType.BUG;
        }
        if (lower.contains("chore") || lower.contains("refactor") || lower.contains("техдолг")) {
            return TaskType.CHORE;
        }
        return TaskType.FEATURE;
    }

    private List<String> heuristicFor(TaskType type, String title) {
        return switch (type) {
            case BUG -> List.of(
                    "Reproduce the bug",
                    "Root cause identified",
                    "Fix applied and verified",
                    "Regression checks passed"
            );
            case CHORE -> List.of(
                    "Scope of change documented",
                    "Change applied safely",
                    "No unrelated diffs",
                    "Verification steps completed"
            );
            case FEATURE -> List.of(
                    "Acceptance criteria covered",
                    "Implementation complete",
                    "Tests pass",
                    "Documentation updated"
            );
        };
    }
}
