package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.*;
import com.example.rankinggame.usecases.GetSubmittedAnswersService;
import com.example.rankinggame.usecases.SubmitAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answers")
public class AnswerController {
    private final SubmitAnswerService submitAnswerService;
    private final GetSubmittedAnswersService getSubmittedAnswersService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitAnswerResponse submitAnswer(
            @PathVariable String roomCode,
            @PathVariable UUID roundId,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        SubmitAnswerResult result = submitAnswerService.submitAnswer(new SubmitAnswerCommand(
                roomCode,
                roundId,
                request.playerId(),
                request.answerText()
        ));


        // TODO: Are SubmitAnswerResult and SubmitAnswerResponse redundant?
        return new SubmitAnswerResponse(
                result.answerId(),
                result.roundId(),
                result.playerId(),
                result.submitted()
        );
    }

    @GetMapping
    public SubmittedAnswersResponse getSubmittedAnswers(
            @PathVariable String roomCode,
            @PathVariable UUID roundId,
            @RequestParam UUID playerId
    ) {
        SubmittedAnswersResult result = getSubmittedAnswersService.getSubmittedAnswers(new GetSubmittedAnswersCommand(
                roomCode,
                roundId,
                playerId
        ));

        return new SubmittedAnswersResponse(result.answers().stream()
                .map(this::toResponse)
                .toList());
    }

    private SubmittedAnswerResponse toResponse(SubmittedAnswerResult answer) {
        return new SubmittedAnswerResponse(
                answer.answerId(),
                answer.playerId(),
                answer.nickname(),
                answer.answerText()
        );
    }
}
