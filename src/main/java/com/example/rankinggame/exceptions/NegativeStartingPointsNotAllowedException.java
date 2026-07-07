package com.example.rankinggame.exceptions;


public class NegativeStartingPointsNotAllowedException extends RuntimeException {
    public NegativeStartingPointsNotAllowedException(int startingPoints) {
        super("Starting points must not be negative (%d).".formatted(startingPoints));
    }
}
