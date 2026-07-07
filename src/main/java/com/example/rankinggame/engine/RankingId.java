package com.example.rankinggame.engine;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.UUID;

public record RankingId(@JsonValue UUID value) {
}
