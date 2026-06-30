package com.example.rankinggame.usecases;

import com.example.rankinggame.dto.StartRankingGameCommand;
import com.example.rankinggame.dto.StartRankingGameResult;
import com.example.rankinggame.engine.Game;
import com.example.rankinggame.engine.GameParticipant;
import com.example.rankinggame.engine.Question;
import com.example.rankinggame.entities.*;
import com.example.rankinggame.events.GameStartedRoomEvent;
import com.example.rankinggame.exceptions.QuestionUnavailableException;
import com.example.rankinggame.exceptions.RoomHasNoActiveGameException;
import com.example.rankinggame.exceptions.RoomNotFoundException;
import com.example.rankinggame.exceptions.RoomNotInLobbyException;
import com.example.rankinggame.mapper.GameMapper;
import com.example.rankinggame.mapper.PlayerMapper;
import com.example.rankinggame.mapper.QuestionMapper;
import com.example.rankinggame.mapper.RoundMapper;
import com.example.rankinggame.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Service
public class StartRankingGameService {
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final QuestionRepository questionRepository;
    private final GameSessionRepository gameSessionRepository;
    private final GameSessionPlayerRepository gameSessionPlayerRepository;
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
        List<GameParticipant> participants = playerMapper.toParticipants(playerEntities);
        GameParticipant hostParticipant = playerMapper.toParticipant(hostPlayer);
        Game game = new Game(participants);
        game.requireCanStart(hostParticipant);

        QuestionEntity questionEntity = requireRandomActiveQuestion();
        startDomainGame(game, hostParticipant, questionEntity);

        GameSession savedGameSession = saveGameSession(game, room);
        saveGameParticipants(savedGameSession, playerEntities);
        RoundEntity savedRound = saveFirstRound(game, savedGameSession, questionEntity);
        RoomEntity savedRoom = moveRoomIntoGame(room);

        assignFirstRoundCard(savedRoom, savedRound, hostPlayer);
        publishGameStarted(savedRoom, savedGameSession);

        return toStartRankingGameResult(savedRoom, savedGameSession, savedRound);
    }

    private RoomEntity requireLobbyRoom(String roomCode) {
        RoomEntity room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));

        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new RoomNotInLobbyException(roomCode);
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

    private void startDomainGame(Game game, GameParticipant firstCaptain, QuestionEntity questionEntity) {
        Question firstQuestion = questionMapper.toDomain(questionEntity);
        game.start(firstQuestion, firstCaptain);
    }

    private GameSession saveGameSession(Game game, RoomEntity room) {
        GameSession gameSession = gameMapper.toEntity(game);
        gameSession.setRoomId(room.getId());
        return gameSessionRepository.save(gameSession);
    }

    private void saveGameParticipants(GameSession savedGameSession, List<PlayerEntity> playerEntities) {
        List<GameSessionPlayerEntity> gameSessionPlayers = playerEntities.stream()
                .map(player -> new GameSessionPlayerEntity(savedGameSession.getId(), player.getId()))
                .toList();

        gameSessionPlayerRepository.saveAll(gameSessionPlayers);
    }

    private RoundEntity saveFirstRound(
            Game game,
            GameSession savedGameSession,
            QuestionEntity questionEntity
    ) {
        RoundEntity round = roundMapper.toEntity(game.getCurrentRound());
        round.setGameSessionId(savedGameSession.getId());
        round.setQuestionEntity(questionEntity);
        return roundRepository.save(round);
    }

    private RoomEntity moveRoomIntoGame(RoomEntity room) {
        room.setStatus(RoomStatus.IN_GAME);
        return roomRepository.save(room);
    }

    private void assignFirstRoundCard(RoomEntity room, RoundEntity round, PlayerEntity hostPlayer) {
        roundCardAssignmentService.getCardValue(room.getId(), round.getId(), hostPlayer.getId());
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
                        gameSession.getCurrentRoundIndex(),
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

    public List<GameSessionPlayerEntity> getActivePlayers(String roomCode) {
        var room = roomRepository.findByCode(roomCode).orElseThrow(() -> new RoomNotFoundException(roomCode));
        var gameSession = gameSessionRepository.findByRoomId(room.getId()).orElseThrow(() -> new RoomHasNoActiveGameException(roomCode));
        return gameSessionPlayerRepository.findByGameSessionId(gameSession.getId());
    }
}
