package com.example.rankinggame.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "ranking_entries",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"roundId", "position"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankingEntity {
    @Id
    private UUID id;

    @OneToOne
    private AnswerEntity answer;

    @Column(name = "round_id", nullable = false)
    private UUID roundId;

    @Column(name = "position", nullable = false)
    private Integer position;
}
