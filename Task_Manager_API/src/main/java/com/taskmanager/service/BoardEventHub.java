package com.taskmanager.service;

import com.taskmanager.config.RealtimeProperties;
import com.taskmanager.web.api.dto.BoardEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class BoardEventHub {

    private static final Logger log = LoggerFactory.getLogger(BoardEventHub.class);
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final RealtimeProperties realtimeProperties;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByBoard = new ConcurrentHashMap<>();

    public BoardEventHub(RealtimeProperties realtimeProperties) {
        this.realtimeProperties = realtimeProperties;
    }

    public SseEmitter subscribe(Long boardId) {
        if (!realtimeProperties.isEnabled()) {
            throw new IllegalStateException("Realtime disabled");
        }
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emittersByBoard.computeIfAbsent(boardId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(boardId, emitter));
        emitter.onTimeout(() -> remove(boardId, emitter));
        emitter.onError(ex -> remove(boardId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("boardId", boardId)));
        } catch (IOException ex) {
            remove(boardId, emitter);
        }
        return emitter;
    }

    public void publish(BoardEvent event) {
        if (!realtimeProperties.isEnabled() || event == null || event.boardId() == null) {
            return;
        }
        List<SseEmitter> emitters = emittersByBoard.get(event.boardId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name("board").data(event));
            } catch (Exception ex) {
                remove(event.boardId(), emitter);
                log.debug("Removed stale SSE emitter for board {}", event.boardId());
            }
        }
    }

    private void remove(Long boardId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByBoard.get(boardId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByBoard.remove(boardId, emitters);
            }
        }
    }
}
