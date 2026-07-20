package com.example.rankinggame.engine.exceptions;

public class NegativePenaltyPointsException extends IllegalArgumentException {
    public NegativePenaltyPointsException(int penaltyPoints) {
        super("Penalty points must not be negative (%d)".formatted(penaltyPoints));
    }
}
