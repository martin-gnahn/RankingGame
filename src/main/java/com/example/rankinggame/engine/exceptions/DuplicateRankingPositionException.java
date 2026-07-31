package com.example.rankinggame.engine.exceptions;

public class DuplicateRankingPositionException extends IllegalArgumentException {
    public DuplicateRankingPositionException(int oneBasedPosition) {
        super("Ranking position '%d' must be unique".formatted(oneBasedPosition));
    }
}
