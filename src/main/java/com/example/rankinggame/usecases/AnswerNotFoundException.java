package com.example.rankinggame.usecases;

public class AnswerNotFoundException extends RuntimeException {
    public AnswerNotFoundException() {
        super("Answer is not found");
    }
}
