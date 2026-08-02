package com.taskmanager.service;

import com.taskmanager.domain.Task;
import com.taskmanager.web.api.dto.DodSuggestionResponse;

public interface DodSuggestionPort {
    DodSuggestionResponse suggest(Task task);
}
