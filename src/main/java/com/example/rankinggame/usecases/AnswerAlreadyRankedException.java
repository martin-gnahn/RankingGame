package com.example.rankinggame.usecases;

public class AnswerAlreadyRankedException extends RuntimeException {
    public AnswerAlreadyRankedException() {
        super("Answer already has been ranked");
    }
}
