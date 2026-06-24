package com.example.rankinggame.engine;

public class AnswersNotAcceptedException extends IllegalArgumentException {
    public AnswersNotAcceptedException() {
        super("Answers are not accepted for this round");
    }
}
