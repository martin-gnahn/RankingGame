package com.example.rankinggame.engine.ranking;

import com.example.rankinggame.engine.CardNumber;
import com.example.rankinggame.engine.PlayerId;
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
