package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.Player;
import com.example.rankinggame.entities.RoundCardAssignment;
import com.example.rankinggame.repositories.JpaRoundCardAssignmentRepository;
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

@Service
@RequiredArgsConstructor
public class RoundCardAssignmentService {
    private static final int MIN_CARD_VALUE = 1;
    private static final int MAX_CARD_VALUE = 10;
    private static final IntStream ONE_TO_TEN_INT_STREAM = IntStream.rangeClosed(MIN_CARD_VALUE, MAX_CARD_VALUE);

    private final PlayerRepository playerRepository;
    private final RoundCardAssignmentRepository roundCardAssignmentRepository;
    private RandomGenerator randomGenerator = new SecureRandom();

    @Transactional
    public int assignedCardValue(UUID roomId, UUID roundId, UUID playerId) {
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

    private int assignMissingCards(UUID roomId, UUID roundId, UUID requestedPlayerId) {
        List<Player> players = playerRepository.findByRoomId(roomId).stream()
                .sorted(Comparator.comparing(Player::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Player::getNickname)
                        .thenComparing(Player::getId))
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
        List<Integer> availableCardValues = ONE_TO_TEN_INT_STREAM
                .filter(cardValue -> !assignedCardValues.contains(cardValue))
                .boxed()
                .collect(Collectors.toCollection(ArrayList::new));
        shuffle(availableCardValues);

        List<Player> playersWithoutCard = players.stream()
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
        }
        roundCardAssignmentRepository.saveAll(assignments);

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
