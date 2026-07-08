package com.example.rankinggame.dto;

import com.example.rankinggame.engine.AnswerId;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.RankingId;
import com.example.rankinggame.engine.RoundId;

public record RankAnswerResult(
        RoundId roundId,
        PlayerId playerId,
        AnswerId answerId,
        RankingId rankingId,
        int oneBasedPosition
) {
}
