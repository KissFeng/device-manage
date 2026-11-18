package chat.hbcit.devicemanagebackend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ControllerSecurityAndApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void categoriesShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @WithMockUser(username = "10", roles = {"USER"})
    void adminEndpointShouldReturn403ForNormalUser() throws Exception {
        mockMvc.perform(get("/admin/borrow/pending"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void loginShouldReturn400WhenValidationFails() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "10", roles = {"USER"})
    void categoriesAndDevicesShouldReturn200ForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/devices").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void overdueSummaryShouldReturn200ForAdmin() throws Exception {
        mockMvc.perform(get("/admin/borrow/overdue-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.overdueCount").exists());
    }

    @Test
    void openApiAndActuatorHealthShouldBeAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths").isMap());
    }
}
