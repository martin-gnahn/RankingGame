package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.GameParticipant;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.entities.PlayerEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerMapper {
    public GameParticipant toParticipant(PlayerEntity playerEntity) {
        return new GameParticipant(
                new PlayerId(playerEntity.getId()),
                playerEntity.getNickname(),
                playerEntity.isHost()
        );
    }

    public List<GameParticipant> toParticipants(List<PlayerEntity> playerEntities) {
        return playerEntities.stream().map(this::toParticipant).toList();
    }
}
