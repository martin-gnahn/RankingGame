package com.example.rankinggame.engine.exceptions;

public enum RankingAssessmentInput {
    CARD_VALUE_INFOS("card value infos"),
    CARD_VALUE_INFO_ENTRIES("card value info entries"),
    RANKED_ANSWERS("ranked answers"),
    RANKED_ANSWER_ENTRIES("ranked answer entries"),
    SUBMITTED_ANSWERS_INSIDE_RANKED_ANSWERS("submitted answers inside ranked answers");

    private final String label;

    RankingAssessmentInput(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }
}
