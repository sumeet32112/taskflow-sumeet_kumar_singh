package com.taskflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ProjectDto {

    /* ── Requests ──────────────────────────────────────────────────── */

    @Getter @Setter
    public static class CreateRequest {
        @NotBlank(message = "is required")
        private String name;
        private String description;
    }

    @Getter @Setter
    public static class UpdateRequest {
        private String name;
        private String description;
    }

    /* ── Responses ─────────────────────────────────────────────────── */

    @Builder @Getter
    public static class ProjectResponse {
        private UUID           id;
        private String         name;
        private String         description;
        private OwnerSummary   owner;
        private OffsetDateTime createdAt;
    }

    @Builder @Getter
    public static class ProjectDetailResponse {
        private UUID             id;
        private String           name;
        private String           description;
        private OwnerSummary     owner;
        private List<TaskDto.TaskResponse> tasks;
        private OffsetDateTime   createdAt;
    }

    @Builder @Getter
    public static class OwnerSummary {
        private UUID   id;
        private String name;
        private String email;
    }

    /* ── Pagination wrapper ────────────────────────────────────────── */

    @Builder @Getter
    public static class PagedResponse<T> {
        private List<T> content;
        private int     page;
        private int     size;
        private long    totalElements;
        private int     totalPages;
    }

    /* ── Stats ─────────────────────────────────────────────────────── */

    @Builder @Getter
    public static class StatsResponse {
        private java.util.Map<String, Long> byStatus;
        private List<AssigneeCount>         byAssignee;
    }

    @Builder @Getter
    public static class AssigneeCount {
        private UUID   userId;
        private String name;
        private long   count;
    }
}
