package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.SubmitAnswerCommand;
import com.example.rankinggame.engine.exceptions.CaptainNotFoundException;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.repositories.GameSessionRepository;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import com.example.rankinggame.repositories.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
class AnswerSubmissionContextLoader {

    private final RoomRepository roomRepository;
    private final RoundRepository roundRepository;
    private final PlayerRepository playerRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RoomCodeService roomCodeService;

    public AnswerSubmissionContext load(SubmitAnswerCommand command) {
        String normalizedRoomCode = roomCodeService.normalizeRoomCode(command);
        RoomEntity room = roomRepository.findByCode(normalizedRoomCode)
                .orElseThrow(() -> new RoomNotFoundException(normalizedRoomCode));
        PlayerEntity requestingPlayer = playerRepository.findById(command.playerId())
                .filter(candidate -> candidate.getRoomId().equals(room.getId()))
                .orElseThrow(PlayerNotInRoomException::new);
        RoundEntity round = roundRepository.findById(command.roundId())
                .orElseThrow(RoundNotPartOfActiveGameException::new);
        var gameSession = gameSessionRepository.findByRoomId(room.getId())
                .filter(candidate -> candidate.getId().equals(round.getGameSessionId()))
                .orElseThrow(RoundNotPartOfActiveGameException::new);
        var captainPlayer = playerRepository.findById(round.getCaptainPlayerId())
                .orElseThrow(CaptainNotFoundException::new);
        return new AnswerSubmissionContext(room, requestingPlayer, round, gameSession, captainPlayer);
    }
}
