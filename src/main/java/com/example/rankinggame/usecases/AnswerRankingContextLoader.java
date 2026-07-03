package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.RoomCommand;
import com.example.rankinggame.dto.SortAnswerCommand;
import com.example.rankinggame.engine.exceptions.CaptainNotFoundException;
import com.example.rankinggame.entities.*;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
class AnswerRankingContextLoader {

    // TODO: Resolve duplicates with AnswerSubmissionContextLoader

    private final RoomRepository roomRepository;
    private final RoundRepository roundRepository;
    private final PlayerRepository playerRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RoomCodeService roomCodeService;
    private final AnswerRepository answerRepository;

    public AnswerRankingContext load(SortAnswerCommand command) {
        RoomEntity room = requireRoom(command);
        PlayerEntity requestingPlayer = requirePlayerInRoom(command, room);
        RoundEntity round = requireRoundInRoom(room, command.roundId());
        AnswerEntity answer = requireAnswer(command.answerId());
        PlayerEntity captain = playerRepository.findById(round.getCaptainPlayerId())
                .orElseThrow(CaptainNotFoundException::new);
        return new AnswerRankingContext(round, answer, captain);
    }

    private AnswerEntity requireAnswer(UUID answerId) {
        return answerRepository.findById(answerId)
                .orElseThrow(AnswerNotFoundException::new);
    }

    private RoomEntity requireRoom(RoomCommand command) {
        String roomCode = roomCodeService.normalizeRoomCode(command);
        return roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));
    }

    private PlayerEntity requirePlayerInRoom(SortAnswerCommand command, RoomEntity room) {
        if (command == null || command.playerId() == null) {
            throw new HostPlayerIdRequiredException();
        }

        return playerRepository.findById(command.playerId())
                .filter(p -> Objects.equals(p.getRoomId(), room.getId()))
                .orElseThrow(PlayerNotInRoomException::new);
    }

    private RoundEntity requireRoundInRoom(RoomEntity room, UUID roundId) {
        RoundEntity round = roundRepository.findById(roundId)
                .orElseThrow(RoundNotPartOfActiveGameException::new);
        GameSession gameSession = gameSessionRepository.findByRoomId(room.getId())
                .filter(candidate -> candidate.getId().equals(round.getGameSessionId()))
                .orElseThrow(RoundNotPartOfActiveGameException::new);

        if (!Objects.equals(gameSession.getRoomId(), room.getId())) {
            throw new RoundNotPartOfActiveGameException();
        }

        return round;
    }
}
