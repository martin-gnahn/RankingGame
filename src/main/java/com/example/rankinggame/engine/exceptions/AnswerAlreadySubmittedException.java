package com.example.rankinggame.engine.exceptions;

public class AnswerAlreadySubmittedException extends IllegalArgumentException {
    public AnswerAlreadySubmittedException() {
        super("Player already submitted an answer for this round");
    }

    public AnswerAlreadySubmittedException(Throwable cause) {
        super("Player already submitted an answer for this round", cause);
    }
}
