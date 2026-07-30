package com.example.rankinggame.engine.ranking;

import com.example.rankinggame.engine.CardNumber;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.exceptions.IncompleteCardValueInfoException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardValueInfoTest {
    @Test
    void keepsPlayerAndCardValue() {
        PlayerId playerId = new PlayerId(UUID.randomUUID());
        CardNumber cardNumber = CardNumber.of(5);

        CardValueInfo cardValueInfo = new CardValueInfo(playerId, cardNumber);

        assertThat(cardValueInfo.playerId()).isEqualTo(playerId);
        assertThat(cardValueInfo.cardValue()).isEqualTo(cardNumber);
    }

    @Test
    void rejectsMissingPlayerId() {
        assertThatThrownBy(() -> new CardValueInfo(null, CardNumber.of(5)))
                .isInstanceOf(IncompleteCardValueInfoException.class)
                .hasMessage("Card value info requires player id");
    }

    @Test
    void rejectsMissingCardNumber() {
        PlayerId playerId = new PlayerId(UUID.randomUUID());

        assertThatThrownBy(() -> new CardValueInfo(playerId, null))
                .isInstanceOf(IncompleteCardValueInfoException.class)
                .hasMessage("Card value info requires card number");
    }
}
