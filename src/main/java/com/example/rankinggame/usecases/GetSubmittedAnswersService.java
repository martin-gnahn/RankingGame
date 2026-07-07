package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.GetSubmittedAnswersCommand;
import com.example.rankinggame.dto.SubmittedAnswerResult;
import com.example.rankinggame.dto.SubmittedAnswersResult;
import com.example.rankinggame.entities.*;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetSubmittedAnswersService {
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RoundRepository roundRepository;
    private final AnswerRepository answerRepository;
    private final RoomCodeService roomCodeService;

    @Transactional(readOnly = true)
    public SubmittedAnswersResult getSubmittedAnswers(GetSubmittedAnswersCommand command) {
        String roomCode = roomCodeService.normalizeRoomCode(command);
        if (command.roundId() == null) {
            throw new RoundIdRequiredException();
        }
        if (command.requesterPlayerId() == null) {
            throw new PlayerIdRequiredException();
        }

        RoomEntity room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));
        requireRoomPlayer(room, command.requesterPlayerId());
        RoundEntity round = requireRoundInRoom(room, command.roundId());
        requireSortingRound(round);
        Map<UUID, PlayerEntity> playersById = playerRepository.findByRoomId(room.getId()).stream()
                .collect(Collectors.toMap(PlayerEntity::getId, Function.identity()));

        return new SubmittedAnswersResult(answerRepository.findByRoundIdOrderBySubmittedAtAsc(round.getId()).stream()
                .map(answer -> toResult(answer, playersById))
                .toList());
    }

    private void requireRoomPlayer(RoomEntity room, UUID requesterPlayerId) {
        playerRepository.findById(requesterPlayerId)
                .filter(player -> Objects.equals(player.getRoomId(), room.getId()))
                .orElseThrow(OnlyRoomPlayersCanQueryAnswers::new);
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

    private void requireSortingRound(RoundEntity round) {
        if (round.getState() != RoundState.SORTING) {
            throw new IllegalArgumentException("Submitted answers are only available in sorting mode");
        }
    }

    private SubmittedAnswerResult toResult(AnswerEntity answer, Map<UUID, PlayerEntity> playersById) {
        PlayerEntity player = playersById.get(answer.getPlayerId());
        String nickname = player == null ? null : player.getNickname();
        return new SubmittedAnswerResult(
                answer.getId(),
                answer.getPlayerId(),
                nickname,
                answer.getText()
        );
    }
}
