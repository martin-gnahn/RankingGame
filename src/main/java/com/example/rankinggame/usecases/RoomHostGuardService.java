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

    public PlayerEntity requireRoomHostForStart(RoomEntity room, UUID hostPlayerId) {
        PlayerEntity hostPlayer = playerRepository.findById(hostPlayerId)
                .filter(player -> Objects.equals(player.getRoomId(), room.getId()))
                .filter(PlayerEntity::isHost)
                .orElseThrow(OnlyHostCanStartGame::new);

        if (!Objects.equals(room.getHostPlayerId(), hostPlayer.getId())) {
            throw new OnlyHostCanStartGame();
        }

        return hostPlayer;
    }

    public PlayerEntity requireRoomHostForVote(RoomEntity room, UUID hostPlayerId) {
        PlayerEntity hostPlayer = playerRepository.findById(hostPlayerId)
                .filter(player -> Objects.equals(player.getRoomId(), room.getId()))
                .filter(PlayerEntity::isHost)
                .orElseThrow(OnlyHostCanStartGame::new);

        if (!Objects.equals(room.getHostPlayerId(), hostPlayer.getId())) {
            throw new OnlyHostCanSortAnswers();
        }

        return hostPlayer;
    }
}
