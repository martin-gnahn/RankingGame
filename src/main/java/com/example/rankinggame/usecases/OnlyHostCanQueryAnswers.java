package com.example.rankinggame.usecases;

public class OnlyHostCanQueryAnswers extends RuntimeException {
    public OnlyHostCanQueryAnswers() {
        super("Only the host can query submitted answers");
    }
}
