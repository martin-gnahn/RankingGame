package com.example.rankinggame.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameSessionPlayerId implements Serializable {
    private UUID gameSessionId;
    private UUID playerId;
}
