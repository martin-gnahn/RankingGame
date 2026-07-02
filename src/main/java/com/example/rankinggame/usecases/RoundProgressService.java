package com.example.rankinggame.usecases;

import com.example.rankinggame.engine.Round;
import com.example.rankinggame.mapper.RoundMapper;
import com.example.rankinggame.repositories.AnswerRepository;
import com.example.rankinggame.repositories.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RoundProgressService {
    private final RoundRepository roundRepository;
    private final GameParticipantContextLoader gameParticipantContextLoader;
    private final RoundMapper roundMapper;
    private final AnswerRepository answerRepository;

    public AnswerSubmissionProgress updateAfterAnswerSubmitted(AnswerSubmissionContext context, Round domainRound) {
        var gameSession = context.gameSession();
        var roundEntity = context.round();
        int submittedAnswersCount = answerRepository.countByRoundId(roundEntity.getId());
        int requiredAnswersCount = gameParticipantContextLoader.getPlayersCount(gameSession);
        boolean sortingHasStarted = domainRound.startSortingIfAllAnswersSubmitted(submittedAnswersCount, requiredAnswersCount);
        if (sortingHasStarted) {
            roundEntity.setState(roundMapper.toEntityState(domainRound.getRoundStatus()));
            roundRepository.save(roundEntity);
        }

        return new AnswerSubmissionProgress(submittedAnswersCount, requiredAnswersCount, sortingHasStarted);
    }
}
