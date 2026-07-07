package com.example.rankinggame.engine.exceptions;

public class NegativePointDecreaseNotAllowedException extends IllegalArgumentException {
    public NegativePointDecreaseNotAllowedException(int amount) {
        super("Point decrease amount must not be negative (%d)".formatted(amount));
    }
}
