package com.taskmanager.service;

import com.taskmanager.config.AiSuggestDodProperties;
import com.taskmanager.domain.Task;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.DodSuggestionResponse;
import com.taskmanager.web.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DodSuggestionService {

    private final AiSuggestDodProperties properties;
    private final DodSuggestionPort suggestionPort;
    private final TaskQueryService taskQueryService;
    private final BoardAccessService boardAccessService;
    private final CurrentUserService currentUserService;

    public DodSuggestionService(
            AiSuggestDodProperties properties,
            DodSuggestionPort suggestionPort,
            TaskQueryService taskQueryService,
            BoardAccessService boardAccessService,
            CurrentUserService currentUserService) {
        this.properties = properties;
        this.suggestionPort = suggestionPort;
        this.taskQueryService = taskQueryService;
        this.boardAccessService = boardAccessService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public DodSuggestionResponse suggest(Long taskId) {
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.NOT_FOUND.value(), "AI Suggest DoD is disabled");
        }
        Task task = taskQueryService.getTaskOrThrow(taskId);
        boardAccessService.requireCanViewBoard(task.getColumn().getBoard(), currentUserService.getCurrentUser());
        return suggestionPort.suggest(task);
    }
}
