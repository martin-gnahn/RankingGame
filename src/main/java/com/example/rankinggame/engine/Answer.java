package com.example.rankinggame.engine;

public record Answer(String answerText, int cardValue) {
    private static final int MAX_ANSWER_LENGTH = 500;
    private static final int MIN_CARD_VALUE = 1;
    private static final int MAX_CARD_VALUE = 10;

    public Answer {
        answerText = normalize(answerText);
        if (cardValue < MIN_CARD_VALUE || cardValue > MAX_CARD_VALUE) {
            throw new IllegalArgumentException("Card value must be between 1 and 10");
        }
    }

    public static String normalizeText(String answerText) {
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("Answer text is required");
        }

        String trimmedAnswerText = answerText.trim();
        if (trimmedAnswerText.length() > MAX_ANSWER_LENGTH) {
            throw new IllegalArgumentException("Answer text must be 500 characters or fewer");
        }

        return trimmedAnswerText;
    }

    private static String normalize(String answerText) {
        return normalizeText(answerText);
    }
}
