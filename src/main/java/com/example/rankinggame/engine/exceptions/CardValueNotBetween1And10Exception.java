package com.example.rankinggame.engine.exceptions;

public class CardValueNotBetween1And10Exception extends IllegalArgumentException {
    public CardValueNotBetween1And10Exception(int cardValue) {
        super("Card value '%d' must be between 1 and 10".formatted(cardValue));
    }
}
