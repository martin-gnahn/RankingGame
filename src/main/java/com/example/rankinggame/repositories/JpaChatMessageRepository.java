package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID>, ChatMessageRepository {
}
