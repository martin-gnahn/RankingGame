package com.example.rankinggame.controllers;

import com.example.rankinggame.auth.TokenGenerator;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.exceptions.UserTokenNotPresentException;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class PlayerSessionService {
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final TokenGenerator tokenGenerator;

    public AuthenticatedPlayer authenticatePlayer(String roomCode, String token) {
        if (StringUtils.isBlank(token)) {
            throw new UserTokenNotPresentException();
        }
        Optional<RoomEntity> roomByCode = roomRepository.findByCode(roomCode);
        if (roomByCode.isEmpty()) {
            throw new UserNotAuthorizedException();
        }
        String tokenHash = tokenGenerator.generateHashFromToken(token);
        Optional<PlayerEntity> byRoomIdAndTokenHash = playerRepository.findByRoomIdAndTokenHash(roomByCode.get().getId(), tokenHash);
        if (byRoomIdAndTokenHash.isEmpty()) {
            throw new UserNotAuthorizedException();
        }
        PlayerEntity foundPlayer = byRoomIdAndTokenHash.get();
        boolean sessionHasExpired = foundPlayer.getSessionExpiresAt() == null || foundPlayer.getSessionExpiresAt().isBefore(Instant.now());
        if (sessionHasExpired) {
            throw new UserSessionExpiredException();
        }
        return new AuthenticatedPlayer(foundPlayer.getId());
    }
}
