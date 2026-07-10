package com.example.rankinggame.engine.exceptions;

public enum CardValueInfoField {
    ROUND_ID("round id"),
    PLAYER_ID("player id"),
    CARD_NUMBER("card number");

    private final String label;

    CardValueInfoField(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }
}
