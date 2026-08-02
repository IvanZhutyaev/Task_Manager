package com.taskmanager.web.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
class OrganizationBoardAccessApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String ownerToken;
    private String memberToken;
    private String outsiderToken;
    private long memberUserId;
    private String memberEmail;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = register("owner-" + System.nanoTime() + "@test.com", "Owner");
        memberEmail = "member-" + System.nanoTime() + "@test.com";
        memberToken = register(memberEmail, "Member");
        memberUserId = meId(memberToken);
        outsiderToken = register("out-" + System.nanoTime() + "@test.com", "Outsider");
    }

    @Test
    void personalProjectBoardsRemainOpenByDefault() throws Exception {
        long projectId = createProject(ownerToken, null);
        MvcResult board = mockMvc.perform(post("/api/v1/projects/" + projectId + "/boards")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Legacy Board\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessMode").value("OPEN"))
                .andReturn();
        long boardId = objectMapper.readTree(board.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + memberUserId + ",\"role\":\"EDITOR\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/boards/" + boardId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessMode").value("OPEN"));
    }

    @Test
    void privateBoardHiddenFromOtherMembers() throws Exception {
        long orgId = createOrg(ownerToken, "LOCAL");
        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + memberEmail + "\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated());

        long projectId = createProject(ownerToken, orgId);
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + memberUserId + ",\"role\":\"EDITOR\"}"))
                .andExpect(status().isCreated());

        MvcResult board = mockMvc.perform(post("/api/v1/projects/" + projectId + "/boards")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Secret\",\"accessMode\":\"PRIVATE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessMode").value("PRIVATE"))
                .andReturn();
        long boardId = objectMapper.readTree(board.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/boards/" + boardId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/boards")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + boardId + ")]").isEmpty());
    }

    @Test
    void teamAclBoardVisibleOnlyToTeamMembers() throws Exception {
        long orgId = createOrg(ownerToken, "COMMERCIAL");
        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + memberEmail + "\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated());

        MvcResult teamResult = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/teams")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Frontend\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long teamId = objectMapper.readTree(teamResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/teams/" + teamId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + memberEmail + "\"}"))
                .andExpect(status().isCreated());

        long projectId = createProject(ownerToken, orgId);
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + memberUserId + ",\"role\":\"EDITOR\"}"))
                .andExpect(status().isCreated());

        // Member cannot create TEAM_ACL
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/boards")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Denied\",\"accessMode\":\"TEAM_ACL\",\"teamIds\":[" + teamId + "]}"))
                .andExpect(status().isForbidden());

        MvcResult board = mockMvc.perform(post("/api/v1/projects/" + projectId + "/boards")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"FE Board\",\"accessMode\":\"TEAM_ACL\",\"teamIds\":[" + teamId + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessMode").value("TEAM_ACL"))
                .andExpect(jsonPath("$.teamIds[0]").value((int) teamId))
                .andReturn();
        long boardId = objectMapper.readTree(board.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/boards/" + boardId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        // outsider not in project
        mockMvc.perform(get("/api/v1/boards/" + boardId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void suggestDodIsPreviewOnlyThenBulkConfirm() throws Exception {
        long projectId = createProject(ownerToken, null);
        long boardId = createBoard(ownerToken, projectId);
        long columnId = createColumn(ownerToken, boardId);

        MvcResult taskResult = mockMvc.perform(post("/api/v1/columns/" + columnId + "/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Login bug\",\"priority\":\"MEDIUM\",\"taskType\":\"BUG\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long taskId = objectMapper.readTree(taskResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult suggestion = mockMvc.perform(post("/api/v1/tasks/" + taskId + "/ai/suggest-dod")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].title").exists())
                .andReturn();

        mockMvc.perform(get("/api/v1/tasks/" + taskId + "/checklist")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        JsonNode items = objectMapper.readTree(suggestion.getResponse().getContentAsString()).get("items");
        StringBuilder bulkBody = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                bulkBody.append(',');
            }
            bulkBody.append("{\"title\":")
                    .append(objectMapper.writeValueAsString(items.get(i).get("title").asText()))
                    .append(",\"done\":false}");
        }
        bulkBody.append(']');

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/checklist/bulk")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkBody.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].title").exists());

        mockMvc.perform(get("/api/v1/boards/" + boardId + "/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }

    private String register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"secret123\",\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long meId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createOrg(String token, String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Org " + System.nanoTime() + "\",\"type\":\"" + type + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createProject(String token, Long organizationId) throws Exception {
        String body = organizationId == null
                ? "{\"name\":\"P\",\"description\":\"d\",\"withDefaultBoard\":false}"
                : "{\"name\":\"P\",\"description\":\"d\",\"withDefaultBoard\":false,\"organizationId\":"
                        + organizationId + "}";
        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createBoard(String token, long projectId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects/" + projectId + "/boards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Main\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createColumn(String token, long boardId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/boards/" + boardId + "/columns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Todo\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
