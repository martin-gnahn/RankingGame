package com.example.rankinggame.engine;

import java.util.Optional;

class GamePoints {
    boolean isActive;
    int points;

    Optional<Integer> get() {
        return isActive ? Optional.of(points) : Optional.empty();
    }

    void setStartingPoints(int startingPoints) {
        points = startingPoints;
    }
}
