package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.StartRankingGameCommand;
import com.example.rankinggame.dto.StartRankingGameResult;
import com.example.rankinggame.engine.Game;
import com.example.rankinggame.engine.GameParticipant;
import com.example.rankinggame.engine.Question;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.QuestionEntity;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.events.GameStartedRoomEvent;
import com.example.rankinggame.exceptions.QuestionUnavailableException;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.mapper.GameMapper;
import com.example.rankinggame.mapper.PlayerMapper;
import com.example.rankinggame.mapper.QuestionMapper;
import com.example.rankinggame.mapper.RoundMapper;
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
    private static final int MIN_ONLINE_PLAYERS_TO_START = 2;

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final QuestionRepository questionRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RoundRepository roundRepository;
    private final RoundCardAssignmentService roundCardAssignmentService;
    private final ApplicationEventPublisher eventPublisher;
    private final PlayerMapper playerMapper;
    private final GameMapper gameMapper;
    private final RoundMapper roundMapper;
    private final QuestionMapper questionMapper;
    private final RoomCodeService roomCodeService;

    @Transactional
    public StartRankingGameResult startGame(StartRankingGameCommand command) {
        String roomCode = roomCodeService.normalizeRoomCode(command);
        UUID hostPlayerId = requireHostPlayerId(command);

        RoomEntity room = requireLobbyRoom(roomCode);
        PlayerEntity hostPlayer = requireRoomHost(room, hostPlayerId);

        List<PlayerEntity> playerEntities = getPlayersSortedByJoinedAt(room);
        List<GameParticipant> participants = playerMapper.toDomain(playerEntities);

        QuestionEntity questionEntity = requireRandomActiveQuestion();
        Game game = startDomainGame(participants, questionEntity);

        GameSession savedGameSession = saveGameSession(game, room);
        RoundEntity savedRound = saveFirstRound(game, savedGameSession, questionEntity, hostPlayer);
        RoomEntity savedRoom = moveRoomIntoGame(room);

        assignFirstRoundCard(savedRoom, savedRound, hostPlayer);
        publishGameStarted(savedRoom, savedGameSession);

        return toStartRankingGameResult(savedRoom, savedGameSession, savedRound);
    }

    private RoomEntity requireLobbyRoom(String roomCode) {
        RoomEntity room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));

        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new IllegalArgumentException("Room is not in lobby");
        }

        return room;
    }

    private PlayerEntity requireRoomHost(RoomEntity room, UUID hostPlayerId) {
        PlayerEntity hostPlayer = playerRepository.findById(hostPlayerId)
                .filter(player -> Objects.equals(player.getRoomId(), room.getId()))
                .filter(PlayerEntity::isHost)
                .orElseThrow(OnlyHostCanStartGame::new);

        if (!Objects.equals(room.getHostPlayerId(), hostPlayer.getId())) {
            throw new OnlyHostCanStartGame();
        }

        return hostPlayer;
    }

    private QuestionEntity requireRandomActiveQuestion() {
        return questionRepository.findRandomActive()
                .orElseThrow(QuestionUnavailableException::new);
    }

    private Game startDomainGame(List<GameParticipant> participants, QuestionEntity questionEntity) {
        Question firstQuestion = questionMapper.toDomain(questionEntity);
        Game game = new Game(participants);
        game.start(firstQuestion);
        return game;
    }

    private GameSession saveGameSession(Game game, RoomEntity room) {
        GameSession gameSession = gameMapper.toEntity(game);
        gameSession.setRoomId(room.getId());
        gameSession.setCurrentRoundNumber(FIRST_ROUND_NUMBER);
        return gameSessionRepository.save(gameSession);
    }

    private RoundEntity saveFirstRound(
            Game game,
            GameSession savedGameSession,
            QuestionEntity questionEntity,
            PlayerEntity hostPlayer
    ) {
        RoundEntity round = roundMapper.toEntity(game.getCurrentRound());
        round.setGameSessionId(savedGameSession.getId());
        round.setQuestionEntity(questionEntity);
        round.setCaptainPlayerId(hostPlayer.getId());
        round.setState(RoundState.QUESTION_REVEALED);
        return roundRepository.save(round);
    }

    private RoomEntity moveRoomIntoGame(RoomEntity room) {
        room.setStatus(RoomStatus.IN_GAME);
        return roomRepository.save(room);
    }

    private void assignFirstRoundCard(RoomEntity room, RoundEntity round, PlayerEntity hostPlayer) {
        roundCardAssignmentService.assignedCardValue(room.getId(), round.getId(), hostPlayer.getId());
    }

    private void publishGameStarted(RoomEntity room, GameSession gameSession) {
        eventPublisher.publishEvent(new GameStartedRoomEvent(
                room.getCode(),
                gameSession.getId(),
                gameSession.getGameType()
        ));
    }

    private StartRankingGameResult toStartRankingGameResult(
            RoomEntity room,
            GameSession gameSession,
            RoundEntity round
    ) {
        return new StartRankingGameResult(
                new StartRankingGameResult.StartedRoom(room.getId(), room.getCode()),
                new StartRankingGameResult.StartedGame(gameSession.getId(), gameSession.getGameType()),
                new StartRankingGameResult.StartedRound(
                        round.getId(),
                        gameSession.getCurrentRoundNumber(),
                        round.getQuestionId()
                )
        );
    }

    private List<PlayerEntity> getPlayersSortedByJoinedAt(RoomEntity room) {
        return new ArrayList<>(playerRepository.findByRoomId(room.getId()).stream()
                .filter(player -> player.getConnectionStatus() == PlayerConnectionStatus.CONNECTED)
                .sorted(Comparator.comparing(PlayerEntity::getJoinedAt))
                .toList());
    }

    private UUID requireHostPlayerId(StartRankingGameCommand command) {
        if (command == null || command.hostPlayerId() == null) {
            throw new HostPlayerIdRequiredException();
        }

        return command.hostPlayerId();
    }
}
