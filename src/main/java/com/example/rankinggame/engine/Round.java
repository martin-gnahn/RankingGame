package com.example.rankinggame.engine;

import com.example.rankinggame.engine.exceptions.AnswerAlreadySubmittedException;
import com.example.rankinggame.engine.exceptions.AnswersNotAcceptedException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class Round {
    private RoundStatus roundStatus;
    private GameParticipant captain;
    private Question question;
    @Builder.Default
    private Map<PlayerId, Answer> submittedAnswers = new HashMap<>();

    public static Round start(GameParticipant captain, Question question) {
        return new Round(captain, question);
    }

    public void requireAcceptingAnswers() {
        if (roundStatus != RoundStatus.QUESTION_REVEALED) {
            throw new AnswersNotAcceptedException();
        }
    }

    public Answer submitAnswer(PlayerId playerId, String answerText, int cardValue) {
        requireAcceptingAnswers();
        if (submittedAnswers.containsKey(playerId)) {
            throw new AnswerAlreadySubmittedException();
        }

        Answer answer = new Answer(answerText, cardValue);
        submittedAnswers.put(playerId, answer);
        return answer;
    }

    private Round(GameParticipant captain, Question question) {
//        this.captain = players.stream().filter(p -> p.playerId() == captainId).findFirst()
//                .orElseThrow(InvalidPlayerException::new);
        this.roundStatus = RoundStatus.QUESTION_REVEALED;
        this.captain = captain;
        this.question = question;
        this.submittedAnswers = new HashMap<>();
        // TODO: How to get question id
    }
}
