package chat.hbcit.devicemanagebackend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiAssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void suggestionsShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/ai/suggestions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @WithMockUser(username = "10", roles = {"USER"})
    void suggestionsShouldReturnUserQuestionsForNormalUser() throws Exception {
        mockMvc.perform(get("/ai/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0]", containsString("借用")));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void suggestionsShouldReturnAdminQuestionsForAdmin() throws Exception {
        mockMvc.perform(get("/ai/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[1]", containsString("待审批")));
    }

    @Test
    @WithMockUser(username = "10", roles = {"USER"})
    void streamChatShouldReturn400WhenMessageBlank() throws Exception {
        mockMvc.perform(post("/ai/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
