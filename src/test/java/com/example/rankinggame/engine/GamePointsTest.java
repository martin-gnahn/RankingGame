package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.CannotDecreaseInactivePointsException;
import com.example.rankinggame.engine.exceptions.NegativePointDecreaseNotAllowedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GamePointsTest {

    @Test
    void decreasesActivePoints() {
        GamePoints gamePoints = GamePoints.starting(5);

        GamePoints decreasedPoints = gamePoints.decreaseBy(2);

        assertThat(decreasedPoints.value()).contains(3);
    }

    @Test
    void doesNotDecreaseBelowZero() {
        GamePoints gamePoints = GamePoints.starting(1);

        GamePoints decreasedPoints = gamePoints.decreaseBy(2);

        assertThat(decreasedPoints.value()).contains(0);
    }

    @Test
    void rejectsDecreaseWhenPointsAreInactive() {
        GamePoints gamePoints = GamePoints.inactive();

        assertThatThrownBy(() -> gamePoints.decreaseBy(1))
                .isInstanceOf(CannotDecreaseInactivePointsException.class)
                .hasMessage("Cannot decrease inactive game points");
    }

    @Test
    void rejectsNegativeDecreaseAmount() {
        GamePoints gamePoints = GamePoints.starting(5);

        assertThatThrownBy(() -> gamePoints.decreaseBy(-1))
                .isInstanceOf(NegativePointDecreaseNotAllowedException.class)
                .hasMessage("Point decrease amount must not be negative (-1)");
    }
}
