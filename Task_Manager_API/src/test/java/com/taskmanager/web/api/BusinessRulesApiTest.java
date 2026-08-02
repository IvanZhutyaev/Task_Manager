package com.taskmanager.web.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class BusinessRulesApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String email = "rules-" + System.nanoTime() + "@test.com";
        String body = "{\"email\":\"%s\",\"password\":\"secret123\",\"name\":\"Rules User\"}".formatted(email);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void mappedStatusOnMoveAndWipLimit() throws Exception {
        Long projectId = createProject(false);
        Long boardId = createBoard(projectId);
        Long todoId = createColumn(boardId, "Todo", null, null);
        Long doneId = createColumn(boardId, "Done", null, "DONE");
        Long limitedId = createColumn(boardId, "Limited", 1, null);

        Long taskId = createTask(todoId, "Move me");

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/move")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnId\":" + doneId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.overdue").value(false));

        createTask(limitedId, "Fill WIP");
        mockMvc.perform(post("/api/v1/columns/" + limitedId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Overflow\",\"priority\":\"LOW\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void defaultBoardTemplate() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Templated\",\"withDefaultBoard\":true,\"strictBusinessRules\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.strictBusinessRules").value(true))
                .andReturn();
        long projectId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/projects/" + projectId + "/boards")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Main"));
    }

    private Long createProject(boolean strict) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P\",\"strictBusinessRules\":" + strict + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createBoard(Long projectId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects/" + projectId + "/boards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createColumn(Long boardId, String name, Integer wip, String mapped) throws Exception {
        String body = mapped == null
                ? "{\"name\":\"%s\",\"wipLimit\":%s}".formatted(name, wip)
                : "{\"name\":\"%s\",\"wipLimit\":%s,\"mappedStatus\":\"%s\"}".formatted(name, wip, mapped);
        if (wip == null && mapped == null) {
            body = "{\"name\":\"%s\"}".formatted(name);
        } else if (wip == null) {
            body = "{\"name\":\"%s\",\"mappedStatus\":\"%s\"}".formatted(name, mapped);
        } else if (mapped == null) {
            body = "{\"name\":\"%s\",\"wipLimit\":%d}".formatted(name, wip);
        }
        MvcResult result = mockMvc.perform(post("/api/v1/boards/" + boardId + "/columns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createTask(Long columnId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/columns/" + columnId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\",\"priority\":\"MEDIUM\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
