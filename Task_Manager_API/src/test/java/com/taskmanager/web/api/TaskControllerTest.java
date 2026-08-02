package com.taskmanager.web.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.domain.TaskPriority;
import com.taskmanager.domain.TaskStatus;
import com.taskmanager.web.api.dto.MoveTaskRequest;
import com.taskmanager.web.api.dto.TaskRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private Long columnOneId;
    private Long columnTwoId;

    @BeforeEach
    void setUp() throws Exception {
        String email = "task-user-" + System.nanoTime() + "@test.com";
        token = registerAndGetToken(email);

        Long projectId = createProject();
        Long boardId = createBoard(projectId);
        columnOneId = createColumn(boardId, "Backlog");
        columnTwoId = createColumn(boardId, "Done");
    }

    @Test
    void createMoveAndFilterTasks() throws Exception {
        TaskRequest taskRequest = new TaskRequest("Write docs", "README section", TaskPriority.HIGH, null, null, null, null, null, null, null, null);

        MvcResult createResult = mockMvc.perform(post("/api/v1/columns/" + columnOneId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Write docs"))
                .andReturn();

        Long taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/tasks/" + taskId + "/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CREATED"));

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/move")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MoveTaskRequest(columnTwoId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columnId").value(columnTwoId));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/columns/" + columnTwoId + "/tasks?priority=HIGH")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Write docs"));
    }

    @Test
    void createTaskCanStartWithDoneStatus() throws Exception {
        TaskRequest taskRequest = new TaskRequest("Ready task", "Done task", TaskPriority.LOW, null, null, TaskStatus.DONE, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/columns/" + columnOneId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void updateTaskStatusChangesStatus() throws Exception {
        TaskRequest createRequest = new TaskRequest("Write docs", "README section", TaskPriority.HIGH, null, null, null, null, null, null, null, null);

        MvcResult createResult = mockMvc.perform(post("/api/v1/columns/" + columnOneId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        TaskRequest updateRequest = new TaskRequest("Write docs", "README section", TaskPriority.HIGH, null, null, TaskStatus.IN_PROGRESS, null, null, null, null, null);

        mockMvc.perform(put("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    private String registerAndGetToken(String email) throws Exception {
        String body = """
                {"email":"%s","password":"secret123","name":"Task User"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private Long createProject() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tasks Project\",\"description\":\"Test\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createBoard(Long projectId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects/" + projectId + "/boards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Main Board\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createColumn(Long boardId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/boards/" + boardId + "/columns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
