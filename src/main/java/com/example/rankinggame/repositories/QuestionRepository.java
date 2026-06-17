package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.Question;

import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {
    Question save(Question question);

    Optional<Question> findById(UUID id);

    Optional<Question> findRandomActive();
}
