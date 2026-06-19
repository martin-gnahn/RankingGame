package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface JpaQuestionRepository extends JpaRepository<QuestionEntity, UUID>, QuestionRepository {
    @Override
    @Query(value = "SELECT * FROM questions WHERE active = TRUE ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<QuestionEntity> findRandomActive();
}
