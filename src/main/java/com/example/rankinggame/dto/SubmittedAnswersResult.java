package com.example.rankinggame.dto;

import java.util.List;

public record SubmittedAnswersResult(
        List<SubmittedAnswerResult> answers
) {
}
