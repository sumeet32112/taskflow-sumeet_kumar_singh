package com.taskflow.repository;

import com.taskflow.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    /**
     * Projects where the user is the owner OR is assigned to at least one task.
     * EXISTS subquery avoids the LEFT JOIN / ON nullable-association pitfalls in JPQL.
     */
    @Query("""
            SELECT p FROM Project p
            WHERE p.owner.id = :userId
               OR EXISTS (
                   SELECT t FROM Task t
                   WHERE t.project = p
                     AND t.assignee.id = :userId
               )
            """)
    Page<Project> findAccessibleByUser(@Param("userId") UUID userId, Pageable pageable);
}
