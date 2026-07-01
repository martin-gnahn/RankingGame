package com.example.rankinggame.usecases;

import com.example.rankinggame.engine.GameParticipant;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.repositories.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class RoundProgressService {
    private final RoundRepository roundRepository;
    private final GameParticipantContextLoader gameParticipantContextLoader;

    public AnswerSubmissionProgress updateAfterAnswerSubmitted(AnswerSubmissionContext context, Round domainRound) {
        var gameSession = context.gameSession();
        var roundEntity = context.round();
        List<GameParticipant> requiredPlayers =
                gameParticipantContextLoader.getAllPlayers(gameSession);
        domainRound.markSortingIfAllAnswersSubmitted(requiredPlayers);
        boolean allAnswersSubmitted = domainRound.allAnswersSubmitted(requiredPlayers);
        if (allAnswersSubmitted) {
            roundRepository.save(roundEntity);
        }

        return new AnswerSubmissionProgress(domainRound.getSubmittedAnswers().size(), requiredPlayers.size(), allAnswersSubmitted);
    }
}
