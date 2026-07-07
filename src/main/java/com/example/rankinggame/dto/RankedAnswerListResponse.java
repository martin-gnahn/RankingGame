package com.example.rankinggame.dto;

import java.util.List;

public record RankedAnswerListResponse(
        List<RankedAnswerDto> rankings
) {
}