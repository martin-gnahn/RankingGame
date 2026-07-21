package com.example.rankinggame.websocket;

import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.events.PlayerLeftRoomEvent;
import com.example.rankinggame.events.PlayerRejoinedRoomEvent;
import com.example.rankinggame.repositories.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PlayerPresenceServiceTest {
    private final LivePlayerSessionRegistry registry = new LivePlayerSessionRegistry();
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
    @SuppressWarnings("rawtypes")
    private final ScheduledFuture scheduledFuture = mock(ScheduledFuture.class);
    private final TransactionOperations transactionOperations = new ImmediateTransactionOperations();
    private final PlayerPresenceService service = new PlayerPresenceService(
            registry,
            playerRepository,
            eventPublisher,
            transactionOperations,
            executor
    );

    @BeforeEach
    void setUp() {
        doReturn(scheduledFuture).when(executor).schedule(any(Runnable.class), eq(3L), eq(TimeUnit.SECONDS));
    }

    @Test
    void marksDisconnectedPlayerConnectedWhenLiveSessionIsRegistered() {
        UUID playerId = UUID.randomUUID();
        PlayerEntity player = player(playerId, PlayerConnectionStatus.DISCONNECTED);
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(playerRepository.save(player)).thenReturn(player);

        service.markConnected("session-1", "abcd12", playerId);

        assertThat(player.getConnectionStatus()).isEqualTo(PlayerConnectionStatus.CONNECTED);
        verify(playerRepository).save(player);
        verify(eventPublisher).publishEvent(new PlayerRejoinedRoomEvent("ABCD12", playerId));
    }

    @Test
    void schedulesDisconnectAfterLastSessionIsRemoved() {
        UUID playerId = UUID.randomUUID();
        registry.register("session-1", "ABCD12", playerId);

        service.markSessionDisconnected("session-1");

        verify(executor).schedule(any(Runnable.class), eq(3L), eq(TimeUnit.SECONDS));
        verify(playerRepository, never()).findById(playerId);
    }

    @Test
    void delayedDisconnectMarksPlayerDisconnectedWhenNoSessionReturned() {
        UUID playerId = UUID.randomUUID();
        PlayerEntity player = player(playerId, PlayerConnectionStatus.CONNECTED);
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(playerRepository.save(player)).thenReturn(player);
        registry.register("session-1", "ABCD12", playerId);

        service.markSessionDisconnected("session-1");
        scheduledDisconnect().run();

        assertThat(player.getConnectionStatus()).isEqualTo(PlayerConnectionStatus.DISCONNECTED);
        verify(playerRepository).save(player);
        verify(eventPublisher).publishEvent(new PlayerLeftRoomEvent("ABCD12", playerId));
    }

    @Test
    void disconnectDoesNotMarkPlayerOfflineWhenAnotherSessionStillExists() {
        UUID playerId = UUID.randomUUID();
        registry.register("session-1", "ABCD12", playerId);
        registry.register("session-2", "ABCD12", playerId);

        service.markSessionDisconnected("session-1");

        verify(executor, never()).schedule(any(Runnable.class), eq(3L), eq(TimeUnit.SECONDS));
        verify(playerRepository, never()).findById(playerId);
    }

    @Test
    void reconnectCancelsPendingDisconnect() {
        UUID playerId = UUID.randomUUID();
        PlayerEntity player = player(playerId, PlayerConnectionStatus.DISCONNECTED);
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(playerRepository.save(player)).thenReturn(player);
        registry.register("session-1", "ABCD12", playerId);

        service.markSessionDisconnected("session-1");
        service.markConnected("session-2", "ABCD12", playerId);
        scheduledDisconnect().run();

        verify(scheduledFuture).cancel(false);
        assertThat(player.getConnectionStatus()).isEqualTo(PlayerConnectionStatus.CONNECTED);
    }

    private Runnable scheduledDisconnect() {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).schedule(runnableCaptor.capture(), eq(3L), eq(TimeUnit.SECONDS));
        return runnableCaptor.getValue();
    }

    private PlayerEntity player(UUID playerId, PlayerConnectionStatus connectionStatus) {
        PlayerEntity player = new PlayerEntity();
        player.setId(playerId);
        player.setConnectionStatus(connectionStatus);
        return player;
    }

    private static class ImmediateTransactionOperations implements TransactionOperations {
        @Override
        public <T> T execute(TransactionCallback<T> action) {
            return action.doInTransaction(mock(TransactionStatus.class));
        }
    }
}
