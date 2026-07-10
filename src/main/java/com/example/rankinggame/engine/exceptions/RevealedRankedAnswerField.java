package com.example.rankinggame.engine.exceptions;

public enum RevealedRankedAnswerField {
    SUBMITTED_ANSWER("submitted answer"),
    CARD_NUMBER("card number");

    private final String label;

    RevealedRankedAnswerField(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }
}
