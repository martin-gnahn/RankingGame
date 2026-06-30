package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.GetSubmittedAnswersCommand;
import com.example.rankinggame.dto.SubmitAnswerCommand;
import com.example.rankinggame.dto.SubmitAnswerResult;
import com.example.rankinggame.dto.SubmittedAnswerResult;
import com.example.rankinggame.dto.SubmittedAnswersResult;
import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.usecases.GetSubmittedAnswersService;
import com.example.rankinggame.usecases.OnlyRoomPlayersCanQueryAnswers;
import com.example.rankinggame.usecases.SubmitAnswerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnswerControllerTest {
    @Test
    void submitsAnswerViaApiEndpoint() throws Exception {
        SubmitAnswerService submitAnswerService = mock(SubmitAnswerService.class);
        UUID roundId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        when(submitAnswerService.submitAnswer(any(SubmitAnswerCommand.class)))
                .thenReturn(new SubmitAnswerResult(answerId, roundId, playerId, true));
        MockMvc mockMvc = mockMvc(submitAnswerService, mock(GetSubmittedAnswersService.class));

        mockMvc.perform(post("/api/rooms/ABCD12/ranking-game/rounds/" + roundId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "value": "Mit WLAN-Problemen."
                                }
                                """.formatted(playerId)))
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
        SubmitAnswerService submitAnswerService = mock(SubmitAnswerService.class);
        UUID roundId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(submitAnswerService.submitAnswer(any(SubmitAnswerCommand.class)))
                .thenThrow(new AnswerAlreadySubmittedException());
        MockMvc mockMvc = mockMvc(submitAnswerService, mock(GetSubmittedAnswersService.class));

        mockMvc.perform(post("/api/rooms/ABCD12/ranking-game/rounds/" + roundId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "value": "Mit WLAN-Problemen."
                                }
                                """.formatted(playerId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ANSWER_ALREADY_SUBMITTED"))
                .andExpect(jsonPath("$.message").value("Player already submitted an answer for this round"));
    }

    @Test
    void returnsSubmittedAnswersViaApiEndpoint() throws Exception {
        SubmitAnswerService submitAnswerService = mock(SubmitAnswerService.class);
        GetSubmittedAnswersService getSubmittedAnswersService = mock(GetSubmittedAnswersService.class);
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
        MockMvc mockMvc = mockMvc(submitAnswerService, getSubmittedAnswersService);

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
        SubmitAnswerService submitAnswerService = mock(SubmitAnswerService.class);
        GetSubmittedAnswersService getSubmittedAnswersService = mock(GetSubmittedAnswersService.class);
        UUID roundId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(getSubmittedAnswersService.getSubmittedAnswers(any(GetSubmittedAnswersCommand.class)))
                .thenThrow(new OnlyRoomPlayersCanQueryAnswers());
        MockMvc mockMvc = mockMvc(submitAnswerService, getSubmittedAnswersService);

        mockMvc.perform(get("/api/rooms/ABCD12/ranking-game/rounds/" + roundId + "/answers")
                        .param("playerId", playerId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Only players in this room can query submitted answers"));
    }

    private MockMvc mockMvc(
            SubmitAnswerService submitAnswerService,
            GetSubmittedAnswersService getSubmittedAnswersService
    ) {
        return MockMvcBuilders.standaloneSetup(new AnswerController(submitAnswerService, getSubmittedAnswersService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
