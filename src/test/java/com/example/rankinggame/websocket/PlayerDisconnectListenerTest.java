package com.example.rankinggame.websocket;

import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.events.PlayerLeftRoomEvent;
import com.example.rankinggame.repositories.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerDisconnectListenerTest {
    @Test
    void marksRegisteredPlayerDisconnectedAndPublishesEvent() {
        LivePlayerSessionRegistry registry = new LivePlayerSessionRegistry();
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        PlayerDisconnectListener listener = new PlayerDisconnectListener(registry, playerRepository, eventPublisher);
        UUID playerId = UUID.randomUUID();
        PlayerEntity player = new PlayerEntity();
        player.setId(playerId);
        player.setConnectionStatus(PlayerConnectionStatus.CONNECTED);

        registry.register("session-1", "ABCD12", playerId);
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(playerRepository.save(player)).thenReturn(player);

        listener.handleDisconnect(disconnectEvent("session-1"));

        assertThat(player.getConnectionStatus()).isEqualTo(PlayerConnectionStatus.DISCONNECTED);
        verify(playerRepository).save(player);

        ArgumentCaptor<PlayerLeftRoomEvent> eventCaptor = ArgumentCaptor.forClass(PlayerLeftRoomEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new PlayerLeftRoomEvent("ABCD12", playerId));
    }

    @Test
    void ignoresUnknownSession() {
        LivePlayerSessionRegistry registry = new LivePlayerSessionRegistry();
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        PlayerDisconnectListener listener = new PlayerDisconnectListener(registry, playerRepository, eventPublisher);

        listener.handleDisconnect(disconnectEvent("missing-session"));

        verify(playerRepository, never()).findById(org.mockito.ArgumentMatchers.any());
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    private SessionDisconnectEvent disconnectEvent(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
        accessor.setSessionId(sessionId);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL);
    }
}
