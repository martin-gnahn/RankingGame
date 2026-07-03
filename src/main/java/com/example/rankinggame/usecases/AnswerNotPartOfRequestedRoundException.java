package com.example.rankinggame.usecases;

public class AnswerNotPartOfRequestedRoundException extends RuntimeException {
    public AnswerNotPartOfRequestedRoundException() {
        super("Answer is not part of the requested round");
    }
}
