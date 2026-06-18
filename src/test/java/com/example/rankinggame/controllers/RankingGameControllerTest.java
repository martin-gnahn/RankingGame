package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.ActiveRoundResult;
import com.example.rankinggame.dto.StartRankingGameCommand;
import com.example.rankinggame.dto.StartRankingGameResult;
import com.example.rankinggame.entities.GameType;
import com.example.rankinggame.usecases.GetActiveRoundService;
import com.example.rankinggame.usecases.StartRankingGameService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RankingGameControllerTest {
    @Test
    void startsRankingGameViaApiEndpoint() throws Exception {
        StartRankingGameService startRankingGameService = mock(StartRankingGameService.class);
        GetActiveRoundService getActiveRoundService = mock(GetActiveRoundService.class);
        UUID roomId = UUID.randomUUID();
        UUID hostPlayerId = UUID.randomUUID();
        UUID gameSessionId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        when(startRankingGameService.startGame(any(StartRankingGameCommand.class)))
                .thenReturn(new StartRankingGameResult(
                        roomId,
                        "ABCD12",
                        gameSessionId,
                        GameType.RANKING_GAME,
                        roundId,
                        1,
                        questionId
                ));
        MockMvc mockMvc = mockMvc(startRankingGameService, getActiveRoundService);

        mockMvc.perform(post("/api/rooms/abcd12/ranking-game/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostPlayerId\":\"" + hostPlayerId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(roomId.toString()))
                .andExpect(jsonPath("$.roomCode").value("ABCD12"))
                .andExpect(jsonPath("$.gameSessionId").value(gameSessionId.toString()))
                .andExpect(jsonPath("$.gameType").value("RANKING_GAME"))
                .andExpect(jsonPath("$.roundId").value(roundId.toString()))
                .andExpect(jsonPath("$.roundNumber").value(1))
                .andExpect(jsonPath("$.questionId").value(questionId.toString()));

        ArgumentCaptor<StartRankingGameCommand> commandCaptor = ArgumentCaptor.forClass(StartRankingGameCommand.class);
        org.mockito.Mockito.verify(startRankingGameService).startGame(commandCaptor.capture());
        assertThat(commandCaptor.getValue().roomCode()).isEqualTo("abcd12");
        assertThat(commandCaptor.getValue().hostPlayerId()).isEqualTo(hostPlayerId);
    }

    @Test
    void rejectsMissingStartGameHostPlayerId() throws Exception {
        MockMvc mockMvc = mockMvc(mock(StartRankingGameService.class), mock(GetActiveRoundService.class));

        mockMvc.perform(post("/api/rooms/ABCD12/ranking-game/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Host player id is required"));
    }

    @Test
    void returnsCurrentRankingGameRoundViaApiEndpoint() throws Exception {
        StartRankingGameService startRankingGameService = mock(StartRankingGameService.class);
        GetActiveRoundService getActiveRoundService = mock(GetActiveRoundService.class);
        UUID roomId = UUID.randomUUID();
        UUID gameSessionId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(getActiveRoundService.getActiveRound("ABCD12", playerId)).thenReturn(new ActiveRoundResult(
                roomId,
                "ABCD12",
                gameSessionId,
                roundId,
                1,
                questionId,
                "Welche Ausrede funktioniert immer?",
                7
        ));
        MockMvc mockMvc = mockMvc(startRankingGameService, getActiveRoundService);

        mockMvc.perform(get("/api/rooms/ABCD12/ranking-game/current-round")
                        .param("playerId", playerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(roomId.toString()))
                .andExpect(jsonPath("$.roomCode").value("ABCD12"))
                .andExpect(jsonPath("$.gameSessionId").value(gameSessionId.toString()))
                .andExpect(jsonPath("$.roundId").value(roundId.toString()))
                .andExpect(jsonPath("$.roundNumber").value(1))
                .andExpect(jsonPath("$.questionId").value(questionId.toString()))
                .andExpect(jsonPath("$.questionText").value("Welche Ausrede funktioniert immer?"))
                .andExpect(jsonPath("$.assignedCardValue").value(7));
    }

    private MockMvc mockMvc(
            StartRankingGameService startRankingGameService,
            GetActiveRoundService getActiveRoundService
    ) {
        return MockMvcBuilders.standaloneSetup(new RankingGameController(
                        new ObjectMapper(),
                        startRankingGameService,
                        getActiveRoundService
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
