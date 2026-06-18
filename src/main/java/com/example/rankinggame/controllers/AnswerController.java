package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.SubmitAnswerCommand;
import com.example.rankinggame.dto.SubmitAnswerRequest;
import com.example.rankinggame.dto.SubmitAnswerResponse;
import com.example.rankinggame.dto.SubmitAnswerResult;
import com.example.rankinggame.usecases.SubmitAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers")
public class AnswerController {
    private final SubmitAnswerService submitAnswerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitAnswerResponse submitAnswer(
            @PathVariable String roomCode,
            @PathVariable UUID roundId,
            @Valid @RequestBody(required = false) SubmitAnswerRequest request
    ) {
        SubmitAnswerResult result = submitAnswerService.submitAnswer(new SubmitAnswerCommand(
                roomCode,
                roundId,
                request == null ? null : request.playerId(),
                request == null ? null : request.answerText()
        ));

        return new SubmitAnswerResponse(
                result.answerId(),
                result.roundId(),
                result.playerId(),
                result.submitted()
        );
    }
}
