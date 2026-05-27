package org.apache.seata.namingserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowStaticJsonResourcesWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/version.json")).andExpect(status().isOk());
    }

    @Test
    void shouldAllowSagaDesignerResourcesWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/saga-statemachine-designer/index.html")).andExpect(result -> {
            int statusCode = result.getResponse().getStatus();
            assertTrue(
                    statusCode == 200 || statusCode == 404,
                    "Bypassed resources should return 200 or 404, but got: " + statusCode);
        });
    }

    @Test
    void shouldSecureProtectedApiEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/console/users")).andExpect(status().isUnauthorized());
    }
}
