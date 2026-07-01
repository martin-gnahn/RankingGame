package com.example.rankinggame.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rounds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundEntity {
    @Id
    private UUID id;

    @Column(name = "game_session_id", nullable = false)
    private UUID gameSessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity questionEntity;

    @Column(name = "captain_player_id", nullable = false)
    private UUID captainPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RoundState state;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UUID getQuestionId() {
        return questionEntity == null ? null : questionEntity.getId();
    }

    public void setQuestionId(UUID questionId) {
        QuestionEntity question = new QuestionEntity();
        question.setId(questionId);
        this.questionEntity = question;
    }
}
