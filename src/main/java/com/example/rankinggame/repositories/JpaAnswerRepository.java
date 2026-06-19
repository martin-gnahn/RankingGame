package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.AnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaAnswerRepository extends JpaRepository<AnswerEntity, UUID>, AnswerRepository {
}
