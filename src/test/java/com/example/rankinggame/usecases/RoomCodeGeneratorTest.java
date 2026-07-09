package com.example.rankinggame.usecases;

import com.example.rankinggame.exceptions.RoomCodeUnavailableException;
import com.example.rankinggame.repositories.RoomRepository;
import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RoomCodeGeneratorTest {
    @Test
    void generatesUppercaseAlphanumericCode() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        when(roomRepository.existsByCode(anyString())).thenReturn(false);
        RoomCodeGenerator generator = new RoomCodeGenerator(roomRepository, new FixedRandomGenerator(0));

        String code = generator.generateUniqueCode();

        assertThat(code).matches("[A-Z0-9]{6}");
        verify(roomRepository).existsByCode(code);
    }

    @Test
    void retriesAfterCollisionUntilUniqueCodeIsGenerated() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        when(roomRepository.existsByCode("AAAAAA")).thenReturn(true);
        when(roomRepository.existsByCode("BBBBBB")).thenReturn(false);
        RoomCodeGenerator generator = new RoomCodeGenerator(
                roomRepository,
                new SequenceRandomGenerator(0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1)
        );

        String code = generator.generateUniqueCode();

        assertThat(code).isEqualTo("BBBBBB");
        verify(roomRepository).existsByCode("AAAAAA");
        verify(roomRepository).existsByCode("BBBBBB");
    }

    @Test
    void failsAfterTooManyCollisions() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        when(roomRepository.existsByCode(anyString())).thenReturn(true);
        RoomCodeGenerator generator = new RoomCodeGenerator(roomRepository, new FixedRandomGenerator(0));

        assertThatThrownBy(generator::generateUniqueCode)
                .isInstanceOf(RoomCodeUnavailableException.class)
                .hasMessage("Unable to allocate a unique room code");
        verify(roomRepository, times(RoomCodeGenerator.MAX_ATTEMPTS)).existsByCode(anyString());
    }

    private static class FixedRandomGenerator implements RandomGenerator {
        private final int value;

        private FixedRandomGenerator(int value) {
            this.value = value;
        }

        @Override
        public long nextLong() {
            return value;
        }

        @Override
        public int nextInt(int bound) {
            return value;
        }
    }

    private static class SequenceRandomGenerator implements RandomGenerator {
        private final int[] values;
        private int index;

        private SequenceRandomGenerator(int... values) {
            this.values = values;
        }

        @Override
        public long nextLong() {
            return nextInt(Integer.MAX_VALUE);
        }

        @Override
        public int nextInt(int bound) {
            return values[index++];
        }
    }
}
