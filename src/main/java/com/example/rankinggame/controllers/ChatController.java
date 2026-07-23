package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.ChatMessageResponse;
import com.example.rankinggame.dto.ChatMessageResult;
import com.example.rankinggame.engine.GameConstants;
import com.example.rankinggame.usecases.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rooms/{roomCode}/chat/messages")
public class ChatController {
    private final ChatMessageService chatMessageService;

    @GetMapping
    public List<ChatMessageResponse> getRecentMessages(
            @PathVariable String roomCode,
            @RequestHeader(value = GameConstants.PLAYER_SESSION_TOKEN, required = false) String token
    ) {
        return chatMessageService.getRecentMessages(roomCode).stream()
                .map(this::toResponse)
                .toList();
    }

    private ChatMessageResponse toResponse(ChatMessageResult result) {
        return new ChatMessageResponse(
                result.messageId(),
                result.playerId(),
                result.senderNickname(),
                result.body(),
                result.createdAt()
        );
    }
}
