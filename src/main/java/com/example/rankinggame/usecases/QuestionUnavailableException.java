package com.example.rankinggame.usecases;

public class QuestionUnavailableException extends RuntimeException {
    public QuestionUnavailableException() {
        super("No active questions are available");
    }
}
