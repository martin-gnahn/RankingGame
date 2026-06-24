package com.example.rankinggame.usecases;

public class RoundIdRequiredException extends RuntimeException {
    public RoundIdRequiredException() {
        super("Round id is required");
    }
}
