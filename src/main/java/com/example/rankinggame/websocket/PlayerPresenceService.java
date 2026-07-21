package com.example.rankinggame.websocket;

import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.events.PlayerLeftRoomEvent;
import com.example.rankinggame.events.PlayerRejoinedRoomEvent;
import com.example.rankinggame.repositories.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.util.UUID;
import java.util.concurrent.*;

@RequiredArgsConstructor
@Component
public class PlayerPresenceService {
    private static final long DISCONNECT_GRACE_PERIOD_SECONDS = 3L;

    private final LivePlayerSessionRegistry sessionRegistry;
    private final PlayerRepository playerRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionOperations transactionOperations;
    private final ScheduledExecutorService playerPresenceDisconnectExecutor;
    private final ConcurrentMap<PlayerPresenceKey, PendingDisconnect> pendingDisconnects = new ConcurrentHashMap<>();

    public void markConnected(String sessionId, String roomCode, UUID playerId) {
        sessionRegistry.register(sessionId, roomCode, playerId)
                .ifPresent(session -> {
                    PlayerPresenceKey key = PlayerPresenceKey.from(session);
                    cancelPendingDisconnect(key);
                    transactionOperations.execute(status -> {
                        playerRepository.findById(session.playerId())
                                .ifPresent(player -> markConnected(session, player));
                        return null;
                    });
                });
    }

    public void markSessionDisconnected(String sessionId) {
        sessionRegistry.remove(sessionId)
                .ifPresent(session -> {
                    if (sessionRegistry.hasSession(session.roomCode(), session.playerId())) {
                        return;
                    }

                    scheduleDisconnect(PlayerPresenceKey.from(session));
                });
    }

    private void markConnected(LivePlayerSession session, PlayerEntity player) {
        if (player.getConnectionStatus() == PlayerConnectionStatus.CONNECTED) {
            return;
        }

        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);
        PlayerEntity savedPlayer = playerRepository.save(player);
        eventPublisher.publishEvent(new PlayerRejoinedRoomEvent(session.roomCode(), savedPlayer.getId()));
    }

    private void scheduleDisconnect(PlayerPresenceKey key) {
        UUID disconnectId = UUID.randomUUID();
        ScheduledFuture<?> scheduledFuture = playerPresenceDisconnectExecutor.schedule(
                () -> disconnectIfNoSessionRemains(key, disconnectId),
                DISCONNECT_GRACE_PERIOD_SECONDS,
                TimeUnit.SECONDS
        );

        PendingDisconnect previous = pendingDisconnects.put(key, new PendingDisconnect(disconnectId, scheduledFuture));
        if (previous != null) {
            previous.scheduledFuture().cancel(false);
        }
    }

    private void cancelPendingDisconnect(PlayerPresenceKey key) {
        PendingDisconnect pendingDisconnect = pendingDisconnects.remove(key);
        if (pendingDisconnect != null) {
            pendingDisconnect.scheduledFuture().cancel(false);
        }
    }

    private void disconnectIfNoSessionRemains(PlayerPresenceKey key, UUID disconnectId) {
        PendingDisconnect pendingDisconnect = pendingDisconnects.get(key);
        if (pendingDisconnect == null || !pendingDisconnect.disconnectId().equals(disconnectId)) {
            return;
        }

        if (sessionRegistry.hasSession(key.roomCode(), key.playerId())) {
            pendingDisconnects.remove(key, pendingDisconnect);
            return;
        }

        transactionOperations.execute(status -> {
            if (!sessionRegistry.hasSession(key.roomCode(), key.playerId())) {
                playerRepository.findById(key.playerId())
                        .ifPresent(player -> markDisconnected(key, player));
            }
            return null;
        });
        pendingDisconnects.remove(key, pendingDisconnect);
    }

    private void markDisconnected(PlayerPresenceKey key, PlayerEntity player) {
        if (player.getConnectionStatus() == PlayerConnectionStatus.DISCONNECTED) {
            return;
        }

        player.setConnectionStatus(PlayerConnectionStatus.DISCONNECTED);
        PlayerEntity savedPlayer = playerRepository.save(player);
        eventPublisher.publishEvent(new PlayerLeftRoomEvent(key.roomCode(), savedPlayer.getId()));
    }

    private record PlayerPresenceKey(String roomCode, UUID playerId) {
        static PlayerPresenceKey from(LivePlayerSession session) {
            return new PlayerPresenceKey(session.roomCode(), session.playerId());
        }
    }

    private record PendingDisconnect(UUID disconnectId, ScheduledFuture<?> scheduledFuture) {
    }
}
