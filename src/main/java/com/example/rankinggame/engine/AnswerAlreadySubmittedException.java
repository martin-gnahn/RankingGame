package com.example.rankinggame.engine;

public class AnswerAlreadySubmittedException extends IllegalArgumentException {
    public AnswerAlreadySubmittedException() {
        super("Player already submitted an answer for this round");
    }
}
