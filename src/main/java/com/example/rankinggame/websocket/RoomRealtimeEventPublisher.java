package com.example.rankinggame.websocket;

import com.example.rankinggame.events.PlayerJoinedRoomEvent;
import com.example.rankinggame.events.PlayerLeftRoomEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class RoomRealtimeEventPublisher {
    public static final String PLAYER_JOINED = "PLAYER_JOINED";
    public static final String PLAYER_LEFT = "PLAYER_LEFT";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishPlayerJoined(PlayerJoinedRoomEvent event) {
        publish(event.roomCode(), new RoomRealtimeEvent(
                PLAYER_JOINED,
                new PlayerJoinedPayload(event.playerId(), event.nickname(), event.host())
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishPlayerLeft(PlayerLeftRoomEvent event) {
        publish(event.roomCode(), new RoomRealtimeEvent(
                PLAYER_LEFT,
                new PlayerLeftPayload(event.playerId())
        ));
    }

    private void publish(String roomCode, RoomRealtimeEvent event) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode, event);
    }
}
