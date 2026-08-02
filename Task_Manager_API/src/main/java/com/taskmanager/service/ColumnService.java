package com.taskmanager.service;

import com.taskmanager.domain.Board;
import com.taskmanager.domain.BoardColumn;
import com.taskmanager.repository.BoardColumnRepository;
import com.taskmanager.security.CurrentUserService;
import com.taskmanager.web.api.dto.ColumnRequest;
import com.taskmanager.web.api.dto.ColumnResponse;
import com.taskmanager.web.exception.ApiException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ColumnService {

    private static final Logger log = LoggerFactory.getLogger(ColumnService.class);

    private final BoardColumnRepository boardColumnRepository;
    private final BoardService boardService;
    private final BoardAccessService boardAccessService;
    private final CurrentUserService currentUserService;

    public ColumnService(
            BoardColumnRepository boardColumnRepository,
            BoardService boardService,
            BoardAccessService boardAccessService,
            CurrentUserService currentUserService) {
        this.boardColumnRepository = boardColumnRepository;
        this.boardService = boardService;
        this.boardAccessService = boardAccessService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<ColumnResponse> listColumns(Long boardId) {
        Board board = boardService.getBoardOrThrow(boardId);
        boardAccessService.requireCanViewBoard(board, currentUserService.getCurrentUser());
        return boardColumnRepository.findByBoardOrderByPositionAsc(board).stream()
                .map(ColumnResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ColumnResponse getColumn(Long columnId) {
        BoardColumn column = getColumnOrThrow(columnId);
        boardAccessService.requireCanViewBoard(column.getBoard(), currentUserService.getCurrentUser());
        return ColumnResponse.from(column);
    }

    @Transactional
    public ColumnResponse createColumn(Long boardId, ColumnRequest request) {
        Board board = boardService.getBoardOrThrow(boardId);
        boardAccessService.requireCanWriteBoard(board, currentUserService.getCurrentUser());

        BoardColumn column = new BoardColumn();
        column.setBoard(board);
        column.setName(request.name());
        column.setPosition(boardColumnRepository.findMaxPositionByBoard(board) + 1);
        column.setWipLimit(request.wipLimit());
        column.setMappedStatus(request.mappedStatus());
        column = boardColumnRepository.save(column);

        log.info("Column created: id={}, boardId={}", column.getId(), boardId);
        return ColumnResponse.from(column);
    }

    @Transactional
    public ColumnResponse updateColumn(Long columnId, ColumnRequest request) {
        BoardColumn column = getColumnOrThrow(columnId);
        boardAccessService.requireCanWriteBoard(column.getBoard(), currentUserService.getCurrentUser());

        column.setName(request.name());
        column.setWipLimit(request.wipLimit());
        column.setMappedStatus(request.mappedStatus());
        column = boardColumnRepository.save(column);
        return ColumnResponse.from(column);
    }

    @Transactional
    public void deleteColumn(Long columnId) {
        BoardColumn column = getColumnOrThrow(columnId);
        boardAccessService.requireCanWriteBoard(column.getBoard(), currentUserService.getCurrentUser());
        boardColumnRepository.delete(column);
        log.info("Column deleted: id={}", columnId);
    }

    public BoardColumn getColumnOrThrow(Long columnId) {
        return boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Column not found"));
    }
}
