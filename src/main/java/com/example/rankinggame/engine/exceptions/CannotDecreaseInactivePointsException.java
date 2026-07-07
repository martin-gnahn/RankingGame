package com.example.rankinggame.engine.exceptions;

public class CannotDecreaseInactivePointsException extends RuntimeException {
    public CannotDecreaseInactivePointsException() {
        super("Cannot decrease inactive game points");
    }
}
