package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.ChatMessageResult;
import com.example.rankinggame.dto.SendChatMessageCommand;
import com.example.rankinggame.entities.ChatMessageEntity;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.events.ChatMessageSentEvent;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.repositories.ChatMessageRepository;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ChatMessageService {
    private static final int MAX_MESSAGE_BODY_LENGTH = 500;

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChatMessageResult sendMessage(SendChatMessageCommand command) {
        String roomCode = normalizeRoomCode(command == null ? null : command.roomCode());
        UUID playerId = requirePlayerId(command);
        String body = normalizeBody(command == null ? null : command.body());

        RoomEntity room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));
        if (room.getStatus() == RoomStatus.CLOSED) {
            throw new IllegalArgumentException("Room is closed");
        }

        PlayerEntity player = playerRepository.findById(playerId)
                .filter(candidate -> candidate.getRoomId().equals(room.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Player is not part of this room"));

        ChatMessageEntity message = new ChatMessageEntity();
        message.setRoomId(room.getId());
        message.setPlayerId(player.getId());
        message.setSenderNickname(player.getNickname());
        message.setBody(body);
        message.setCreatedAt(Instant.now());

        ChatMessageEntity savedMessage = chatMessageRepository.save(message);
        ChatMessageResult result = toResult(roomCode, savedMessage);
        eventPublisher.publishEvent(new ChatMessageSentEvent(
                result.roomCode(),
                result.messageId(),
                result.playerId(),
                result.senderNickname(),
                result.body(),
                result.createdAt()
        ));
        return result;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResult> getRecentMessages(String roomCode) {
        String normalizedRoomCode = normalizeRoomCode(roomCode);
        RoomEntity room = roomRepository.findByCode(normalizedRoomCode)
                .orElseThrow(() -> new RoomNotFoundException(normalizedRoomCode));

        List<ChatMessageEntity> messages = new ArrayList<>(
                chatMessageRepository.findTop50ByRoomIdOrderByCreatedAtDesc(room.getId())
        );
        Collections.reverse(messages);
        return messages.stream()
                .map(message -> toResult(normalizedRoomCode, message))
                .toList();
    }

    private String normalizeRoomCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new RoomNotFoundException("");
        }

        return roomCode.trim().toUpperCase(Locale.ROOT);
    }

    private UUID requirePlayerId(SendChatMessageCommand command) {
        if (command == null || command.playerId() == null) {
            throw new IllegalArgumentException("Player id is required");
        }

        return command.playerId();
    }

    private String normalizeBody(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Message body is required");
        }

        String trimmedBody = body.trim();
        if (trimmedBody.length() > MAX_MESSAGE_BODY_LENGTH) {
            throw new IllegalArgumentException("Message body must be 500 characters or fewer");
        }

        return trimmedBody;
    }

    private ChatMessageResult toResult(String roomCode, ChatMessageEntity message) {
        return new ChatMessageResult(
                message.getId(),
                roomCode,
                message.getPlayerId(),
                message.getSenderNickname(),
                message.getBody(),
                message.getCreatedAt()
        );
    }
}
