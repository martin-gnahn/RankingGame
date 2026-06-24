package com.example.rankinggame.usecases;

public class OnlyHostCanStartGame extends RuntimeException {
    public OnlyHostCanStartGame() {
        super("Only the host can start the game");
    }
}
