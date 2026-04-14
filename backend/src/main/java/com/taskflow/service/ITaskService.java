package com.taskflow.service;

import com.taskflow.dto.ProjectDto;
import com.taskflow.dto.TaskDto;
import com.taskflow.model.*;
import java.util.UUID;

public interface ITaskService {

    ProjectDto.PagedResponse<TaskDto.TaskResponse> list(UUID projectId,
                                                               TaskStatus status,
                                                               UUID         assigneeId,
                                                               int page, int size);

    TaskDto.TaskResponse create(UUID actorId, UUID projectId, TaskDto.CreateRequest req);

    TaskDto.TaskResponse update(UUID actorId, UUID taskId, TaskDto.UpdateRequest req);

    void delete(UUID actorId, UUID taskId);

}
