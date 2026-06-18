package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.StartRankingGameCommand;
import com.example.rankinggame.dto.StartRankingGameResult;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.GameSessionStatus;
import com.example.rankinggame.entities.GameType;
import com.example.rankinggame.entities.Player;
import com.example.rankinggame.entities.Question;
import com.example.rankinggame.entities.Room;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.entities.Round;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.events.GameStartedRoomEvent;
import com.example.rankinggame.exceptions.QuestionUnavailableException;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.repositories.GameSessionRepository;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.QuestionRepository;
import com.example.rankinggame.repositories.RoomRepository;
import com.example.rankinggame.repositories.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class StartRankingGameService {
    private static final int FIRST_ROUND_NUMBER = 1;

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final QuestionRepository questionRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RoundRepository roundRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public StartRankingGameResult startGame(StartRankingGameCommand command) {
        String roomCode = normalizeRoomCode(command);
        UUID hostPlayerId = requireHostPlayerId(command);

        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));

        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new IllegalArgumentException("Room is not in lobby");
        }

        Player hostPlayer = playerRepository.findById(hostPlayerId)
                .filter(player -> Objects.equals(player.getRoomId(), room.getId()))
                .filter(Player::isHost)
                .orElseThrow(() -> new IllegalArgumentException("Only the host can start the game"));

        if (!Objects.equals(room.getHostPlayerId(), hostPlayer.getId())) {
            throw new IllegalArgumentException("Only the host can start the game");
        }

        Question question = questionRepository.findRandomActive()
                .orElseThrow(QuestionUnavailableException::new);

        GameSession gameSession = new GameSession();
        gameSession.setRoomId(room.getId());
        gameSession.setGameType(GameType.RANKING_GAME);
        gameSession.setStatus(GameSessionStatus.IN_PROGRESS);
        gameSession.setCurrentRoundNumber(FIRST_ROUND_NUMBER);
        GameSession savedGameSession = gameSessionRepository.save(gameSession);

        Round round = new Round();
        round.setGameSessionId(savedGameSession.getId());
        round.setQuestionId(question.getId());
        round.setRoundNumber(FIRST_ROUND_NUMBER);
        round.setState(RoundState.QUESTION_REVEALED);
        Round savedRound = roundRepository.save(round);

        room.setStatus(RoomStatus.IN_GAME);
        Room savedRoom = roomRepository.save(room);

        eventPublisher.publishEvent(new GameStartedRoomEvent(
                savedRoom.getCode(),
                savedGameSession.getId(),
                savedGameSession.getGameType()
        ));

        // TODO: can we decrease the arguments of this constructor. It looks a bit obese.
        return new StartRankingGameResult(
                savedRoom.getId(),
                savedRoom.getCode(),
                savedGameSession.getId(),
                savedGameSession.getGameType(),
                savedRound.getId(),
                savedRound.getRoundNumber(),
                savedRound.getQuestionId()
        );
    }

    private String normalizeRoomCode(StartRankingGameCommand command) {
        if (command == null || command.roomCode() == null) {
            throw new IllegalArgumentException("Room code is required");
        }

        String roomCode = command.roomCode().trim().toUpperCase(Locale.ROOT);

        if (roomCode.isBlank()) {
            throw new IllegalArgumentException("Room code is required");
        }

        return roomCode;
    }

    // TODO: custom exception (domain specific, understandable)
    private UUID requireHostPlayerId(StartRankingGameCommand command) {
        if (command == null || command.hostPlayerId() == null) {
            throw new IllegalArgumentException("Host player id is required");
        }

        return command.hostPlayerId();
    }
}
