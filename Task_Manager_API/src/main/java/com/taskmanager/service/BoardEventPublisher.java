package com.taskmanager.service;

import com.taskmanager.web.api.dto.BoardEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class BoardEventPublisher {

    private final BoardEventHub boardEventHub;

    public BoardEventPublisher(BoardEventHub boardEventHub) {
        this.boardEventHub = boardEventHub;
    }

    public void publishAfterCommit(String type, Long boardId, Long taskId) {
        if (boardId == null) {
            return;
        }
        BoardEvent event = BoardEvent.of(type, boardId, taskId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    boardEventHub.publish(event);
                }
            });
        } else {
            boardEventHub.publish(event);
        }
    }
}
