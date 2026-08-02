package com.taskmanager.web.api;

import com.taskmanager.config.ApiConstants;
import com.taskmanager.service.ColumnService;
import com.taskmanager.web.api.dto.ColumnRequest;
import com.taskmanager.web.api.dto.ColumnResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = ApiConstants.API_V1 + "/boards/{boardId}/columns", produces = MediaType.APPLICATION_JSON_VALUE)
public class ColumnController {

    private final ColumnService columnService;

    public ColumnController(ColumnService columnService) {
        this.columnService = columnService;
    }

    @GetMapping
    public List<ColumnResponse> listColumns(@PathVariable Long boardId) {
        return columnService.listColumns(boardId);
    }

    @GetMapping("/{columnId}")
    public ColumnResponse getColumn(@PathVariable Long boardId, @PathVariable Long columnId) {
        return columnService.getColumn(columnId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColumnResponse createColumn(
            @PathVariable Long boardId,
            @Valid @RequestBody ColumnRequest request) {
        return columnService.createColumn(boardId, request);
    }

    @PutMapping("/{columnId}")
    public ColumnResponse updateColumn(
            @PathVariable Long boardId,
            @PathVariable Long columnId,
            @Valid @RequestBody ColumnRequest request) {
        return columnService.updateColumn(columnId, request);
    }

    @DeleteMapping("/{columnId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteColumn(@PathVariable Long boardId, @PathVariable Long columnId) {
        columnService.deleteColumn(columnId);
    }
}
