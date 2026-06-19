package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoundCardAssignment;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoundCardAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundCardAssignmentServiceTest {
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private RoundCardAssignmentRepository assignmentRepository;

    @InjectMocks
    private RoundCardAssignmentService service;

    @Test
    void assignsUniqueCardsToPlayersInRound() {
        service.setRandomGenerator(new FixedRandomGenerator());
        UUID roomId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID firstPlayerId = UUID.randomUUID();
        UUID secondPlayerId = UUID.randomUUID();
        UUID thirdPlayerId = UUID.randomUUID();
        List<RoundCardAssignment> assignments = new ArrayList<>();

        when(playerRepository.findByRoomId(roomId)).thenReturn(List.of(
                player(firstPlayerId, roomId, "Marta", Instant.parse("2026-06-18T08:00:00Z")),
                player(secondPlayerId, roomId, "Alex", Instant.parse("2026-06-18T08:00:01Z")),
                player(thirdPlayerId, roomId, "Sam", Instant.parse("2026-06-18T08:00:02Z"))
        ));
        when(assignmentRepository.findByRoundId(any(UUID.class))).thenAnswer(invocation -> {
            UUID requestedRoundId = invocation.getArgument(0);
            return assignments.stream()
                    .filter(assignment -> assignment.getRoundId().equals(requestedRoundId))
                    .toList();
        });
        when(assignmentRepository.findByRoundIdAndPlayerId(any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
            UUID requestedRoundId = invocation.getArgument(0);
            UUID requestedPlayerId = invocation.getArgument(1);
            return assignments.stream()
                    .filter(assignment -> assignment.getRoundId().equals(requestedRoundId))
                    .filter(assignment -> assignment.getPlayerId().equals(requestedPlayerId))
                    .findFirst();
        });
        when(assignmentRepository.save(any(RoundCardAssignment.class))).thenAnswer(invocation -> {
            RoundCardAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            assignments.add(assignment);
            return assignment;
        });

        int assignedCard = service.assignedCardValue(roomId, roundId, firstPlayerId);

        assertThat(assignedCard).isBetween(1, 10);
        assertThat(assignments).hasSize(3);
        assertThat(assignments)
                .extracting(RoundCardAssignment::getCardValue)
                .doesNotHaveDuplicates()
                .allSatisfy(cardValue -> assertThat(cardValue).isBetween(1, 10));

        service.assignedCardValue(roomId, roundId, secondPlayerId);
        verify(assignmentRepository, times(3)).save(any(RoundCardAssignment.class));

        service.assignedCardValue(roomId, UUID.randomUUID(), thirdPlayerId);
        verify(assignmentRepository, times(6)).save(any(RoundCardAssignment.class));
    }

    private PlayerEntity player(UUID playerId, UUID roomId, String nickname, Instant joinedAt) {
        PlayerEntity player = new PlayerEntity();
        player.setId(playerId);
        player.setRoomId(roomId);
        player.setNickname(nickname);
        player.setJoinedAt(joinedAt);
        return player;
    }

    private static class FixedRandomGenerator implements RandomGenerator {
        @Override
        public long nextLong() {
            return 0;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
