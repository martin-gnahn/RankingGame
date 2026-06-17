package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaAnswerRepository extends JpaRepository<Answer, UUID>, AnswerRepository {
}
