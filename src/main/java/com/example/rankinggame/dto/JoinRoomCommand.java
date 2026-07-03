package com.example.rankinggame.dto;

public record JoinRoomCommand(String roomCode, String playerName) implements RoomCommand {
}
