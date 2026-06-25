package com.example.rankinggame.engine.exceptions;

public class CannotUseSameQuestionAgainException extends RuntimeException {
    public CannotUseSameQuestionAgainException() {
        super("Question was already used in this game");
    }
}
