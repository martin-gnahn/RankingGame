package com.example.rankinggame.usecases;

import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.RoomEntity;
import com.example.rankinggame.entities.RoundEntity;
import com.example.rankinggame.entities.RoundState;
import com.example.rankinggame.repositories.AnswerRepository;
import com.example.rankinggame.repositories.PlayerRepository;
import com.example.rankinggame.repositories.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RoundProgressService {
    private final PlayerRepository playerRepository;
    private final AnswerRepository answerRepository;
    private final RoundRepository roundRepository;

    public AnswerSubmissionProgress updateAfterAnswerSubmitted(RoomEntity room, RoundEntity round) {
        long connectedPlayerCount = playerRepository.findByRoomId(room.getId()).stream()
                .filter(player -> player.getConnectionStatus() == PlayerConnectionStatus.CONNECTED)
                .count();
        long submittedAnswerCount = answerRepository.countByRoundId(round.getId());
        boolean allAnswersSubmitted = connectedPlayerCount > 0 && submittedAnswerCount >= connectedPlayerCount;

        if (allAnswersSubmitted) {
            round.setState(RoundState.SORTING);
            roundRepository.save(round);
        }

        return new AnswerSubmissionProgress(submittedAnswerCount, connectedPlayerCount, allAnswersSubmitted);
    }
}
