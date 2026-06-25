package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.SubmitAnswerCommand;
import com.example.rankinggame.dto.SubmitAnswerResult;
import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.usecases.SubmitAnswerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
        MockMvc mockMvc = mockMvc(submitAnswerService);

        mockMvc.perform(post("/api/rooms/ABCD12/ranking-game/rounds/" + roundId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "answerText": "Mit WLAN-Problemen."
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
        MockMvc mockMvc = mockMvc(submitAnswerService);

        mockMvc.perform(post("/api/rooms/ABCD12/ranking-game/rounds/" + roundId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "answerText": "Mit WLAN-Problemen."
                                }
                                """.formatted(playerId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ANSWER_ALREADY_SUBMITTED"))
                .andExpect(jsonPath("$.message").value("Player already submitted an answer for this round"));
    }

    private MockMvc mockMvc(SubmitAnswerService submitAnswerService) {
        return MockMvcBuilders.standaloneSetup(new AnswerController(submitAnswerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
