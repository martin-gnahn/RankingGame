package com.example.rankinggame.engine.exceptions;

public class InvalidCardValueException extends IllegalArgumentException {
    public InvalidCardValueException(int minValue, int maxValue) {
        super("Card value must be between " + minValue + " and " + maxValue);
    }
}
