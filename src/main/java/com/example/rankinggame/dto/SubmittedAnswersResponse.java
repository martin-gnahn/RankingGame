package com.example.rankinggame.dto;

import java.util.List;

public record SubmittedAnswersResponse(
        List<SubmittedAnswerResponse> answers
) {
}
