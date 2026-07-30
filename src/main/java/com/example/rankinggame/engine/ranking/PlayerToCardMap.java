package com.example.rankinggame.engine.ranking;

import com.example.rankinggame.engine.CardNumber;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.exceptions.DuplicateCardValueInfoForPlayerException;
import com.example.rankinggame.engine.exceptions.MissingCardValueForRankedAnswerException;
import com.example.rankinggame.engine.exceptions.RankingAssessmentInput;
import com.example.rankinggame.engine.exceptions.RankingAssessmentInputRequiredException;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
class PlayerToCardMap {
    private Map<PlayerId, CardNumber> cardsByPlayerId;

    static PlayerToCardMap from(List<CardValueInfo> cardValueInfos) {
        Map<PlayerId, CardNumber> cardsByPlayerId = new HashMap<>();
        for (CardValueInfo cardValueInfo : cardValueInfos) {
            if (cardValueInfo == null) {
                throw new RankingAssessmentInputRequiredException(RankingAssessmentInput.CARD_VALUE_INFO_ENTRIES);
            }
            CardNumber existingCard = cardsByPlayerId.putIfAbsent(cardValueInfo.playerId(), cardValueInfo.cardValue());
            if (existingCard != null) {
                throw new DuplicateCardValueInfoForPlayerException(cardValueInfo.playerId());
            }
        }
        return new PlayerToCardMap(cardsByPlayerId);
    }

    CardNumber cardFor(PlayerId playerId) {
        CardNumber cardNumber = cardsByPlayerId.get(playerId);
        if (cardNumber == null) {
            throw new MissingCardValueForRankedAnswerException(playerId);
        }
        return cardNumber;
    }

    public Set<PlayerId> expectedPlayerIds() {
        return cardsByPlayerId.keySet();
    }
}
