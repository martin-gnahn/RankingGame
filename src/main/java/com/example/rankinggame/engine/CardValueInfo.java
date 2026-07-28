package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.CardValueInfoField;
import com.example.rankinggame.engine.exceptions.IncompleteCardValueInfoException;

public record CardValueInfo(
        PlayerId playerId,
        CardNumber cardValue
) {
    public CardValueInfo {
        if (playerId == null) {
            throw new IncompleteCardValueInfoException(CardValueInfoField.PLAYER_ID);
        }
        if (cardValue == null) {
            throw new IncompleteCardValueInfoException(CardValueInfoField.CARD_NUMBER);
        }
    }
}
