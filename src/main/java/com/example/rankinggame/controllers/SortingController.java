package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.SortAnswersCommand;
import com.example.rankinggame.dto.SortAnswersRequest;
import com.example.rankinggame.usecases.SortAnswersService;
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
@RequestMapping("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/sort")
public class SortingController {
    private final SortAnswersService sortAnswersService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sortAnswers(
            @PathVariable String roomCode,
            @PathVariable UUID roundId,
            @Valid @RequestBody(required = false) SortAnswersRequest request
    ) {
        sortAnswersService.sortAnswers(new SortAnswersCommand(
                roomCode,
                roundId,
                request == null ? null : request.hostPlayerId()
        ));
    }
}
