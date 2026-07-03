package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.SortAnswerRequest;
import com.example.rankinggame.dto.SortAnswersCommand;
import com.example.rankinggame.entities.RankingEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rooms/{roomCode}/ranking-game/rounds/{roundId}/answer/position")
public class SortController {
    private final SortAnswerService sortAnswerService;

    @PostMapping("/new")
    public HttpEntity<RankingEntity> addRanking(
            @PathVariable String roomCode,
            @PathVariable UUID roundId,
            @Valid @RequestBody SortAnswerRequest request
    ) {
        RankingEntity rankingEntity = sortAnswerService.addRanking(new SortAnswersCommand(
                roomCode,
                roundId,
                request.hostId(),
                request.answerId()
        ));
        return ResponseEntity.ok(rankingEntity);
    }

    @GetMapping("/all")
    public List<RankingEntity> getOrderOfAnswers(
            @PathVariable String roomCode,
            @PathVariable UUID roundId,
            @RequestParam UUID playerId
    ) {
        return sortAnswerService.getOrderOfAnswers(new GetAnswerOrderCommand(
                roomCode,
                roundId,
                playerId
        ));
    }


}
