package com.taskmanager.repository;

import com.taskmanager.domain.BoardColumn;
import com.taskmanager.domain.Label;
import com.taskmanager.domain.Project;
import com.taskmanager.domain.Sprint;
import com.taskmanager.domain.Task;
import com.taskmanager.domain.TaskPriority;
import com.taskmanager.domain.TaskStatus;
import com.taskmanager.domain.TaskType;
import com.taskmanager.domain.User;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    long countByColumnAndDeletedAtIsNull(BoardColumn column);

    List<Task> findBySprintAndDeletedAtIsNull(Sprint sprint);

    List<Task> findByStatusAndDeletedAtIsNull(TaskStatus status);

    List<Task> findByColumn_Board_ProjectAndDeletedAtIsNull(Project project);

    @Query("""
            SELECT COALESCE(SUM(t.estimateHours), 0) FROM Task t
            WHERE t.column.board.project = :project
              AND t.assignee = :assignee
              AND t.deletedAt IS NULL
              AND t.status NOT IN :terminal
              AND (:excludeTaskId IS NULL OR t.id <> :excludeTaskId)
            """)
    BigDecimal sumActiveEstimateHours(
            @Param("project") Project project,
            @Param("assignee") User assignee,
            @Param("excludeTaskId") Long excludeTaskId,
            @Param("terminal") List<TaskStatus> terminal);

    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.column.board.project = :project
              AND t.assignee = :assignee
              AND t.deletedAt IS NULL
              AND t.status = :status
              AND t.priority = :priority
              AND (:excludeTaskId IS NULL OR t.id <> :excludeTaskId)
            """)
    long countByAssigneeStatusPriority(
            @Param("project") Project project,
            @Param("assignee") User assignee,
            @Param("status") TaskStatus status,
            @Param("priority") TaskPriority priority,
            @Param("excludeTaskId") Long excludeTaskId);

    @Query("""
            SELECT t FROM Task t
            WHERE t.deletedAt IS NULL
              AND t.status = :status
              AND t.column.board.project.autoArchiveDoneDays IS NOT NULL
            """)
    List<Task> findDoneCandidatesForAutoArchive(@Param("status") TaskStatus status);

    @Query("""
            SELECT t FROM Task t
            WHERE t.deletedAt IS NULL
              AND t.deadline IS NOT NULL
              AND t.status NOT IN :terminal
              AND (t.column.board.project.slaWarningDays IS NOT NULL
                   OR t.column.board.project.slaEscalateDays IS NOT NULL)
            """)
    List<Task> findSlaCandidates(@Param("terminal") List<TaskStatus> terminal);

    @Query("""
            SELECT t FROM Task t
            WHERE t.deletedAt IS NULL
              AND t.status NOT IN :terminal
              AND t.column.board.project = :project
            """)
    List<Task> findActiveInProject(
            @Param("project") Project project,
            @Param("terminal") List<TaskStatus> terminal);

    @Query("""
            SELECT t FROM Task t
            WHERE t.column = :column
              AND t.deletedAt IS NULL
              AND (:assignee IS NULL OR t.assignee = :assignee)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:status IS NULL OR t.status = :status)
              AND (:taskType IS NULL OR t.taskType = :taskType)
              AND (:label IS NULL OR :label MEMBER OF t.labels)
              AND (:q = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Task> findFilteredPageable(
            @Param("column") BoardColumn column,
            @Param("assignee") User assignee,
            @Param("priority") TaskPriority priority,
            @Param("status") TaskStatus status,
            @Param("taskType") TaskType taskType,
            @Param("label") Label label,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT t FROM Task t
            WHERE t.column.board.project = :project
              AND t.deletedAt IS NULL
              AND (:assignee IS NULL OR t.assignee = :assignee)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:status IS NULL OR t.status = :status)
              AND (:taskType IS NULL OR t.taskType = :taskType)
              AND (:label IS NULL OR :label MEMBER OF t.labels)
              AND (:q = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Task> findInProject(
            @Param("project") Project project,
            @Param("assignee") User assignee,
            @Param("priority") TaskPriority priority,
            @Param("status") TaskStatus status,
            @Param("taskType") TaskType taskType,
            @Param("label") Label label,
            @Param("q") String q,
            Pageable pageable);
}
