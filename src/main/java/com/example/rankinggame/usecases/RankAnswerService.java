package com.example.rankinggame.usecases;

import com.example.rankinggame.controllers.GetRankingPositionsCommand;
import com.example.rankinggame.dto.AddRankingPositionCommand;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.RankedAnswer;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.engine.SubmittedAnswer;
import com.example.rankinggame.entities.RankedAnswerEntity;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.mapper.AnswerMapper;
import com.example.rankinggame.mapper.RankingMapper;
import com.example.rankinggame.mapper.RoundMapper;
import com.example.rankinggame.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class RankAnswerService {
    private final RoomCodeService roomCodeService;
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final RoundRepository roundRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RankingRepository rankingRepository;
    private final AnswerRepository jpaAnswerRepository;
    private final RoundMapper roundMapper;
    private final AnswerMapper answerMapper;
    private final RankingMapper rankingMapper;
    private final AnswerRankingContextLoader answerRankingContextLoader;

    // TODO: implement domain specific sorting algorithm, which checks the right order of the cards

    @Transactional
    public RankedAnswerEntity addRankingPosition(AddRankingPositionCommand command) {
        if (command.roundId() == null) {
            throw new RoundIdRequiredException();
        }
        AnswerRankingContext context = answerRankingContextLoader.load(command);

        SubmittedAnswer domainAnswer = answerMapper.toSubmittedAnswer(context.answer());
        var allAnswersInRound = jpaAnswerRepository.findByRoundIdOrderBySubmittedAtAsc(context.round().getId());
        var allRankingsInRound = rankingRepository.findByRoundIdOrderByPositionAsc(context.round().getId());
        Round domainRound =
                roundMapper.toDomain(context.round(), context.captainPlayer(), allAnswersInRound, allRankingsInRound);

        RankedAnswer newRankedAnswer = domainRound.rankAnswer(new PlayerId(command.playerId()), domainAnswer.answerId());
        RankedAnswerEntity newRankedAnswerEntity = rankingMapper.toEntity(newRankedAnswer, domainRound);

        // all validations passed
        log.info("Added sorting for answer '{}' to new position {} (starting at position 1).", context.answer().getText(), newRankedAnswer.getOneBasedPosition());
        return rankingRepository.save(newRankedAnswerEntity);
    }

    // TODO: maybe extract to host rule guard

    private void checkIfRoundIsInSortingState(RoundEntity round) {
        if (round.getState() != RoundState.SORTING) {
            throw new RoundNotInSortingStateException();
        }
    }

    public List<RankedAnswerEntity> getRankingPositions(GetRankingPositionsCommand command) {
        if (command.roundId() == null) {
            throw new RoundIdRequiredException();
        }
        AnswerRankingContext context = answerRankingContextLoader.load(command);
        checkIfRoundIsInSortingState(context.round());

        return rankingRepository.findByRoundIdOrderByPositionAsc(context.round().getId());
    }
}
