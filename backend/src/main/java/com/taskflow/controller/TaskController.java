package com.taskflow.controller;

import com.taskflow.dto.ProjectDto;
import com.taskflow.dto.TaskDto;
import com.taskflow.model.TaskStatus;
import com.taskflow.service.ITaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final ITaskService taskService;
    private final CurrentUser currentUser;

    /* GET /projects/:id/tasks?page=&limit= */
    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<ProjectDto.PagedResponse<TaskDto.TaskResponse>> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) UUID assignee,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(
                taskService.list(projectId, status, assignee, page, limit));
    }

    /* POST /projects/:id/tasks */
    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskDto.TaskResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody TaskDto.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(currentUser.id(), projectId, req));
    }

    /* PATCH /tasks/:id */
    @PatchMapping("/tasks/{id}")
    public ResponseEntity<TaskDto.TaskResponse> update(
            @PathVariable UUID id,
            @RequestBody TaskDto.UpdateRequest req) {
        return ResponseEntity.ok(taskService.update(currentUser.id(), id, req));
    }

    /* DELETE /tasks/:id */
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        taskService.delete(currentUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}
