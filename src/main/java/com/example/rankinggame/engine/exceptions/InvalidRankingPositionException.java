package com.example.rankinggame.engine.exceptions;

public class InvalidRankingPositionException extends IllegalArgumentException {
    public InvalidRankingPositionException(int oneBasedPosition) {
        super("Ranking position must be positive (%d)".formatted(oneBasedPosition));
    }
}
