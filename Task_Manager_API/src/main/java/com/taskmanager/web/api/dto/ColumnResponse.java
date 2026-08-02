package com.taskmanager.web.api.dto;

import com.taskmanager.domain.BoardColumn;
import com.taskmanager.domain.TaskStatus;

public record ColumnResponse(
        Long id,
        Long boardId,
        String name,
        int position,
        Integer wipLimit,
        TaskStatus mappedStatus
) {

    public static ColumnResponse from(BoardColumn column) {
        return new ColumnResponse(
                column.getId(),
                column.getBoard().getId(),
                column.getName(),
                column.getPosition(),
                column.getWipLimit(),
                column.getMappedStatus()
        );
    }
}
