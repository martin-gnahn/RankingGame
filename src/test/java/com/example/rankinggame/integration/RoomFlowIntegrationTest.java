package com.example.rankinggame.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomFlowIntegrationTest extends BackendIntegrationTest {
    @Test
    void createsRoomJoinsSecondPlayerAndReadsLobbyFromPostgres() throws Exception {
        String createRoomResponse = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"Marta\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").isString())
                .andExpect(jsonPath("$.roomId").isString())
                .andExpect(jsonPath("$.playerId").isString())
                .andExpect(jsonPath("$.nickname").value("Marta"))
                .andExpect(jsonPath("$.host").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String roomCode = JsonPath.read(createRoomResponse, "$.roomCode");

        mockMvc.perform(post("/api/rooms/{roomCode}/players", roomCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"Alex\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value(roomCode))
                .andExpect(jsonPath("$.nickname").value("Alex"))
                .andExpect(jsonPath("$.host").value(false));

        mockMvc.perform(get("/api/rooms/{roomCode}", roomCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value(roomCode))
                .andExpect(jsonPath("$.status").value("LOBBY"))
                .andExpect(jsonPath("$.players", hasSize(2)))
                .andExpect(jsonPath("$.players[0].nickname").value("Marta"))
                .andExpect(jsonPath("$.players[0].host").value(true))
                .andExpect(jsonPath("$.players[0].connectionStatus").value("CONNECTED"))
                .andExpect(jsonPath("$.players[1].nickname").value("Alex"))
                .andExpect(jsonPath("$.players[1].host").value(false))
                .andExpect(jsonPath("$.players[1].connectionStatus").value("CONNECTED"));
    }
}
