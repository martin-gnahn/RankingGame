package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.QuestionEntity;

import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {
    QuestionEntity save(QuestionEntity question);

    Optional<QuestionEntity> findById(UUID id);

    Optional<QuestionEntity> findRandomActive();
}
