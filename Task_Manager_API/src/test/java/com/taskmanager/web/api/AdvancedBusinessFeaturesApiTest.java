package com.taskmanager.web.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.service.MaintenanceService;
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
class AdvancedBusinessFeaturesApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MaintenanceService maintenanceService;

    private String token;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        String email = "adv-" + System.nanoTime() + "@test.com";
        String body = "{\"email\":\"%s\",\"password\":\"secret123\",\"name\":\"Adv User\"}".formatted(email);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode auth = objectMapper.readTree(result.getResponse().getContentAsString());
        token = auth.get("token").asText();
        userId = auth.get("user").get("id").asLong();
    }

    @Test
    void capacityLimitBlocksOverloadedAssignee() throws Exception {
        long projectId = createProject(false);
        mockMvc.perform(patch("/api/v1/projects/" + projectId + "/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacityLimitHours\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacityLimitHours").value(5));

        long boardId = createBoard(projectId);
        long colId = createColumn(boardId);

        mockMvc.perform(post("/api/v1/columns/" + colId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"A\",\"priority\":\"MEDIUM\",\"assigneeId\":" + userId
                                + ",\"estimateHours\":3}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/columns/" + colId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"B\",\"priority\":\"MEDIUM\",\"assigneeId\":" + userId
                                + ",\"estimateHours\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void priorityQueueAllowsOnlyOneHighInProgress() throws Exception {
        long projectId = createProject(false);
        mockMvc.perform(patch("/api/v1/projects/" + projectId + "/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priorityQueueRules\":true}"))
                .andExpect(status().isOk());

        long boardId = createBoard(projectId);
        long todo = createColumn(boardId);
        long doing = createMappedColumn(boardId, "Doing", "IN_PROGRESS");

        long t1 = createTask(todo, "H1", "HIGH");
        long t2 = createTask(todo, "H2", "HIGH");

        mockMvc.perform(patch("/api/v1/tasks/" + t1 + "/move")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnId\":" + doing + "}"))
                .andExpect(status().isOk());

        // assign both to self first for queue rule
        putAssignee(t1, userId);
        putAssignee(t2, userId);

        mockMvc.perform(patch("/api/v1/tasks/" + t2 + "/move")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnId\":" + doing + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approvalRequiredBeforeDone() throws Exception {
        long projectId = createProject(false);
        mockMvc.perform(patch("/api/v1/projects/" + projectId + "/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requireApprovalForDone\":true}"))
                .andExpect(status().isOk());

        long boardId = createBoard(projectId);
        long todo = createColumn(boardId);
        long done = createMappedColumn(boardId, "Done", "DONE");
        long taskId = createTask(todo, "Needs review", "MEDIUM");

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/move")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnId\":" + done + "}"))
                .andExpect(status().isBadRequest());

        MvcResult approval = mockMvc.perform(post("/api/v1/tasks/" + taskId + "/approvals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approverId\":" + userId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        long approvalId = objectMapper.readTree(approval.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/approvals/" + approvalId + "/approve")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"LGTM\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/move")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnId\":" + done + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void goalsRisksTimeReportAndMaintenance() throws Exception {
        long projectId = createProject(false);
        long boardId = createBoard(projectId);
        long colId = createColumn(boardId);
        long taskId = createTask(colId, "Billable", "LOW");

        mockMvc.perform(put("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Billable\",\"priority\":\"LOW\",\"spentHours\":2.5,\"status\":\"BACKLOG\"}"))
                .andExpect(status().isOk());

        MvcResult goal = mockMvc.perform(post("/api/v1/projects/" + projectId + "/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Ship v1\",\"description\":\"MVP\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long goalId = objectMapper.readTree(goal.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/goals/" + goalId + "/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/risks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Vendor delay\",\"taskId\":" + taskId + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/time-report?format=json")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/tasks/" + taskId + "/status-history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        maintenanceService.runOnce();
    }

    @Test
    void scrumTemplateSeedsBoardAndSprint() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Scrum\",\"template\":\"SCRUM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dodTemplatesEnabled").value(true))
                .andExpect(jsonPath("$.customWorkflowEnabled").value(true))
                .andReturn();
        long projectId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/boards")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Main"));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/sprints")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sprint 1"));
    }

    private long createProject(boolean strict) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Adv\",\"strictBusinessRules\":" + strict + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createBoard(long projectId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects/" + projectId + "/boards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createColumn(long boardId) throws Exception {
        return createMappedColumn(boardId, "Todo", null);
    }

    private long createMappedColumn(long boardId, String name, String mapped) throws Exception {
        String body = mapped == null
                ? "{\"name\":\"" + name + "\"}"
                : "{\"name\":\"" + name + "\",\"mappedStatus\":\"" + mapped + "\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/boards/" + boardId + "/columns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createTask(long columnId, String title, String priority) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/columns/" + columnId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"priority\":\"" + priority + "\",\"assigneeId\":"
                                + userId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void putAssignee(long taskId, long assigneeId) throws Exception {
        mockMvc.perform(put("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"priority\":\"HIGH\",\"assigneeId\":" + assigneeId + "}"))
                .andExpect(status().isOk());
    }
}
