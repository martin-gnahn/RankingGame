package com.example.rankinggame.usecases;

public class OnlyHostCanSortAnswers extends RuntimeException {
    public OnlyHostCanSortAnswers() {
        super("Only the host can sort submitted answers");
    }
}
