package com.example.rankinggame.engine.exceptions;

public class IncompleteCardValueInfoException extends IllegalArgumentException {
    public IncompleteCardValueInfoException(CardValueInfoField missingField) {
        super("Card value info requires %s".formatted(missingField.label()));
    }
}
