package com.example.rankinggame.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class Ranking {
    private RankingId id;
    private SubmittedAnswer answer;
    private int oneBasedPosition;
}