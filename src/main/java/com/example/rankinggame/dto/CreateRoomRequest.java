package com.example.rankinggame.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @JsonAlias({"hostNickname", "nickname"})
        @NotBlank(message = "Player name is required")
        @Size(max = 80, message = "Player name must be 80 characters or fewer")
        String playerName
) {
}
