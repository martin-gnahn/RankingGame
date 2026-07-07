package com.example.rankinggame.usecases;

import com.example.rankinggame.engine.Round;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.mapper.RoundMapper;
import com.example.rankinggame.repositories.AnswerRepository;
import com.example.rankinggame.repositories.RankingRepository;
import com.example.rankinggame.repositories.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// TODO: this is a very critical and complex service. Check that in detail.
@RequiredArgsConstructor
@Service
public class RoundProgressService {
    private final RoundRepository roundRepository;
    private final GameParticipantContextLoader gameParticipantContextLoader;
    private final RoundMapper roundMapper;
    private final AnswerRepository answerRepository;
    private final RankingRepository rankingRepository;

    public AnswerSubmissionProgress updateAfterAnswerSubmitted(AnswerSubmissionContext context, Round domainRound) {
        var gameSession = context.gameSession();
        var roundEntity = context.round();
        // pessimistic lock to prevent that two players simultaneously set round state to sorting
        roundRepository.findByIdForUpdate(roundEntity.getId())
                .orElseThrow(RoundNotPartOfActiveGameException::new);

        int submittedAnswersCount = answerRepository.countByRoundId(roundEntity.getId());
        int requiredAnswersCount = gameParticipantContextLoader.getPlayersCount(gameSession);
        boolean allAnswersSubmitted = requiredAnswersCount > 0 && submittedAnswersCount >= requiredAnswersCount;
        boolean shouldStartSorting = domainRound.startRankingIfAllowed(submittedAnswersCount, requiredAnswersCount);
        boolean sortingHasStarted = false;

        if (shouldStartSorting) {
            RoundState newState = roundMapper.toEntityState(domainRound.getRoundStatus());
            int updatedRows = roundRepository.updateStateIfCurrent(
                    roundEntity.getId(),
                    RoundState.ANSWER_SUBMISSION,
                    newState
            );
            sortingHasStarted = updatedRows == 1;
            if (sortingHasStarted) {
                roundEntity.setState(newState);
            }
        }

        return new AnswerSubmissionProgress(
                submittedAnswersCount,
                requiredAnswersCount,
                allAnswersSubmitted,
                sortingHasStarted
        );
    }

    public AnswerRankingProgress updateAfterAnswerRanked(AnswerRankingContext context) {
        var roundEntity = context.round();
        // pessimistic lock to prevent that two host requests simultaneously advance the round
        roundRepository.findByIdForUpdate(roundEntity.getId())
                .orElseThrow(RoundNotPartOfActiveGameException::new);

        int rankedAnswerCount = rankingRepository.countByRoundId(roundEntity.getId());
        int submittedAnswerCount = answerRepository.countByRoundId(roundEntity.getId());
        boolean allSubmittedAnswersRanked =
                submittedAnswerCount > 0 && rankedAnswerCount >= submittedAnswerCount;
        boolean resultHasStarted = false;

        if (allSubmittedAnswersRanked) {
            int updatedRows = roundRepository.updateStateIfCurrent(
                    roundEntity.getId(),
                    RoundState.SORTING,
                    RoundState.RESULT
            );
            resultHasStarted = updatedRows == 1;
            if (resultHasStarted) {
                roundEntity.setState(RoundState.RESULT);
            }
        }

        return new AnswerRankingProgress(
                rankedAnswerCount,
                submittedAnswerCount,
                allSubmittedAnswersRanked,
                resultHasStarted
        );
    }
}
