package com.example.rankinggame.websocket;

import com.example.rankinggame.events.PlayerJoinedRoomEvent;
import com.example.rankinggame.events.PlayerLeftRoomEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RoomRealtimeEventPublisherTest {
    @Test
    void publishesPlayerJoinedToRoomTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RoomRealtimeEventPublisher publisher = new RoomRealtimeEventPublisher(messagingTemplate);
        UUID playerId = UUID.randomUUID();

        publisher.publishPlayerJoined(new PlayerJoinedRoomEvent("ABCD12", playerId, "Alex", false));

        ArgumentCaptor<RoomRealtimeEvent> eventCaptor = ArgumentCaptor.forClass(RoomRealtimeEvent.class);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/rooms/ABCD12"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(RoomRealtimeEventPublisher.PLAYER_JOINED);
        assertThat(eventCaptor.getValue().payload()).isEqualTo(new PlayerJoinedPayload(playerId, "Alex", false));
    }

    @Test
    void publishesPlayerLeftToRoomTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RoomRealtimeEventPublisher publisher = new RoomRealtimeEventPublisher(messagingTemplate);
        UUID playerId = UUID.randomUUID();

        publisher.publishPlayerLeft(new PlayerLeftRoomEvent("ABCD12", playerId));

        ArgumentCaptor<RoomRealtimeEvent> eventCaptor = ArgumentCaptor.forClass(RoomRealtimeEvent.class);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/rooms/ABCD12"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(RoomRealtimeEventPublisher.PLAYER_LEFT);
        assertThat(eventCaptor.getValue().payload()).isEqualTo(new PlayerLeftPayload(playerId));
    }
}
