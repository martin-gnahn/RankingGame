package com.example.rankinggame.engine.exceptions;

public class AnswerTextTooLongException extends IllegalArgumentException {
    public AnswerTextTooLongException(int maxLength) {
        super("Answer text must be " + maxLength + " characters or fewer");
    }
}
