package com.taskmanager.web.api.dto;

import java.time.Instant;

public record BoardEvent(
        String type,
        Long boardId,
        Long taskId,
        Instant at
) {
    public static BoardEvent of(String type, Long boardId, Long taskId) {
        return new BoardEvent(type, boardId, taskId, Instant.now());
    }
}
