package com.example.rankinggame.engine.ranking;

import com.example.rankinggame.engine.CardNumber;
import com.example.rankinggame.engine.PlayerId;
import com.example.rankinggame.engine.exceptions.DuplicateCardValueInfoForCardNumberException;
import com.example.rankinggame.engine.exceptions.DuplicateCardValueInfoForPlayerException;
import com.example.rankinggame.engine.exceptions.MissingCardValueForRankedAnswerException;
import com.example.rankinggame.engine.exceptions.RankingAssessmentInput;
import com.example.rankinggame.engine.exceptions.RankingAssessmentInputRequiredException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class PlayerToCardMap {
    private final Map<PlayerId, CardNumber> cardsByPlayerId;

    private PlayerToCardMap(Map<PlayerId, CardNumber> cardsByPlayerId) {
        this.cardsByPlayerId = Map.copyOf(cardsByPlayerId);
    }

    static PlayerToCardMap from(List<CardValueInfo> cardValueInfos) {
        Map<PlayerId, CardNumber> cardsByPlayerId = new HashMap<>();
        Set<CardNumber> assignedCardNumbers = new HashSet<>();
        for (CardValueInfo cardValueInfo : cardValueInfos) {
            if (cardValueInfo == null) {
                throw new RankingAssessmentInputRequiredException(RankingAssessmentInput.CARD_VALUE_INFO_ENTRIES);
            }
            CardNumber existingCard = cardsByPlayerId.putIfAbsent(cardValueInfo.playerId(), cardValueInfo.cardValue());
            if (existingCard != null) {
                throw new DuplicateCardValueInfoForPlayerException(cardValueInfo.playerId());
            }
            boolean wasAdded = assignedCardNumbers.add(cardValueInfo.cardValue());
            if (!wasAdded) {
                throw new DuplicateCardValueInfoForCardNumberException(cardValueInfo.cardValue());
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

    Set<PlayerId> expectedPlayerIds() {
        return cardsByPlayerId.keySet();
    }
}
