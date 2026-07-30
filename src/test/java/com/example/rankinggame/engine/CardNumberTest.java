package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.CardValueNotBetween1And10Exception;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

class CardNumberTest {
    List<Integer> intListFrom1To10 = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    @Test
    void shouldAllowNumbersBetween1And10() {
        assertThatCode(() -> {
            List<CardNumber> cardNumbersFrom1To10 = IntStream.rangeClosed(1, 10)
                    .mapToObj(CardNumber::of)
                    .toList();
            assertThat(cardNumbersFrom1To10)
                    .extracting(CardNumber::value)
                    .containsExactlyElementsOf(intListFrom1To10);
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldForbidCardWithZero() {
        assertThatExceptionOfType(CardValueNotBetween1And10Exception.class)
                .isThrownBy(() -> CardNumber.of(0));
    }

    @Test
    void shouldForbidCardWithNegativeNumber() {
        assertThatExceptionOfType(CardValueNotBetween1And10Exception.class)
                .isThrownBy(() -> CardNumber.of(-1));
    }

    @Test
    void shouldForbidCardWithValueOver10() {
        assertThatExceptionOfType(CardValueNotBetween1And10Exception.class)
                .isThrownBy(() -> CardNumber.of(11));
    }
}