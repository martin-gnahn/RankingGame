package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.RoundCardAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoundCardAssignmentRepository {
    RoundCardAssignment save(RoundCardAssignment roundCardAssignment);

    List<RoundCardAssignment> findByRoundId(UUID roundId);

    Optional<RoundCardAssignment> findByRoundIdAndPlayerId(UUID roundId, UUID playerId);
}
