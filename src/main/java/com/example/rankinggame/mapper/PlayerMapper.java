package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.Game;
import com.example.rankinggame.engine.GameId;
import com.example.rankinggame.engine.Player;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.PlayerEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerMapper {
    public Player toDomain(PlayerEntity playerEntity) {
        return Player.builder()
                .playerId(new PlayerId(playerEntity.getId()))
                .playerName(playerEntity.getNickname())
                .build();
    }
}
