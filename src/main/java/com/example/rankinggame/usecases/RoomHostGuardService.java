package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.repositories.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
class RoomHostGuardService {
    private final PlayerRepository playerRepository;

    public PlayerEntity requireRoomHost(RoomEntity room, UUID hostPlayerId) {
        return playerRepository.findById(hostPlayerId)
                .filter(player -> Objects.equals(player.getRoomId(), room.getId()))
                .filter(player -> Objects.equals(room.getHostPlayerId(), player.getId()))
                .orElseThrow(OnlyHostCanStartGame::new);
    }
}
