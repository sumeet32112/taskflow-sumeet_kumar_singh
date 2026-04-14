package com.taskflow.dto;

import com.taskflow.model.TaskPriority;
import com.taskflow.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class TaskDto {

    /* ── Requests ──────────────────────────────────────────────────── */

    @Getter @Setter
    public static class CreateRequest {
        @NotBlank(message = "is required")
        private String title;

        private String       description;
        private TaskStatus   status;
        private TaskPriority priority;
        private UUID         assigneeId;
        private LocalDate    dueDate;
    }

    @Getter @Setter
    public static class UpdateRequest {
        private String       title;
        private String       description;
        private TaskStatus   status;
        private TaskPriority priority;
        private UUID         assigneeId;
        private LocalDate    dueDate;
    }

    /* ── Response ──────────────────────────────────────────────────── */

    @Builder @Getter
    public static class TaskResponse {
        private UUID           id;
        private String         title;
        private String         description;
        private TaskStatus     status;
        private TaskPriority   priority;
        private UUID           projectId;
        private AssigneeSummary assignee;
        private LocalDate      dueDate;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Builder @Getter
    public static class AssigneeSummary {
        private UUID   id;
        private String name;
        private String email;
    }
}
