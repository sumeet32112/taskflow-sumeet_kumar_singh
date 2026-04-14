package com.taskflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.AuthDto;
import com.taskflow.dto.ProjectDto;
import com.taskflow.dto.TaskDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectTaskIntegrationTest {

    @Autowired MockMvc      mvc;
    @Autowired ObjectMapper mapper;

    private static String bearerToken;
    private static String projectId;
    private static String taskId;

    private static final String EMAIL    = "proj-it-" + UUID.randomUUID() + "@example.com";
    private static final String PASSWORD = "testpassword1";

    /* ── Setup: register + login ────────────────────────────────────── */

    @Test @Order(1)
    void setup_registerAndLogin() throws Exception {
        AuthDto.RegisterRequest reg = new AuthDto.RegisterRequest();
        reg.setName("Proj Tester");
        reg.setEmail(EMAIL);
        reg.setPassword(PASSWORD);

        MvcResult result = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        bearerToken = "Bearer " + mapper.readTree(
                result.getResponse().getContentAsString()).get("token").asText();
    }

    /* ── Projects ───────────────────────────────────────────────────── */

    @Test @Order(2)
    void createProject_returnsProjectWithOwner() throws Exception {
        ProjectDto.CreateRequest req = new ProjectDto.CreateRequest();
        req.setName("My Project");
        req.setDescription("Integration test project");

        MvcResult result = mvc.perform(post("/projects")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id",         notNullValue()))
                .andExpect(jsonPath("$.name",        is("My Project")))
                .andExpect(jsonPath("$.owner.email", is(EMAIL)))
                .andReturn();

        projectId = mapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    @Test @Order(3)
    void listProjects_includesCreatedProject() throws Exception {
        mvc.perform(get("/projects")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].id", notNullValue()));
    }

    @Test @Order(4)
    void getProject_returnsDetailWithEmptyTasks() throws Exception {
        mvc.perform(get("/projects/" + projectId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",    is(projectId)))
                .andExpect(jsonPath("$.tasks", hasSize(0)));
    }

    @Test @Order(5)
    void updateProject_withOwner_succeeds() throws Exception {
        ProjectDto.UpdateRequest req = new ProjectDto.UpdateRequest();
        req.setName("Updated Project Name");

        mvc.perform(patch("/projects/" + projectId)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Project Name")));
    }

    /* ── Tasks ──────────────────────────────────────────────────────── */

    @Test @Order(6)
    void createTask_returnsTaskWithDefaults() throws Exception {
        TaskDto.CreateRequest req = new TaskDto.CreateRequest();
        req.setTitle("First Task");
        req.setDescription("Do something important");

        MvcResult result = mvc.perform(post("/projects/" + projectId + "/tasks")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id",       notNullValue()))
                .andExpect(jsonPath("$.status",   is("todo")))
                .andExpect(jsonPath("$.priority", is("medium")))
                .andReturn();

        taskId = mapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    @Test @Order(7)
    void listTasks_filterByStatus_returnOnlyMatchingTasks() throws Exception {
        mvc.perform(get("/projects/" + projectId + "/tasks?status=todo")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("todo"))));
    }

    @Test @Order(8)
    void updateTask_changesStatusAndPriority() throws Exception {
        TaskDto.UpdateRequest req = new TaskDto.UpdateRequest();
        req.setStatus(com.taskflow.model.TaskStatus.in_progress);
        req.setPriority(com.taskflow.model.TaskPriority.high);

        mvc.perform(patch("/tasks/" + taskId)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status",   is("in_progress")))
                .andExpect(jsonPath("$.priority", is("high")));
    }

    @Test @Order(9)
    void getProjectStats_returnsTaskCounts() throws Exception {
        mvc.perform(get("/projects/" + projectId + "/stats")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatus", notNullValue()));
    }

    @Test @Order(10)
    void deleteTask_removesTask() throws Exception {
        mvc.perform(delete("/tasks/" + taskId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isNoContent());
    }

    @Test @Order(11)
    void deleteProject_removesProject() throws Exception {
        mvc.perform(delete("/projects/" + projectId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isNoContent());
    }

    @Test @Order(12)
    void getDeletedProject_returns404() throws Exception {
        mvc.perform(get("/projects/" + projectId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("not found")));
    }
}
