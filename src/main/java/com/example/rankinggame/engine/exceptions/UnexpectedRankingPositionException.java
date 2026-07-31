package com.example.rankinggame.engine.exceptions;

public class UnexpectedRankingPositionException extends IllegalArgumentException {
    public UnexpectedRankingPositionException(int expectedPosition, int actualPosition) {
        super("Expected ranking position '%d' but found '%d'".formatted(expectedPosition, actualPosition));
    }
}
