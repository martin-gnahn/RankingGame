package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.AddRankingPositionCommand;
import com.example.rankinggame.dto.AddRankingPositionRequest;
import com.example.rankinggame.dto.RankedAnswerDto;
import com.example.rankinggame.dto.RankedAnswerListResponse;
import com.example.rankinggame.usecases.RankAnswerResult;
import com.example.rankinggame.usecases.RankAnswerService;
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
public class RankAnswerController {
    private final RankAnswerService rankAnswerService;

    @PostMapping("/new")
    public HttpEntity<RankAnswerResult> addRankingPosition(
            @PathVariable String roomCode,
            @PathVariable UUID roundId,
            @Valid @RequestBody AddRankingPositionRequest request
    ) {
        RankAnswerResult rankAnswerResult = rankAnswerService.addRankingPosition(new AddRankingPositionCommand(
                roomCode,
                roundId,
                request.hostId(),
                request.answerId()
        ));
        return ResponseEntity.ok(rankAnswerResult);
    }

    @GetMapping("/all")
    public RankedAnswerListResponse getOrderOfAnswers(
            @PathVariable String roomCode,
            @PathVariable UUID roundId,
            @RequestParam UUID playerId
    ) {
        List<RankedAnswerDto> rankingPositions = rankAnswerService.getRankingPositions(new GetRankingPositionsCommand(
                roomCode,
                roundId,
                playerId
        ));
        return new RankedAnswerListResponse(rankingPositions);
    }


}
