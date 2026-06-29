package com.example.rankinggame.websocket;

import com.example.rankinggame.events.AnswerSubmittedEvent;
import com.example.rankinggame.events.ChatMessageSentEvent;
import com.example.rankinggame.entities.GameType;
import com.example.rankinggame.events.GameStartedRoomEvent;
import com.example.rankinggame.events.PlayerJoinedRoomEvent;
import com.example.rankinggame.events.PlayerLeftRoomEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
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

    @Test
    void publishesGameStartedToRoomTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RoomRealtimeEventPublisher publisher = new RoomRealtimeEventPublisher(messagingTemplate);
        UUID gameSessionId = UUID.randomUUID();

        publisher.publishGameStarted(new GameStartedRoomEvent("ABCD12", gameSessionId, GameType.RANKING_GAME));

        ArgumentCaptor<RoomRealtimeEvent> eventCaptor = ArgumentCaptor.forClass(RoomRealtimeEvent.class);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/rooms/ABCD12"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(RoomRealtimeEventPublisher.GAME_STARTED);
        assertThat(eventCaptor.getValue().payload()).isEqualTo(new GameStartedPayload(gameSessionId, GameType.RANKING_GAME));
    }

    @Test
    void publishesChatMessageSentToRoomTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RoomRealtimeEventPublisher publisher = new RoomRealtimeEventPublisher(messagingTemplate);
        UUID messageId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-24T10:15:30Z");

        publisher.publishChatMessageSent(new ChatMessageSentEvent(
                "ABCD12",
                messageId,
                playerId,
                "Alex",
                "Hallo",
                createdAt
        ));

        ArgumentCaptor<RoomRealtimeEvent> eventCaptor = ArgumentCaptor.forClass(RoomRealtimeEvent.class);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/rooms/ABCD12"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(RoomRealtimeEventPublisher.CHAT_MESSAGE_SENT);
        assertThat(eventCaptor.getValue().payload()).isEqualTo(new ChatMessagePayload(
                messageId,
                playerId,
                "Alex",
                "Hallo",
                createdAt
        ));
    }

    @Test
    void publishesAnswerSubmittedToRoomTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RoomRealtimeEventPublisher publisher = new RoomRealtimeEventPublisher(messagingTemplate);
        UUID roundId = UUID.randomUUID();

        publisher.publishAnswerSubmitted(new AnswerSubmittedEvent(
                "ABCD12",
                roundId,
                2,
                3,
                false
        ));

        ArgumentCaptor<RoomRealtimeEvent> eventCaptor = ArgumentCaptor.forClass(RoomRealtimeEvent.class);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/rooms/ABCD12"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(RoomRealtimeEventPublisher.ANSWER_SUBMITTED);
        assertThat(eventCaptor.getValue().payload()).isEqualTo(new AnswerSubmittedPayload(
                roundId,
                2,
                3,
                false
        ));
    }
}
