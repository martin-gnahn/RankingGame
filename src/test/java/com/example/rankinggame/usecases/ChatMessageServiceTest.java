package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.ChatMessageResult;
import com.example.rankinggame.dto.SendChatMessageCommand;
import com.example.rankinggame.entities.ChatMessageEntity;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.events.ChatMessageSentEvent;
import com.example.rankinggame.repositories.ChatMessageRepository;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatMessageServiceTest {
    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ChatMessageService service = new ChatMessageService(
            roomRepository,
            playerRepository,
            chatMessageRepository,
            eventPublisher
    );

    @Test
    void sendsMessageForPlayerInRoomAndPublishesEvent() {
        UUID roomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        RoomEntity room = room(roomId, "ABCD12", RoomStatus.LOBBY);
        PlayerEntity player = player(playerId, roomId, "Alex");

        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(chatMessageRepository.save(any(ChatMessageEntity.class))).thenAnswer(invocation -> {
            ChatMessageEntity message = invocation.getArgument(0);
            message.setId(messageId);
            return message;
        });

        ChatMessageResult result = service.sendMessage(new SendChatMessageCommand(
                " abcd12 ",
                playerId,
                "  Hallo zusammen  "
        ));

        assertThat(result.messageId()).isEqualTo(messageId);
        assertThat(result.roomCode()).isEqualTo("ABCD12");
        assertThat(result.playerId()).isEqualTo(playerId);
        assertThat(result.senderNickname()).isEqualTo("Alex");
        assertThat(result.body()).isEqualTo("Hallo zusammen");
        assertThat(result.createdAt()).isNotNull();

        ArgumentCaptor<ChatMessageEntity> messageCaptor = ArgumentCaptor.forClass(ChatMessageEntity.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getRoomId()).isEqualTo(roomId);
        assertThat(messageCaptor.getValue().getPlayerId()).isEqualTo(playerId);
        assertThat(messageCaptor.getValue().getSenderNickname()).isEqualTo("Alex");
        assertThat(messageCaptor.getValue().getBody()).isEqualTo("Hallo zusammen");

        ArgumentCaptor<ChatMessageSentEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().roomCode()).isEqualTo("ABCD12");
        assertThat(eventCaptor.getValue().messageId()).isEqualTo(messageId);
        assertThat(eventCaptor.getValue().body()).isEqualTo("Hallo zusammen");
    }

    @Test
    void rejectsPlayerOutsideRoom() {
        UUID roomId = UUID.randomUUID();
        UUID otherRoomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room(roomId, "ABCD12", RoomStatus.LOBBY)));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player(playerId, otherRoomId, "Alex")));

        assertThatThrownBy(() -> service.sendMessage(new SendChatMessageCommand("ABCD12", playerId, "Hallo")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Player is not part of this room");
    }

    @Test
    void returnsRecentMessagesOldestFirst() {
        UUID roomId = UUID.randomUUID();
        ChatMessageEntity newest = message(UUID.randomUUID(), roomId, "Neu", Instant.parse("2026-06-24T10:01:00Z"));
        ChatMessageEntity oldest = message(UUID.randomUUID(), roomId, "Alt", Instant.parse("2026-06-24T10:00:00Z"));

        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room(roomId, "ABCD12", RoomStatus.LOBBY)));
        when(chatMessageRepository.findTop50ByRoomIdOrderByCreatedAtDesc(roomId)).thenReturn(List.of(newest, oldest));

        List<ChatMessageResult> results = service.getRecentMessages("ABCD12");

        assertThat(results)
                .extracting(ChatMessageResult::body)
                .containsExactly("Alt", "Neu");
    }

    private RoomEntity room(UUID roomId, String code, RoomStatus status) {
        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setCode(code);
        room.setStatus(status);
        return room;
    }

    private PlayerEntity player(UUID playerId, UUID roomId, String nickname) {
        PlayerEntity player = new PlayerEntity();
        player.setId(playerId);
        player.setRoomId(roomId);
        player.setNickname(nickname);
        return player;
    }

    private ChatMessageEntity message(UUID id, UUID roomId, String body, Instant createdAt) {
        ChatMessageEntity message = new ChatMessageEntity();
        message.setId(id);
        message.setRoomId(roomId);
        message.setPlayerId(UUID.randomUUID());
        message.setSenderNickname("Alex");
        message.setBody(body);
        message.setCreatedAt(createdAt);
        return message;
    }
}
