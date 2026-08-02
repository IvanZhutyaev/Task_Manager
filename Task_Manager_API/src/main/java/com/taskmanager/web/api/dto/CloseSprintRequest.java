package com.taskmanager.web.api.dto;

import com.taskmanager.domain.SprintCloseAction;

public record CloseSprintRequest(SprintCloseAction unfinishedAction) {
}
