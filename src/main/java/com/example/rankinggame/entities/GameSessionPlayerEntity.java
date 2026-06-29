package com.example.rankinggame.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "game_session_players")
@IdClass(GameSessionPlayerId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameSessionPlayerEntity {
    @Id
    @Column(name = "game_session_id", nullable = false)
    private UUID gameSessionId;

    @Id
    @Column(name = "player_id", nullable = false)
    private UUID playerId;
}
