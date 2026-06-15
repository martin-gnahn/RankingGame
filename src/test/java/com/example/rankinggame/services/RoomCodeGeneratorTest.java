package com.example.rankinggame.services;

import com.example.rankinggame.repositories.RoomRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomCodeGeneratorTest {
    @Test
    void generatesUppercaseAlphanumericCode() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        when(roomRepository.existsByCode(anyString())).thenReturn(false);
        RoomCodeGenerator generator = new RoomCodeGenerator(roomRepository);

        String code = generator.generateUniqueCode();

        assertThat(code).matches("[A-Z0-9]{6}");
        verify(roomRepository).existsByCode(code);
    }

    @Test
    void failsAfterTooManyCollisions() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        when(roomRepository.existsByCode(anyString())).thenReturn(true);
        RoomCodeGenerator generator = new RoomCodeGenerator(roomRepository);

        assertThatThrownBy(generator::generateUniqueCode)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to generate a unique room code");
        verify(roomRepository, times(RoomCodeGenerator.MAX_ATTEMPTS)).existsByCode(anyString());
    }
}
