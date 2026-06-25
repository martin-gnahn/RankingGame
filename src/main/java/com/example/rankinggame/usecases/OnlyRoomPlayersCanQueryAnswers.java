package com.example.rankinggame.usecases;

public class OnlyRoomPlayersCanQueryAnswers extends RuntimeException {
    public OnlyRoomPlayersCanQueryAnswers() {
        super("Only players in this room can query submitted answers");
    }
}
