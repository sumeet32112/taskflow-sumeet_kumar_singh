package com.taskflow.service.impl;

import com.taskflow.dto.ProjectDto;
import com.taskflow.dto.TaskDto;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.NotFoundException;
import com.taskflow.model.*;
import com.taskflow.repository.*;
import com.taskflow.service.ITaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService implements ITaskService {

    private final TaskRepository    taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository    userRepository;

    /* ── List (with optional filters) ──────────────────────────────── */

    @Transactional(readOnly = true)
    @Override
    public ProjectDto.PagedResponse<TaskDto.TaskResponse> list(UUID projectId,
                                                               TaskStatus   status,
                                                               UUID         assigneeId,
                                                               int page, int size) {
        requireProjectExists(projectId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Task> tasks;
        if (status != null && assigneeId != null) {
            tasks = taskRepository.findByProjectIdAndStatusAndAssigneeId(
                    projectId, status, assigneeId, pageable);
        } else if (status != null) {
            tasks = taskRepository.findByProjectIdAndStatus(projectId, status, pageable);
        } else if (assigneeId != null) {
            tasks = taskRepository.findByProjectIdAndAssigneeId(projectId, assigneeId, pageable);
        } else {
            tasks = taskRepository.findByProjectId(projectId, pageable);
        }

        List<TaskDto.TaskResponse> content = tasks.getContent().stream()
                .map(this::toResponse)
                .toList();

        return ProjectDto.PagedResponse.<TaskDto.TaskResponse>builder()
                .content(content)
                .page(tasks.getNumber())
                .size(tasks.getSize())
                .totalElements(tasks.getTotalElements())
                .totalPages(tasks.getTotalPages())
                .build();
    }

    /* ── Create ─────────────────────────────────────────────────────── */

    @Transactional
    @Override
    public TaskDto.TaskResponse create(UUID actorId, UUID projectId,
                                       TaskDto.CreateRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(NotFoundException::new);

        User assignee = null;
        if (req.getAssigneeId() != null) {
            assignee = userRepository.findById(req.getAssigneeId())
                    .orElseThrow(() -> new NotFoundException());
        }

        Task task = taskRepository.save(Task.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .status(req.getStatus() != null ? req.getStatus() : TaskStatus.todo)
                .priority(req.getPriority() != null ? req.getPriority() : TaskPriority.medium)
                .project(project)
                .assignee(assignee)
                .dueDate(req.getDueDate())
                .build());

        log.info("Created task id={} in project={} by user={}", task.getId(), projectId, actorId);
        return toResponse(task);
    }

    /* ── Update ─────────────────────────────────────────────────────── */

    @Transactional
    @Override
    public TaskDto.TaskResponse update(UUID actorId, UUID taskId,
                                       TaskDto.UpdateRequest req) {
        Task task = findOrThrow(taskId);

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            task.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            task.setDescription(req.getDescription());
        }
        if (req.getStatus() != null) {
            task.setStatus(req.getStatus());
        }
        if (req.getPriority() != null) {
            task.setPriority(req.getPriority());
        }
        if (req.getDueDate() != null) {
            task.setDueDate(req.getDueDate());
        }
        // assigneeId key present but null = unassign; we detect this by always applying
        if (req.getAssigneeId() != null) {
            User assignee = userRepository.findById(req.getAssigneeId())
                    .orElseThrow(NotFoundException::new);
            task.setAssignee(assignee);
        }

        return toResponse(taskRepository.save(task));
    }

    /* ── Delete ─────────────────────────────────────────────────────── */

    @Transactional
    @Override
    public void delete(UUID actorId, UUID taskId) {
        Task task = findOrThrow(taskId);

        boolean isProjectOwner = task.getProject().getOwner().getId().equals(actorId);
        // Task "creator" isn't tracked in this schema intentionally (see README).
        // Deletion is allowed for the project owner only.
        if (!isProjectOwner) {
            throw new ForbiddenException();
        }

        taskRepository.delete(task);
        log.info("Deleted task id={} by user={}", taskId, actorId);
    }

    /* ── Helpers ────────────────────────────────────────────────────── */

    private Task findOrThrow(UUID id) {
        return taskRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    private void requireProjectExists(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException();
        }
    }

    TaskDto.TaskResponse toResponse(Task t) {
        TaskDto.AssigneeSummary assignee = t.getAssignee() == null ? null :
                TaskDto.AssigneeSummary.builder()
                        .id(t.getAssignee().getId())
                        .name(t.getAssignee().getName())
                        .email(t.getAssignee().getEmail())
                        .build();

        return TaskDto.TaskResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .status(t.getStatus())
                .priority(t.getPriority())
                .projectId(t.getProject().getId())
                .assignee(assignee)
                .dueDate(t.getDueDate())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
