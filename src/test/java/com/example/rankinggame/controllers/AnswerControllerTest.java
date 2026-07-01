package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.*;
import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.usecases.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static com.example.rankinggame.controllers.ErrorConstants.VALIDATION_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnswerControllerTest {
    @Mock
    private SubmitAnswerService submitAnswerService;

    @Mock
    private GetSubmittedAnswersService getSubmittedAnswersService;

    @InjectMocks
    private AnswerController answerController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(answerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsBadRequestWhenAnswerTextIsBlank() throws Exception {
        UUID roundId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        mockMvc.perform(post("/api/rooms/ABCD12/ranking-game/rounds/" + roundId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitAnswerRequest(playerId, " ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(VALIDATION_ERROR));
    }

    @Test
    void submitsAnswerViaApiEndpoint() throws Exception {
        UUID roundId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        when(submitAnswerService.submitAnswer(any(SubmitAnswerCommand.class)))
                .thenReturn(new SubmitAnswerResult(answerId, roundId, playerId, true));

        mockMvc.perform(post("/api/rooms/ABCD12/ranking-game/rounds/" + roundId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitAnswerRequest(playerId, "Mit WLAN-Problemen.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answerId").value(answerId.toString()))
                .andExpect(jsonPath("$.roundId").value(roundId.toString()))
                .andExpect(jsonPath("$.playerId").value(playerId.toString()))
                .andExpect(jsonPath("$.submitted").value(true));

        ArgumentCaptor<SubmitAnswerCommand> commandCaptor = ArgumentCaptor.forClass(SubmitAnswerCommand.class);
        org.mockito.Mockito.verify(submitAnswerService).submitAnswer(commandCaptor.capture());
        assertThat(commandCaptor.getValue().roomCode()).isEqualTo("ABCD12");
        assertThat(commandCaptor.getValue().roundId()).isEqualTo(roundId);
        assertThat(commandCaptor.getValue().playerId()).isEqualTo(playerId);
        assertThat(commandCaptor.getValue().answerText()).isEqualTo("Mit WLAN-Problemen.");
    }

    @Test
    void returnsConflictWhenAnswerWasAlreadySubmitted() throws Exception {
        UUID roundId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(submitAnswerService.submitAnswer(any(SubmitAnswerCommand.class)))
                .thenThrow(new AnswerAlreadySubmittedException());

        mockMvc.perform(post("/api/rooms/ABCD12/ranking-game/rounds/" + roundId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitAnswerRequest(playerId, "Mit WLAN-Problemen.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ANSWER_ALREADY_SUBMITTED"))
                .andExpect(jsonPath("$.message").value("Player already submitted an answer for this round"));
    }

    @Test
    void returnsForbiddenWhenSubmittingPlayerIsOutsideRoom() throws Exception {
        UUID roundId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(submitAnswerService.submitAnswer(any(SubmitAnswerCommand.class)))
                .thenThrow(new PlayerNotInRoomException());

        mockMvc.perform(post("/api/rooms/ABCD12/ranking-game/rounds/" + roundId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitAnswerRequest(playerId, "Mit WLAN-Problemen.")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Player is not part of this room"));
    }

    @Test
    void returnsConflictWhenSubmittingToRoundOutsideActiveGame() throws Exception {
        UUID roundId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(submitAnswerService.submitAnswer(any(SubmitAnswerCommand.class)))
                .thenThrow(new RoundNotPartOfActiveGameException());

        mockMvc.perform(post("/api/rooms/ABCD12/ranking-game/rounds/" + roundId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitAnswerRequest(playerId, "Mit WLAN-Problemen.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_STATE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Round is not part of the active game"));
    }

    @Test
    void returnsSubmittedAnswersViaApiEndpoint() throws Exception {
        UUID roundId = UUID.randomUUID();
        UUID hostPlayerId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(getSubmittedAnswersService.getSubmittedAnswers(any(GetSubmittedAnswersCommand.class)))
                .thenReturn(new SubmittedAnswersResult(List.of(new SubmittedAnswerResult(
                        answerId,
                        playerId,
                        "Alex",
                        "Mit WLAN-Problemen.",
                        7
                ))));

        mockMvc.perform(get("/api/rooms/ABCD12/ranking-game/rounds/" + roundId + "/answers")
                        .param("playerId", hostPlayerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers[0].answerId").value(answerId.toString()))
                .andExpect(jsonPath("$.answers[0].playerId").value(playerId.toString()))
                .andExpect(jsonPath("$.answers[0].nickname").value("Alex"))
                .andExpect(jsonPath("$.answers[0].answerText").value("Mit WLAN-Problemen."))
                .andExpect(jsonPath("$.answers[0].cardValue").value(7));

        ArgumentCaptor<GetSubmittedAnswersCommand> commandCaptor = ArgumentCaptor.forClass(GetSubmittedAnswersCommand.class);
        org.mockito.Mockito.verify(getSubmittedAnswersService).getSubmittedAnswers(commandCaptor.capture());
        assertThat(commandCaptor.getValue().roomCode()).isEqualTo("ABCD12");
        assertThat(commandCaptor.getValue().roundId()).isEqualTo(roundId);
        assertThat(commandCaptor.getValue().requesterPlayerId()).isEqualTo(hostPlayerId);
    }

    @Test
    void returnsForbiddenWhenNonRoomPlayerQueriesSubmittedAnswers() throws Exception {
        UUID roundId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(getSubmittedAnswersService.getSubmittedAnswers(any(GetSubmittedAnswersCommand.class)))
                .thenThrow(new OnlyRoomPlayersCanQueryAnswers());

        mockMvc.perform(get("/api/rooms/ABCD12/ranking-game/rounds/" + roundId + "/answers")
                        .param("playerId", playerId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Only players in this room can query submitted answers"));
    }

    private String submitAnswerRequest(UUID playerId, String answerText) {
        return objectMapper.writeValueAsString(new SubmitAnswerRequest(playerId, answerText));
    }
}
