package com.example.rankinggame.controllers;

import com.example.rankinggame.dto.SortAnswersCommand;
import com.example.rankinggame.entities.*;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.repositories.*;
import com.example.rankinggame.usecases.HostPlayerIdRequiredException;
import com.example.rankinggame.usecases.OnlyHostCanSortAnswers;
import com.example.rankinggame.usecases.RoomCodeService;
import com.example.rankinggame.usecases.RoundIdRequiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
class SortAnswerService {
    private final RoomCodeService roomCodeService;
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final RoundRepository roundRepository;
    private final GameSessionRepository gameSessionRepository;
    private final JpaRankingRepository rankingRepository;
    private final JpaAnswerRepository jpaAnswerRepository;

    // TODO: implement domain specific sorting algorithm, which checks the right order of the cards

    @Transactional
    public RankingEntity addRanking(SortAnswersCommand command) {
        if (command.roundId() == null) {
            throw new RoundIdRequiredException();
        }
        RoomEntity room = requireRoom(command);
        checkIfPlayerIdIsFromHost(command, room);
        RoundEntity round = requireRoundInRoom(room, command.roundId());
        AnswerEntity answer = requireAnswerInRound(round, command.answerId());
        checkIfRoundIsInSortingState(round);
        checkIfAnswerAlreadyAddedToRanking(round, answer);

        // all validations passed
        // TODO: use domain/repository mapping here
        int nextPosition = rankingRepository.findMaxPosition(round.getId()) + 1;
        RankingEntity ranking = new RankingEntity(UUID.randomUUID(), answer, round.getId(), nextPosition);
        log.info("Added sorting for answer '{}' to new position {} (starting at position 1).", answer.getText(), nextPosition);
        return rankingRepository.save(ranking);
    }

    private void checkIfAnswerAlreadyAddedToRanking(RoundEntity round, AnswerEntity answer) {
        var existingRanking = rankingRepository.findByRoundIdAndAnswer(round.getId(), answer);
        if (existingRanking.isPresent()) {
            // TODO: change this to custom AnswerAlreadyHaveBeenRankedException
            throw new IllegalArgumentException("Answer already has been ranked");
        }
    }

    private AnswerEntity requireAnswerInRound(RoundEntity round, UUID answerId) {
        AnswerEntity answer = jpaAnswerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Answer is not found"));

        if (!Objects.equals(answer.getRoundId(), round.getId())) {
            throw new IllegalArgumentException("Answer is not part of the requested round");
        }

        return answer;
    }

    // TODO: provide common interface for commands to make them reusable or so.
    private RoomEntity requireRoom(SortAnswersCommand command) {
        String roomCode = roomCodeService.normalizeRoomCode(command);
        return roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));
    }

    // TODO: provide common interface for commands to make them reusable or so.
    private RoomEntity requireRoom(GetAnswerOrderCommand command) {
        String roomCode = roomCodeService.normalizeRoomCode(command);
        return roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));
    }

    // TODO: maybe extract to host rule guard
    private void checkIfPlayerIdIsFromHost(SortAnswersCommand command, RoomEntity room) {
        if (command == null || command.hostPlayerId() == null) {
            throw new HostPlayerIdRequiredException();
        }

        PlayerEntity hostPlayer = playerRepository.findById(command.hostPlayerId())
                .filter(player -> Objects.equals(player.getRoomId(), room.getId()))
                .filter(PlayerEntity::isHost)
                .orElseThrow(OnlyHostCanSortAnswers::new);

        if (!Objects.equals(room.getHostPlayerId(), hostPlayer.getId())) {
            throw new OnlyHostCanSortAnswers();
        }
    }

    private RoundEntity requireRoundInRoom(RoomEntity room, UUID roundId) {
        RoundEntity round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round is not part of the active game"));
        GameSession gameSession = gameSessionRepository.findByRoomId(room.getId())
                .filter(candidate -> candidate.getId().equals(round.getGameSessionId()))
                .orElseThrow(() -> new IllegalArgumentException("Round is not part of the active game"));

        if (!Objects.equals(gameSession.getRoomId(), room.getId())) {
            throw new IllegalArgumentException("Round is not part of the active game");
        }

        return round;
    }

    private void checkIfRoundIsInSortingState(RoundEntity round) {
        if (round.getState() != RoundState.SORTING) {
            throw new IllegalArgumentException("Answers can only be sorted in sorting mode");
        }
    }

    public List<RankingEntity> getOrderOfAnswers(GetAnswerOrderCommand command) {
        if (command.roundId() == null) {
            throw new RoundIdRequiredException();
        }
        RoomEntity room = requireRoom(command);
        RoundEntity round = requireRoundInRoom(room, command.roundId());
        checkIfRoundIsInSortingState(round);

        return rankingRepository.findAllByRoundIdOrderByPositionAsc(round.getId());
    }
}
