package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoundCardAssignment;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoundCardAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO: dirty. optimize.
@Service
@RequiredArgsConstructor
public class RoundCardAssignmentService {
    private static final int MIN_CARD_VALUE = 1;
    private static final int MAX_CARD_VALUE = 10;
    private static final List<Integer> CARD_VALUE_NUMBERS_LIST = IntStream.rangeClosed(MIN_CARD_VALUE, MAX_CARD_VALUE).boxed().toList();

    private final PlayerRepository playerRepository;
    private final RoundCardAssignmentRepository roundCardAssignmentRepository;
    private RandomGenerator randomGenerator = new SecureRandom();

    @Transactional
    public synchronized int getCardValue(UUID roomId, UUID roundId, UUID playerId) {
        if (playerId == null) {
            throw new IllegalArgumentException("Player id is required");
        }

        return roundCardAssignmentRepository.findByRoundIdAndPlayerId(roundId, playerId)
                .map(RoundCardAssignment::getCardValue)
                .orElseGet(() -> assignMissingCards(roomId, roundId, playerId));
    }

    void setRandomGenerator(RandomGenerator randomGenerator) {
        this.randomGenerator = randomGenerator;
    }

    // TODO: Overly complex. Reduce complexity.
    private int assignMissingCards(UUID roomId, UUID roundId, UUID requestedPlayerId) {
        List<PlayerEntity> players = playerRepository.findByRoomId(roomId).stream()
                .sorted(Comparator.comparing(PlayerEntity::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PlayerEntity::getNickname)
                        .thenComparing(PlayerEntity::getId))
                .toList();

        if (players.stream().noneMatch(player -> player.getId().equals(requestedPlayerId))) {
            throw new IllegalArgumentException("Player is not part of this room");
        }

        if (players.size() > MAX_CARD_VALUE) {
            throw new IllegalArgumentException("Only up to 10 players can receive unique cards");
        }

        List<RoundCardAssignment> existingAssignments = roundCardAssignmentRepository.findByRoundId(roundId);
        Set<UUID> assignedPlayerIds = existingAssignments.stream()
                .map(RoundCardAssignment::getPlayerId)
                .collect(Collectors.toSet());
        Set<Integer> assignedCardValues = existingAssignments.stream()
                .map(RoundCardAssignment::getCardValue)
                .collect(Collectors.toSet());
        List<Integer> availableCardValues = CARD_VALUE_NUMBERS_LIST
                .stream()
                .filter(cardValue -> !assignedCardValues.contains(cardValue))
                .collect(Collectors.toCollection(ArrayList::new));
        shuffle(availableCardValues);

        List<PlayerEntity> playersWithoutCard = players.stream()
                .filter(player -> !assignedPlayerIds.contains(player.getId()))
                .toList();

        Optional<RoundCardAssignment> requestedAssignment = Optional.empty();
        List<RoundCardAssignment> assignments = new ArrayList<>();
        int index = 0;
        for(var playerWithoutCard: playersWithoutCard) {
            RoundCardAssignment assignment = new RoundCardAssignment();
            assignment.setRoundId(roundId);
            assignment.setPlayerId(playerWithoutCard.getId());
            assignment.setCardValue(availableCardValues.get(index));
            // roundCardAssignmentRepository.save(assignment);
            assignments.add(assignment);
            if (assignment.getRoundId().equals(roundId) && assignment.getPlayerId().equals(requestedPlayerId)) {
                requestedAssignment = Optional.of(assignment);
            }
            index++;
            // TODO: temp solution. Prefer batch with .saveAll(.)
            roundCardAssignmentRepository.save(assignment);
        }
        // roundCardAssignmentRepository.saveAll(assignments);

        if (requestedAssignment.isEmpty()) {
            // TODO: use custom exception here.
            throw new IllegalStateException("Player card assignment could not be created");
        }

        return requestedAssignment.get().getCardValue();
    }

    private void shuffle(List<Integer> values) {
        for (int index = values.size() - 1; index > 0; index--) {
            int swapIndex = randomGenerator.nextInt(index + 1);
            Integer current = values.get(index);
            values.set(index, values.get(swapIndex));
            values.set(swapIndex, current);
        }
    }
}
