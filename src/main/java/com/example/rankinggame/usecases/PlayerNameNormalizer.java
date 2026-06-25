package com.example.rankinggame.usecases;

final class PlayerNameNormalizer {
    private static final int MAX_PLAYER_NAME_LENGTH = 80;

    private PlayerNameNormalizer() {
    }

    static String normalize(String playerName) {
        if (playerName == null) {
            throw new PlayerNameRequiredException();
        }

        String normalizedPlayerName = playerName.trim();

        if (normalizedPlayerName.isBlank()) {
            throw new PlayerNameRequiredException();
        }

        if (normalizedPlayerName.length() > MAX_PLAYER_NAME_LENGTH) {
            throw new PlayerNameTooLongException(MAX_PLAYER_NAME_LENGTH);
        }

        return normalizedPlayerName;
    }
}
