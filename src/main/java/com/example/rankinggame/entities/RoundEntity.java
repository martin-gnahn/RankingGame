package com.example.rankinggame.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "rounds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_session_id", nullable = false)
    private UUID gameSessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity questionEntity;

    @Column(name = "captain_player_id", nullable = false)
    private UUID captainPlayerId;

    // TODO: Deprecated: remove
    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RoundState state;

    public UUID getQuestionId() {
        return questionEntity == null ? null : questionEntity.getId();
    }

    public void setQuestionId(UUID questionId) {
        QuestionEntity question = new QuestionEntity();
        question.setId(questionId);
        this.questionEntity = question;
    }
}
