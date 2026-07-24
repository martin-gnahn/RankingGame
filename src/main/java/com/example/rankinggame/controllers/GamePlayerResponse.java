package com.example.rankinggame.controllers;

import com.example.rankinggame.entities.GameSessionPlayerEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class GamePlayerResponse {
    private UUID gameSessionId;
    private UUID playerId;

    static List<GamePlayerResponse> from(List<GameSessionPlayerEntity> entityList) {
        return entityList.stream().map(GamePlayerResponse::buildResponse).toList();
    }

    private static GamePlayerResponse buildResponse(GameSessionPlayerEntity entity) {
        return GamePlayerResponse.builder().gameSessionId(entity.getGameSessionId()).playerId(entity.getPlayerId()).build();
    }
}
