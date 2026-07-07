package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.CannotDecreaseInactivePointsException;
import com.example.rankinggame.engine.exceptions.NegativePointDecreaseNotAllowedException;
import com.example.rankinggame.exceptions.NegativeStartingPointsNotAllowedException;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
class GamePoints {
    private final boolean active;
    private final int points;

    static GamePoints inactive() {
        return new GamePoints(false, 0);
    }

    static GamePoints starting(int startingPoints) {
        if (startingPoints < 0) {
            throw new NegativeStartingPointsNotAllowedException(startingPoints);
        }
        return new GamePoints(true, startingPoints);
    }

    GamePoints decreaseBy(int amount) {
        if (!active) {
            throw new CannotDecreaseInactivePointsException();
        }
        if (amount < 0) {
            throw new NegativePointDecreaseNotAllowedException(amount);
        }
        return new GamePoints(true, Math.max(0, points - amount));
    }

    Optional<Integer> value() {
        return active ? Optional.of(points) : Optional.empty();
    }
}
