package com.spe.calculator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full application context (including Actuator) and hits real endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CalculatorIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void contextLoadsAndAddWorksEndToEnd() throws Exception {
        mvc.perform(get("/api/v1/add").param("a", "0.1").param("b", "0.2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(0.3));
    }

    @Test
    void actuatorHealthIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void unknownEndpointIs404() throws Exception {
        mvc.perform(get("/api/v1/power").param("a", "2").param("b", "3"))
                .andExpect(status().isNotFound());
    }
}
