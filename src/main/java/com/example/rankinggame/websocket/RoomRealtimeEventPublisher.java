package com.example.rankinggame.websocket;

import com.example.rankinggame.events.*;
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
    public static final String GAME_STARTED = "GAME_STARTED";
    public static final String ANSWER_SUBMITTED = "ANSWER_SUBMITTED";
    public static final String SORTING_STARTED = "SORTING_STARTED";
    public static final String ANSWER_RANKED = "ANSWER_RANKED";
    public static final String CHAT_MESSAGE_SENT = "CHAT_MESSAGE_SENT";

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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishGameStarted(GameStartedRoomEvent event) {
        publish(event.roomCode(), new RoomRealtimeEvent(
                GAME_STARTED,
                new GameStartedPayload(event.gameSessionId(), event.gameType())
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAnswerSubmitted(AnswerSubmittedEvent event) {
        publish(event.roomCode(), new RoomRealtimeEvent(
                ANSWER_SUBMITTED,
                new AnswerSubmittedPayload(
                        event.roundId(),
                        event.submittedAnswerCount(),
                        event.requiredAnswerCount(),
                        event.allAnswersSubmitted()
                )
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishSortingStarted(SortingStartedEvent event) {
        publish(event.roomCode(), new RoomRealtimeEvent(
                SORTING_STARTED,
                new SortingStartedPayload(
                        event.roundId()
                )
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAnswerRanked(AnswerRankedEvent event) {
        publish(event.roomCode(), new RoomRealtimeEvent(
                ANSWER_RANKED,
                new AnswerRankedPayload(
                        event.roundId(),
                        event.answerId().value(),
                        event.oneBasedPosition()
                )
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishChatMessageSent(ChatMessageSentEvent event) {
        publish(event.roomCode(), new RoomRealtimeEvent(
                CHAT_MESSAGE_SENT,
                new ChatMessagePayload(
                        event.messageId(),
                        event.playerId(),
                        event.senderNickname(),
                        event.body(),
                        event.createdAt()
                )
        ));
    }

    private void publish(String roomCode, RoomRealtimeEvent event) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode, event);
    }
}
