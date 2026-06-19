package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.Player;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.Round;
import com.example.rankinggame.entities.PlayerEntity;
import com.example.rankinggame.entities.RoundEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class RoundMapper {
    public Round toDomain(RoundEntity roundEntity) {
        return Round.builder()
                .submittedAnswers(new HashMap<>())
                .roundState(roundEntity.getState())
                .captainPlayerId(new PlayerId(roundEntity.getCaptainPlayerId()))
                .build();
    }
}
