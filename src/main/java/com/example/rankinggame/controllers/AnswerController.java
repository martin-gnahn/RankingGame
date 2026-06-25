package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.GetSubmittedAnswersCommand;
import com.example.rankinggame.dto.SubmitAnswerCommand;
import com.example.rankinggame.dto.SubmitAnswerRequest;
import com.example.rankinggame.dto.SubmitAnswerResponse;
import com.example.rankinggame.dto.SubmitAnswerResult;
import com.example.rankinggame.dto.SubmittedAnswerResponse;
import com.example.rankinggame.dto.SubmittedAnswerResult;
import com.example.rankinggame.dto.SubmittedAnswersResponse;
import com.example.rankinggame.dto.SubmittedAnswersResult;
import com.example.rankinggame.usecases.GetSubmittedAnswersService;
import com.example.rankinggame.usecases.SubmitAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
                answer.answerText(),
                answer.cardValue()
        );
    }
}
