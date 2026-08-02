package com.taskmanager.web.api.dto;

import com.taskmanager.domain.BoardAccessMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BoardRequest(
        @NotBlank @Size(max = 255) String name,
        BoardAccessMode accessMode,
        List<Long> teamIds
) {
    public BoardRequest(String name) {
        this(name, null, null);
    }

    public BoardAccessMode accessModeOrDefault() {
        return accessMode == null ? BoardAccessMode.OPEN : accessMode;
    }
}
