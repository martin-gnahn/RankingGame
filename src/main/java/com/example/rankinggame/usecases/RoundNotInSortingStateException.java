package com.example.rankinggame.usecases;

public class RoundNotInSortingStateException extends RuntimeException {
    public RoundNotInSortingStateException() {
        super("Answers can only be sorted in sorting mode");
    }
}
