package com.example.rankinggame;

import com.example.rankinggame.engine.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RankingGameEngine {
    private static final int MAX_ANSWER_LENGTH = 500;

    Game startGame(List<Player> players) {
        return new Game(players);
    }

//    Round startNewRound(Game game, PlayerId captainId) {
//        Round round = new Round(captainId);
//        game.getAllRounds().add(round);
//        return round;
//    }

    public void submitAnswer(Game game, PlayerId playerId, String answerText, RoundId roundId) {
        String answerTextTrimmed = normalizeAnswerText(answerText);
        // int cardValue = roundCardAssignmentService.assignedCardValue(room.getId(), round.getId(), player.getId());
        game.getCurrentRound().setPlayerAnswer(playerId, answerTextTrimmed);
    }

    private String normalizeAnswerText(String answerText) {
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("Answer text is required");
        }

        String trimmedAnswerText = answerText.trim();
        if (trimmedAnswerText.length() > MAX_ANSWER_LENGTH) {
            throw new IllegalArgumentException("Answer text must be 500 characters or fewer");
        }

        return trimmedAnswerText;
    }
}
