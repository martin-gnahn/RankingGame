package com.example.rankinggame.auth;

import com.example.rankinggame.auth.port.PlayerSessionPort;
import com.example.rankinggame.controllers.UserNotAuthorizedException;
import com.example.rankinggame.controllers.UserSessionExpiredException;
import com.example.rankinggame.exceptions.UserTokenNotPresentException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class PlayerSessionService {
    private final PlayerSessionPort playerSessionPort;
    private final TokenGenerator tokenGenerator;

    public AuthenticatedPlayer authenticatePlayer(String roomCode, String token) {
        if (StringUtils.isBlank(token)) {
            throw new UserTokenNotPresentException();
        }
        String tokenHash = tokenGenerator.generateHashFromToken(token);
        Optional<PlayerSession> byRoomIdAndTokenHash = playerSessionPort.findByRoomCodeAndTokenHash(roomCode, tokenHash);
        if (byRoomIdAndTokenHash.isEmpty()) {
            throw new UserNotAuthorizedException();
        }
        PlayerSession foundSession = byRoomIdAndTokenHash.get();
        boolean sessionHasExpired = foundSession.sessionExpiresAt() == null || foundSession.sessionExpiresAt().isBefore(Instant.now());
        if (sessionHasExpired) {
            throw new UserSessionExpiredException();
        }
        return new AuthenticatedPlayer(foundSession.playerId());
    }
}
