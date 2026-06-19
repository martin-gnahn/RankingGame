package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.StartRankingGameCommand;
import com.example.rankinggame.dto.StartRankingGameResult;
import com.example.rankinggame.engine.Game;
import com.example.rankinggame.engine.Player;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.GameSessionStatus;
import com.example.rankinggame.entities.GameType;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.Question;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.entities.RoundEntity;
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

import java.util.*;

@RequiredArgsConstructor
@Service
public class StartRankingGameService {
    private static final int FIRST_ROUND_NUMBER = 1;

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final QuestionRepository questionRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RoundRepository roundRepository;
    private final RoundCardAssignmentService roundCardAssignmentService;
    private final ApplicationEventPublisher eventPublisher;

    // TODO: here new game
    @Transactional
    public StartRankingGameResult startGame(StartRankingGameCommand command) {
        String roomCode = normalizeRoomCode(command);
        UUID hostPlayerId = requireHostPlayerId(command);

        RoomEntity room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));

        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new IllegalArgumentException("Room is not in lobby");
        }

        PlayerEntity hostPlayer = playerRepository.findById(hostPlayerId)
                .filter(player -> Objects.equals(player.getRoomId(), room.getId()))
                .filter(PlayerEntity::isHost)
                .orElseThrow(() -> new IllegalArgumentException("Only the host can start the game"));

        if (!Objects.equals(room.getHostPlayerId(), hostPlayer.getId())) {
            throw new IllegalArgumentException("Only the host can start the game");
        }

        if (!hasConnectedGuest(room, hostPlayer)) {
            throw new IllegalArgumentException("At least two online players are required to start the game");
        }

        Question question = questionRepository.findRandomActive()
                .orElseThrow(QuestionUnavailableException::new);

        var playerEntities = getPlayerList(room, hostPlayer);
        var players = playerEntities.stream().map(p -> new Player(new PlayerId(p.getId()), p.getNickname())).toList();
        Game game = new Game(players);
        game.start();



        GameSession gameSession = new GameSession();
        gameSession.setRoomId(room.getId());
        gameSession.setGameType(GameType.RANKING_GAME);
        gameSession.setStatus(GameSessionStatus.IN_PROGRESS);
        gameSession.setCurrentRoundNumber(FIRST_ROUND_NUMBER);
        GameSession savedGameSession = gameSessionRepository.save(gameSession);

        RoundEntity round = new RoundEntity();
        round.setGameSessionId(savedGameSession.getId());
        round.setQuestionId(question.getId());
        round.setRoundNumber(FIRST_ROUND_NUMBER);
        round.setState(RoundState.QUESTION_REVEALED);
        RoundEntity savedRound = roundRepository.save(round);

        room.setStatus(RoomStatus.IN_GAME);
        RoomEntity savedRoom = roomRepository.save(room);

        roundCardAssignmentService.assignedCardValue(savedRoom.getId(), savedRound.getId(), hostPlayer.getId());

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

    private List<PlayerEntity> getPlayerList(RoomEntity room, PlayerEntity hostPlayer) {
        List<PlayerEntity> connectedPlayers = new ArrayList<>(playerRepository.findByRoomId(room.getId()).stream()
                .filter(player -> !Objects.equals(player.getId(), hostPlayer.getId())
                        && player.getConnectionStatus() == PlayerConnectionStatus.CONNECTED).toList());
        connectedPlayers.add(hostPlayer);
        return connectedPlayers;
    }

    // TODO: extract to pure game logic (GameEngine) class
    private boolean hasConnectedGuest(RoomEntity room, PlayerEntity hostPlayer) {
        return playerRepository.findByRoomId(room.getId()).stream()
                .anyMatch(player -> !Objects.equals(player.getId(), hostPlayer.getId())
                        && player.getConnectionStatus() == PlayerConnectionStatus.CONNECTED);
    }

    // TODO: custom exception (domain specific, understandable)
    private UUID requireHostPlayerId(StartRankingGameCommand command) {
        if (command == null || command.hostPlayerId() == null) {
            throw new IllegalArgumentException("Host player id is required");
        }

        return command.hostPlayerId();
    }
}
