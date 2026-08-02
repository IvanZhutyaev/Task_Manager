package com.taskmanager.web.api;

import com.taskmanager.config.ApiConstants;
import com.taskmanager.config.RealtimeProperties;
import com.taskmanager.domain.Board;
import com.taskmanager.service.BoardAccessService;
import com.taskmanager.service.BoardEventHub;
import com.taskmanager.service.BoardService;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/boards")
public class BoardEventsController {

    private final BoardService boardService;
    private final BoardAccessService boardAccessService;
    private final BoardEventHub boardEventHub;
    private final CurrentUserService currentUserService;
    private final RealtimeProperties realtimeProperties;

    public BoardEventsController(
            BoardService boardService,
            BoardAccessService boardAccessService,
            BoardEventHub boardEventHub,
            CurrentUserService currentUserService,
            RealtimeProperties realtimeProperties) {
        this.boardService = boardService;
        this.boardAccessService = boardAccessService;
        this.boardEventHub = boardEventHub;
        this.currentUserService = currentUserService;
        this.realtimeProperties = realtimeProperties;
    }

    @GetMapping(value = "/{boardId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long boardId) {
        if (!realtimeProperties.isEnabled()) {
            throw new ApiException(HttpStatus.NOT_FOUND.value(), "Realtime is disabled");
        }
        Board board = boardService.getBoardOrThrow(boardId);
        boardAccessService.requireCanViewBoard(board, currentUserService.getCurrentUser());
        return boardEventHub.subscribe(boardId);
    }
}
