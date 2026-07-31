package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.CardValueNotBetween1And10Exception;
import com.fasterxml.jackson.annotation.JsonValue;

public record CardNumber(@JsonValue int value) {
    private static final int MIN_VALUE = 1;
    private static final int MAX_VALUE = 10;

    public CardNumber {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new CardValueNotBetween1And10Exception(value);
        }
    }

    public static CardNumber of(int value) {
        return new CardNumber(value);
    }
}
