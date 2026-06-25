package com.example.rankinggame.config;

import com.example.rankinggame.integration.BackendIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiDocumentationTest extends BackendIntegrationTest {
    @Test
    void exposesOpenApiSpecification() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("RankingGame API"))
                .andExpect(jsonPath("$['paths']['/health']").exists())
                .andExpect(jsonPath("$['paths']['/api/rooms']").exists())
                .andExpect(jsonPath("$['paths']['/products']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/products/{id}']").doesNotExist());
    }

    @Test
    void servesSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }

    @Test
    void doesNotServeProductEndpoints() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/products"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/products/1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isNotFound());
    }
}
