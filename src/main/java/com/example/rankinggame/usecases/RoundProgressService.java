package com.example.rankinggame.usecases;

import com.example.rankinggame.engine.GameParticipant;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.entities.GameSession;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.mapper.RoundMapper;
import com.example.rankinggame.repositories.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class RoundProgressService {
    private final RoundRepository roundRepository;
    private final GameParticipantContextLoader gameParticipantContextLoader;
    private final RoundMapper roundMapper;

    public AnswerSubmissionProgress updateAfterAnswerSubmitted(GameSession gameSession, Round domainRound) {
        List<GameParticipant> requiredPlayers =
                gameParticipantContextLoader.getAllPlayers(gameSession);
        domainRound.markSortingIfAllAnswersSubmitted(requiredPlayers);
        boolean allAnswersSubmitted = domainRound.allAnswersSubmitted(requiredPlayers);
        if (allAnswersSubmitted) {
            RoundEntity roundEntity = roundMapper.toEntity(domainRound);
            roundRepository.save(roundEntity);
        }

        return new AnswerSubmissionProgress(domainRound.getSubmittedAnswers().size(), requiredPlayers.size(), allAnswersSubmitted);
    }
}
