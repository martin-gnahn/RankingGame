package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.ChatMessageResponse;
import com.example.rankinggame.dto.ChatMessageResult;
import com.example.rankinggame.usecases.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rooms/{roomCode}/chat/messages")
public class ChatController {
    private final ChatMessageService chatMessageService;

    @GetMapping
    public List<ChatMessageResponse> getRecentMessages(@PathVariable String roomCode) {
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
