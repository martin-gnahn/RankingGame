package com.example.rankinggame.usecases;

import com.example.rankinggame.engine.GameParticipant;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.mapper.PlayerMapper;
import com.example.rankinggame.repositories.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
class GameParticipantContextLoader {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

    public List<GameParticipant> getAllPlayers(GameSession gameSession) {
        List<PlayerEntity> byGameSessionId = playerRepository.findByGameSessionId(gameSession.getId());
        return playerMapper.toParticipants(byGameSessionId);
    }
}
