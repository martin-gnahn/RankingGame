package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.ChatMessageEntity;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository {
    ChatMessageEntity save(ChatMessageEntity message);

    List<ChatMessageEntity> findTop50ByRoomIdOrderByCreatedAtDesc(UUID roomId);
}
