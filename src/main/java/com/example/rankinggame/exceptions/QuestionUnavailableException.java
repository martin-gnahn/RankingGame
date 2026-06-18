package com.example.rankinggame.exceptions;

public class QuestionUnavailableException extends RuntimeException {
    public QuestionUnavailableException() {
        super("No active questions are available");
    }
}
