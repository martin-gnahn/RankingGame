package com.example.rankinggame.dto;

import com.example.rankinggame.engine.AnswerId;
import com.example.rankinggame.engine.AnswerText;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.RankingId;

public record RankedAnswerDto(
        RankingId rankingId,
        AnswerId answerId,
        PlayerId playerId,
        AnswerText answerText,
        int oneBasedPosition
) {
}