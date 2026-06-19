package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.Player;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.entities.PlayerEntity;
import org.springframework.stereotype.Service;

@Service
public class PlayerMapper {
    public Player toDomain(PlayerEntity playerEntity) {
        return Player.builder()
                .playerId(new PlayerId(playerEntity.getId()))
                .playerName(playerEntity.getNickname())
                .build();
    }

    public PlayerEntity toEntity(Player player) {
        PlayerEntity playerEntity = new PlayerEntity();
        playerEntity.setId(player.playerId() == null ? null : player.playerId().value());
        playerEntity.setNickname(player.playerName());
        return playerEntity;
    }
}
