package com.example.rankinggame.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
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

    // TODO: extract to room. Game should not know room id.
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

    @OneToMany
    List<PlayerEntity> players;

    @OneToMany
    List<RoundEntity> rounds;
}
