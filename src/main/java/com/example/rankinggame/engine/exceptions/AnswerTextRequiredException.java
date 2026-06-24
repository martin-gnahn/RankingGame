package com.example.rankinggame.engine.exceptions;

public class AnswerTextRequiredException extends IllegalArgumentException {
    public AnswerTextRequiredException() {
        super("Answer text is required");
    }
}
