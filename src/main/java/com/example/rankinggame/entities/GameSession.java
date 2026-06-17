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
@Table(name = "game_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false, length = 50)
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GameSessionStatus status;

    @Column(name = "current_round_number", nullable = false)
    private int currentRoundNumber;
}
