package com.example.rankinggame.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
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

    @Test
    void hostCanQueryBothSubmittedAnswersAfterAllPlayersAnswered() throws Exception {
        String createRoomResponse = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"Marta\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String roomCode = JsonPath.read(createRoomResponse, "$.roomCode");
        String hostPlayerId = JsonPath.read(createRoomResponse, "$.playerId");

        String joinRoomResponse = mockMvc.perform(post("/api/rooms/{roomCode}/players", roomCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"Alex\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String guestPlayerId = JsonPath.read(joinRoomResponse, "$.playerId");

        String startGameResponse = mockMvc.perform(post("/api/rooms/{roomCode}/ranking-game/start", roomCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostPlayerId\":\"" + hostPlayerId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String roundId = JsonPath.read(startGameResponse, "$.roundId");

        mockMvc.perform(post("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers", roomCode, roundId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "answerText": "Answer1"
                                }
                                """.formatted(hostPlayerId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers", roomCode, roundId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "answerText": "Answer2"
                                }
                                """.formatted(guestPlayerId)))
                .andExpect(status().isCreated());

        String roundState = jdbcTemplate.queryForObject(
                "SELECT state FROM rounds WHERE id = ?",
                String.class,
                UUID.fromString(roundId)
        );
        org.assertj.core.api.Assertions.assertThat(roundState).isEqualTo("SORTING");

        mockMvc.perform(get("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers", roomCode, roundId)
                        .param("playerId", hostPlayerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers", hasSize(2)))
                .andExpect(jsonPath("$.answers[*].answerText", containsInAnyOrder("Answer1", "Answer2")))
                .andExpect(jsonPath("$.answers[*].playerId", containsInAnyOrder(hostPlayerId, guestPlayerId)));

        mockMvc.perform(get("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers", roomCode, roundId)
                        .param("playerId", guestPlayerId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Only the host can query submitted answers"));
    }
}
