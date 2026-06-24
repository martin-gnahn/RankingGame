package com.example.rankinggame.engine.exceptions;

public class AnswersNotAcceptedException extends IllegalArgumentException {
    public AnswersNotAcceptedException() {
        super("Answers are not accepted for this round");
    }
}
