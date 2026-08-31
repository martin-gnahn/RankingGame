package com.example.rankinggame.auth.adapter;

import com.example.rankinggame.auth.PlayerSession;
import com.example.rankinggame.auth.port.PlayerSessionPort;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class JpaPlayerSessionAdapter implements PlayerSessionPort {
    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository;

    @Override
    public Optional<PlayerSession> findByRoomCodeAndTokenHash(String roomCode, String tokenHash) {
        var room = roomRepository.findByCode(roomCode);
        if (room.isEmpty()) {
            return Optional.empty();
        }
        Optional<PlayerEntity> byRoomIdAndTokenHash = playerRepository.findByRoomIdAndTokenHash(room.get().getId(), tokenHash);
        if (byRoomIdAndTokenHash.isEmpty()) {
            return Optional.empty();
        }
        var playerEntity = byRoomIdAndTokenHash.get();
        return Optional.of(new PlayerSession(playerEntity.getSessionExpiresAt(), playerEntity.getId()));
    }
}