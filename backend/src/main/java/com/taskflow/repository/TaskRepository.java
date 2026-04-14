package com.taskflow.repository;

import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByProjectId(UUID projectId, Pageable pageable);

    Page<Task> findByProjectIdAndStatus(UUID projectId, TaskStatus status, Pageable pageable);

    Page<Task> findByProjectIdAndAssigneeId(UUID projectId, UUID assigneeId, Pageable pageable);

    Page<Task> findByProjectIdAndStatusAndAssigneeId(
            UUID projectId, TaskStatus status, UUID assigneeId, Pageable pageable);

    // For stats endpoint
    @Query("""
            SELECT t.status, COUNT(t)
            FROM Task t
            WHERE t.project.id = :projectId
            GROUP BY t.status
            """)
    java.util.List<Object[]> countByStatusForProject(@Param("projectId") UUID projectId);

    @Query("""
            SELECT t.assignee.id, t.assignee.name, COUNT(t)
            FROM Task t
            WHERE t.project.id = :projectId AND t.assignee IS NOT NULL
            GROUP BY t.assignee.id, t.assignee.name
            """)
    java.util.List<Object[]> countByAssigneeForProject(@Param("projectId") UUID projectId);
}
