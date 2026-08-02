package com.taskmanager.web.api.dto;

import com.taskmanager.domain.Board;
import com.taskmanager.domain.BoardAccessMode;
import java.util.List;

public record BoardResponse(
        Long id,
        Long projectId,
        String name,
        BoardAccessMode accessMode,
        Long createdById,
        List<Long> teamIds
) {

    public static BoardResponse from(Board board) {
        return from(board, List.of());
    }

    public static BoardResponse from(Board board, List<Long> teamIds) {
        Long createdById = board.getCreatedBy() != null ? board.getCreatedBy().getId() : null;
        BoardAccessMode mode = board.getAccessMode() != null ? board.getAccessMode() : BoardAccessMode.OPEN;
        return new BoardResponse(
                board.getId(),
                board.getProject().getId(),
                board.getName(),
                mode,
                createdById,
                teamIds == null ? List.of() : teamIds
        );
    }
}
