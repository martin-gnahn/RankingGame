package com.example.rankinggame.engine;

import com.example.rankinggame.entities.Question;
import com.example.rankinggame.entities.RoundState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class Round {
    private RoundState roundState;
    // TODO: move outside
    private PlayerId captainPlayerId;
    // private List<Player> players;
    private QuestionId questionId;
    private Map<PlayerId, Answer> submittedAnswers = new HashMap<>();

    public static Round start(PlayerId captainPlayerId) {
        Round round = new Round(captainPlayerId);
        round.roundState = RoundState.ANSWER_SUBMISSION;
        return round;
    }

    public void setPlayerAnswer(PlayerId playerId, String answerText) {
        submittedAnswers.putIfAbsent(playerId, new Answer(answerText));
    }

    private Round(PlayerId captainId) {
//        this.captain = players.stream().filter(p -> p.playerId() == captainId).findFirst()
//                .orElseThrow(InvalidPlayerException::new);
        this.roundState = RoundState.ANSWER_SUBMISSION;
        this.captainPlayerId = captainId;
        // TODO: How to get question id
    }
}
