package com.example.rankinggame.engine.exceptions;

public class IncompleteRevealedRankedAnswerException extends IllegalArgumentException {
    public IncompleteRevealedRankedAnswerException(RevealedRankedAnswerField missingField) {
        super("Revealed ranked answer requires %s".formatted(missingField.label()));
    }
}
