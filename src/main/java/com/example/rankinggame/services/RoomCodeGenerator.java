package com.example.rankinggame.services;

import com.example.rankinggame.repositories.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

@Component
public class RoomCodeGenerator {
    static final int CODE_LENGTH = 6;
    static final int MAX_ATTEMPTS = 25;

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final RoomRepository roomRepository;
    private final RandomGenerator randomGenerator;

    @Autowired
    public RoomCodeGenerator(RoomRepository roomRepository) {
        this(roomRepository, new SecureRandom());
    }

    RoomCodeGenerator(RoomRepository roomRepository, RandomGenerator randomGenerator) {
        this.roomRepository = roomRepository;
        this.randomGenerator = randomGenerator;
    }

    public String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = generateCode();

            if (!roomRepository.existsByCode(code)) {
                return code;
            }
        }

        throw new IllegalStateException("Unable to generate a unique room code");
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int index = 0; index < CODE_LENGTH; index++) {
            int randomIndex = randomGenerator.nextInt(ALPHABET.length);
            code.append(ALPHABET[randomIndex]);
        }

        return code.toString();
    }
}
