package com.taskmanager.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TaskTest {

    @Test
    void backlogCanMoveToInProgress() {
        Task task = new Task();
        task.setStatus(TaskStatus.BACKLOG);

        task.transitionTo(TaskStatus.IN_PROGRESS);

        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
    }

    @Test
    void sameStatusTransitionIsAllowedAsNoOp() {
        Task task = new Task();
        task.setStatus(TaskStatus.BACKLOG);

        task.transitionTo(TaskStatus.BACKLOG);

        assertEquals(TaskStatus.BACKLOG, task.getStatus());
    }

    @Test
    void doneCannotMoveBackToBacklog() {
        Task task = new Task();
        task.setStatus(TaskStatus.DONE);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> task.transitionTo(TaskStatus.BACKLOG));

        assertEquals("Invalid task status transition from DONE to BACKLOG", ex.getMessage());
    }
}
