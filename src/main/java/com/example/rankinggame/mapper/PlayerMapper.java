package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.GameParticipant;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.entities.PlayerEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerMapper {
    public GameParticipant toDomain(PlayerEntity playerEntity) {
        return new GameParticipant(
                new PlayerId(playerEntity.getId()),
                playerEntity.getNickname(),
                playerEntity.isHost()
        );
    }

    public List<GameParticipant> toDomain(List<PlayerEntity> playerEntities) {
        return playerEntities.stream().map(this::toDomain).toList();
    }

    public PlayerEntity toEntity(GameParticipant participant) {
        PlayerEntity playerEntity = new PlayerEntity();
        playerEntity.setId(participant.playerId() == null ? null : participant.playerId().value());
        playerEntity.setNickname(participant.name());
        playerEntity.setHost(participant.host());
        return playerEntity;
    }
}
