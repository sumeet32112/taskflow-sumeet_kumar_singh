package com.taskflow.controller;

import com.taskflow.dto.ProjectDto;
import com.taskflow.service.IProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final IProjectService projectService;
    private final CurrentUser    currentUser;

    /* GET /projects?page=0&limit=20 */
    @GetMapping
    public ResponseEntity<ProjectDto.PagedResponse<ProjectDto.ProjectResponse>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(projectService.listForUser(currentUser.id(), page, limit));
    }

    /* POST /projects */
    @PostMapping
    public ResponseEntity<ProjectDto.ProjectResponse> create(
            @Valid @RequestBody ProjectDto.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(currentUser.id(), req));
    }

    /* GET /projects/:id */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto.ProjectDetailResponse> getOne(
            @PathVariable java.util.UUID id) {
        return ResponseEntity.ok(projectService.getDetail(id));
    }

    /* PATCH /projects/:id */
    @PatchMapping("/{id}")
    public ResponseEntity<ProjectDto.ProjectResponse> update(
            @PathVariable java.util.UUID id,
            @RequestBody ProjectDto.UpdateRequest req) {
        return ResponseEntity.ok(projectService.update(currentUser.id(), id, req));
    }

    /* DELETE /projects/:id */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable java.util.UUID id) {
        projectService.delete(currentUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    /* GET /projects/:id/stats */
    @GetMapping("/{id}/stats")
    public ResponseEntity<ProjectDto.StatsResponse> stats(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(projectService.getStats(id));
    }
}
