package com.taskflow.service;

import com.taskflow.dto.ProjectDto;
import java.util.UUID;

public interface IProjectService {

    ProjectDto.PagedResponse<ProjectDto.ProjectResponse> listForUser(UUID userId,
                                                                            int page,
                                                                            int size);
    ProjectDto.ProjectResponse create(UUID ownerId, ProjectDto.CreateRequest req);
    ProjectDto.ProjectDetailResponse getDetail(UUID projectId);
    ProjectDto.ProjectResponse update(UUID actorId, UUID projectId,
                                             ProjectDto.UpdateRequest req);
   void delete(UUID actorId, UUID projectId);
   ProjectDto.StatsResponse getStats(UUID projectId);
}
