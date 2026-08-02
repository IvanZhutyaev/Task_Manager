package com.taskmanager.web.api;

import com.taskmanager.config.ApiConstants;
import com.taskmanager.service.BoardService;
import com.taskmanager.web.api.dto.BoardRequest;
import com.taskmanager.web.api.dto.BoardResponse;
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
@RequestMapping(value = ApiConstants.API_V1 + "/projects/{projectId}/boards", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    public List<BoardResponse> listBoards(@PathVariable Long projectId) {
        return boardService.listBoards(projectId);
    }

    @GetMapping("/{boardId}")
    public BoardResponse getBoard(@PathVariable Long projectId, @PathVariable Long boardId) {
        return boardService.getBoard(boardId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BoardResponse createBoard(
            @PathVariable Long projectId,
            @Valid @RequestBody BoardRequest request) {
        return boardService.createBoard(projectId, request);
    }

    @PutMapping("/{boardId}")
    public BoardResponse updateBoard(
            @PathVariable Long projectId,
            @PathVariable Long boardId,
            @Valid @RequestBody BoardRequest request) {
        return boardService.updateBoard(boardId, request);
    }

    @DeleteMapping("/{boardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBoard(@PathVariable Long projectId, @PathVariable Long boardId) {
        boardService.deleteBoard(boardId);
    }
}
