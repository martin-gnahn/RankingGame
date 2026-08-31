package com.example.rankinggame.controllers;

import com.example.rankinggame.auth.AuthenticatedPlayer;
import com.example.rankinggame.auth.PlayerSessionService;
import com.example.rankinggame.dto.ActiveRoundResult;
import com.example.rankinggame.dto.StartGameResult;
import com.example.rankinggame.dto.StartRankingGameCommand;
import com.example.rankinggame.engine.AnswerText;
import com.example.rankinggame.entities.GameType;
import com.example.rankinggame.exceptions.RoomHasNoActiveGameException;
import com.example.rankinggame.exceptions.RoomNotInLobbyException;
import com.example.rankinggame.usecases.GetActiveRoundService;
import com.example.rankinggame.usecases.OnlyHostCanStartGame;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RankedAnswerGameControllerTest {
    @Test
    void startsRankingGameViaApiEndpoint() throws Exception {
        ControllerFixture fixture = controllerFixture();
        UUID roomId = UUID.randomUUID();
        UUID gameSessionId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        when(fixture.startRankingGameService().startGame(any(StartRankingGameCommand.class)))
                .thenReturn(new StartGameResult(
                        roomId,
                        "ABCD12",
                        gameSessionId,
                        GameType.RANKING_GAME,
                        roundId,
                        1,
                        questionId
                ));
        MockMvc mockMvc = mockMvc(fixture.startRankingGameService, fixture.getActiveRoundService, fixture.authenticatedPlayerId());

        mockMvc.perform(post("/api/rooms/abcd12/ranking-game/start")
                        .header("X-Player-Session-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value("ABCD12"))
                .andExpect(jsonPath("$.roomId").doesNotExist())
                .andExpect(jsonPath("$.gameSessionId").doesNotExist())
                .andExpect(jsonPath("$.roundId").doesNotExist());

        ArgumentCaptor<StartRankingGameCommand> commandCaptor = ArgumentCaptor.forClass(StartRankingGameCommand.class);
        org.mockito.Mockito.verify(fixture.startRankingGameService()).startGame(commandCaptor.capture());
        assertThat(commandCaptor.getValue().roomCode()).isEqualTo("abcd12");
        assertThat(commandCaptor.getValue().hostPlayerId()).isEqualTo(fixture.authenticatedPlayerId());
    }

    @Test
    void startsRankingGameWithoutRequestBodyIdentity() throws Exception {
        StartRankingGameService startRankingGameService = mock(StartRankingGameService.class);
        GetActiveRoundService getActiveRoundService = mock(GetActiveRoundService.class);
        UUID playerId = UUID.randomUUID();
        when(startRankingGameService.startGame(any(StartRankingGameCommand.class)))
                .thenReturn(new StartGameResult(
                        UUID.randomUUID(),
                        "ABCD12",
                        UUID.randomUUID(),
                        GameType.RANKING_GAME,
                        UUID.randomUUID(),
                        1,
                        UUID.randomUUID()
                ));
        MockMvc mockMvc = mockMvc(startRankingGameService, getActiveRoundService, playerId);

        mockMvc.perform(post("/api/rooms/ABCD12/ranking-game/start")
                        .header("X-Player-Session-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    @Test
    void returnsForbiddenWhenNonHostStartsRankingGame() throws Exception {
        ControllerFixture fixture = controllerFixture();
        when(fixture.startRankingGameService().startGame(any(StartRankingGameCommand.class)))
                .thenThrow(new OnlyHostCanStartGame());
        MockMvc mockMvc = mockMvc(fixture.startRankingGameService, fixture.getActiveRoundService, fixture.authenticatedPlayerId());

        mockMvc.perform(post("/api/rooms/ABCD12/ranking-game/start")
                        .header("X-Player-Session-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorKey").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Only the host can start the game"));
    }

    @Test
    void returnsConflictWhenStartingOutsideLobby() throws Exception {
        ControllerFixture fixture = controllerFixture();
        when(fixture.startRankingGameService().startGame(any(StartRankingGameCommand.class)))
                .thenThrow(new RoomNotInLobbyException("ABCD12"));
        MockMvc mockMvc = mockMvc(fixture.startRankingGameService, fixture.getActiveRoundService, fixture.authenticatedPlayerId());

        mockMvc.perform(post("/api/rooms/ABCD12/ranking-game/start")
                        .header("X-Player-Session-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorKey").value("GAME_STATE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Room 'ABCD12' is not in lobby."));
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
        boolean currentPlayerIsCaptain = false;
        when(getActiveRoundService.loadActiveRoundForPlayer("ABCD12", playerId)).thenReturn(new ActiveRoundResult(
                roomId,
                "ABCD12",
                gameSessionId,
                roundId,
                1,
                questionId,
                "Welche Ausrede funktioniert immer?",
                7,
                false,
                null,
                currentPlayerIsCaptain));
        MockMvc mockMvc = mockMvc(startRankingGameService, getActiveRoundService, playerId);

        mockMvc.perform(get("/api/rooms/ABCD12/ranking-game/current-round")
                        .header("X-Player-Session-Token", "token"))
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

    @Test
    void returnsCurrentRoundWithUserAnswer() throws Exception {
        ControllerFixture fixture = controllerFixture();
        CurrentRoundFixture currentRound = currentRound();
        String submittedAnswer = "Mit WLAN-Problemen.";
        when(fixture.getActiveRoundService().loadActiveRoundForPlayer("ABCD12", fixture.authenticatedPlayerId()))
                .thenReturn(currentRound.result(true, new AnswerText(submittedAnswer)));

        fixture.mockMvc().perform(get("/api/rooms/ABCD12/ranking-game/current-round")
                        .header("X-Player-Session-Token", "token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPlayerSubmitted").value(true))
                .andExpect(jsonPath("$.submittedAnswerByPlayer").value(submittedAnswer));
    }

    @Test
    void returnsConflictWhenCurrentRoundHasNoActiveGame() throws Exception {
        ControllerFixture fixture = controllerFixture();
        when(fixture.getActiveRoundService()
                .loadActiveRoundForPlayer("ABCD12", fixture.authenticatedPlayerId()))
                .thenThrow(new RoomHasNoActiveGameException("ABCD12"));
        MockMvc mockMvc = mockMvc(fixture.startRankingGameService, fixture.getActiveRoundService, fixture.authenticatedPlayerId());

        mockMvc.perform(get("/api/rooms/ABCD12/ranking-game/current-round")
                        .header("X-Player-Session-Token", "token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorKey").value("GAME_STATE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Room 'ABCD12' has no active game."));
    }

    private ControllerFixture controllerFixture() {
        UUID authenticatedPlayerId = UUID.randomUUID();
        StartRankingGameService startRankingGameService = mock(StartRankingGameService.class);
        GetActiveRoundService getActiveRoundService = mock(GetActiveRoundService.class);
        return new ControllerFixture(
                startRankingGameService,
                getActiveRoundService,
                authenticatedPlayerId,
                mockMvc(startRankingGameService, getActiveRoundService, authenticatedPlayerId)
        );
    }

    private CurrentRoundFixture currentRound() {
        return new CurrentRoundFixture(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }

    private MockMvc mockMvc(
            StartRankingGameService startRankingGameService,
            GetActiveRoundService getActiveRoundService,
            UUID authenticatedPlayerId
    ) {
        PlayerSessionService playerSessionService = mock(PlayerSessionService.class);
        when(playerSessionService.authenticatePlayer(anyString(), anyString()))
                .thenReturn(new AuthenticatedPlayer(authenticatedPlayerId));

        return MockMvcBuilders.standaloneSetup(new GameController(
                        new ObjectMapper(),
                        startRankingGameService,
                        getActiveRoundService,
                        playerSessionService
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private record ControllerFixture(
            StartRankingGameService startRankingGameService,
            GetActiveRoundService getActiveRoundService,
            UUID authenticatedPlayerId,
            MockMvc mockMvc
    ) {
    }

    private record CurrentRoundFixture(
            UUID roomId,
            UUID gameSessionId,
            UUID roundId,
            UUID questionId
    ) {
        private ActiveRoundResult result(
                boolean currentPlayerSubmitted,
                AnswerText submittedAnswerByPlayer
        ) {
            return new ActiveRoundResult(
                    roomId,
                    "ABCD12",
                    gameSessionId,
                    roundId,
                    1,
                    questionId,
                    "Welche Ausrede funktioniert immer?",
                    7,
                    currentPlayerSubmitted,
                    submittedAnswerByPlayer,
                    false
            );
        }
    }
}
