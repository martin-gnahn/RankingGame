package com.example.rankinggame.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "rounds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Round {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_session_id", nullable = false)
    private UUID gameSessionId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RoundState state;
}
