package com.taskflow.service.impl;

import com.taskflow.dto.ProjectDto;
import com.taskflow.dto.TaskDto;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.NotFoundException;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.User;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.IProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService implements IProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository    taskRepository;
    private final UserRepository    userRepository;

    /* ── List ──────────────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    @Override
    public ProjectDto.PagedResponse<ProjectDto.ProjectResponse> listForUser(UUID userId,
                                                                            int page,
                                                                            int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Project> projects = projectRepository.findAccessibleByUser(userId, pageable);

        List<ProjectDto.ProjectResponse> content = projects.getContent().stream()
                .map(this::toResponse)
                .toList();

        return ProjectDto.PagedResponse.<ProjectDto.ProjectResponse>builder()
                .content(content)
                .page(projects.getNumber())
                .size(projects.getSize())
                .totalElements(projects.getTotalElements())
                .totalPages(projects.getTotalPages())
                .build();
    }

    /* ── Create ─────────────────────────────────────────────────────── */

    @Transactional
    @Override
    public ProjectDto.ProjectResponse create(UUID ownerId, ProjectDto.CreateRequest req) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(NotFoundException::new);

        Project project = projectRepository.save(Project.builder()
                .name(req.getName())
                .description(req.getDescription())
                .owner(owner)
                .build());

        log.info("Created project id={} owner={}", project.getId(), ownerId);
        return toResponse(project);
    }

    /* ── Get (with tasks) ───────────────────────────────────────────── */

    @Transactional(readOnly = true)
    @Override
    public ProjectDto.ProjectDetailResponse getDetail(UUID projectId) {
        Project project = findOrThrow(projectId);

        List<Task> tasks = taskRepository
                .findByProjectId(projectId, Pageable.unpaged())
                .getContent();

        List<TaskDto.TaskResponse> taskDtos = tasks.stream()
                .map(this::toTaskResponse)
                .toList();

        return ProjectDto.ProjectDetailResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .owner(toOwnerSummary(project.getOwner()))
                .tasks(taskDtos)
                .createdAt(project.getCreatedAt())
                .build();
    }

    /* ── Update ─────────────────────────────────────────────────────── */

    @Transactional
    @Override
    public ProjectDto.ProjectResponse update(UUID actorId, UUID projectId,
                                             ProjectDto.UpdateRequest req) {
        Project project = findOrThrow(projectId);
        requireOwner(actorId, project);

        if (req.getName() != null && !req.getName().isBlank()) {
            project.setName(req.getName());
        }
        if (req.getDescription() != null) {
            project.setDescription(req.getDescription());
        }

        return toResponse(projectRepository.save(project));
    }

    /* ── Delete ─────────────────────────────────────────────────────── */

    @Transactional
    @Override
    public void delete(UUID actorId, UUID projectId) {
        Project project = findOrThrow(projectId);
        requireOwner(actorId, project);
        projectRepository.delete(project);
        log.info("Deleted project id={} by user={}", projectId, actorId);
    }

    /* ── Stats ──────────────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    @Override
    public ProjectDto.StatsResponse getStats(UUID projectId) {
        findOrThrow(projectId); // ensure exists

        Map<String, Long> byStatus = new LinkedHashMap<>();
        taskRepository.countByStatusForProject(projectId)
                .forEach(row -> byStatus.put(row[0].toString(), (Long) row[1]));

        List<ProjectDto.AssigneeCount> byAssignee = taskRepository
                .countByAssigneeForProject(projectId)
                .stream()
                .map(row -> ProjectDto.AssigneeCount.builder()
                        .userId((UUID) row[0])
                        .name((String) row[1])
                        .count((Long) row[2])
                        .build())
                .toList();

        return ProjectDto.StatsResponse.builder()
                .byStatus(byStatus)
                .byAssignee(byAssignee)
                .build();
    }

    /* ── Helpers ────────────────────────────────────────────────────── */

    public Project findOrThrow(UUID id) {
        return projectRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    private void requireOwner(UUID actorId, Project project) {
        if (!project.getOwner().getId().equals(actorId)) {
            throw new ForbiddenException();
        }
    }

    private ProjectDto.ProjectResponse toResponse(Project p) {
        return ProjectDto.ProjectResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .owner(toOwnerSummary(p.getOwner()))
                .createdAt(p.getCreatedAt())
                .build();
    }

    private ProjectDto.OwnerSummary toOwnerSummary(User u) {
        return ProjectDto.OwnerSummary.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .build();
    }

    private TaskDto.TaskResponse toTaskResponse(Task t) {
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
