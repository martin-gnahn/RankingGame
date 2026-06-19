package com.example.rankinggame.engine;

import com.example.rankinggame.entities.RoundState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class Round {
    private RoundState roundState;
    // TODO: move outside
    private PlayerId captainPlayerId;
    // private List<Player> players;
    private Question question;
    private Map<PlayerId, Answer> submittedAnswers = new HashMap<>();

    public static Round start(PlayerId captainPlayerId, Question question) {
        return new Round(captainPlayerId, question);
    }

    public void setPlayerAnswer(PlayerId playerId, String answerText) {
        submittedAnswers.putIfAbsent(playerId, new Answer(answerText));
    }

    private Round(PlayerId captainId, Question question) {
//        this.captain = players.stream().filter(p -> p.playerId() == captainId).findFirst()
//                .orElseThrow(InvalidPlayerException::new);
        this.roundState = RoundState.ANSWER_SUBMISSION;
        this.captainPlayerId = captainId;
        this.question = question;
        // TODO: How to get question id
    }
}
