package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.SortAnswersCommand;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.repositories.GameSessionRepository;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoomRepository;
import com.example.rankinggame.repositories.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SortAnswersService {
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RoundRepository roundRepository;
    private final RoomCodeService roomCodeService;

    @Transactional(readOnly = true)
    public void sortAnswers(SortAnswersCommand command) {
        String roomCode = roomCodeService.normalizeRoomCode(command);
        if (command.roundId() == null) {
            throw new RoundIdRequiredException();
        }
        UUID hostPlayerId = requireHostPlayerId(command);

        RoomEntity room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));
        requireHost(room, hostPlayerId);
        RoundEntity round = requireRoundInRoom(room, command.roundId());
        requireSortingRound(round);
    }

    private UUID requireHostPlayerId(SortAnswersCommand command) {
        if (command == null || command.hostPlayerId() == null) {
            throw new HostPlayerIdRequiredException();
        }

        return command.hostPlayerId();
    }

    private void requireHost(RoomEntity room, UUID hostPlayerId) {
        PlayerEntity hostPlayer = playerRepository.findById(hostPlayerId)
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

    private void requireSortingRound(RoundEntity round) {
        if (round.getState() != RoundState.SORTING) {
            throw new IllegalArgumentException("Answers can only be sorted in sorting mode");
        }
    }
}
