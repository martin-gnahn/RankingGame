package com.example.rankinggame.controllers;

import com.example.rankinggame.entities.PlayerConnectionStatus;
import com.example.rankinggame.entities.RoomStatus;
import com.example.rankinggame.usecases.CreateRoomCommand;
import com.example.rankinggame.usecases.CreateRoomResult;
import com.example.rankinggame.usecases.CreateRoomService;
import com.example.rankinggame.usecases.GetRoomService;
import com.example.rankinggame.usecases.JoinRoomCommand;
import com.example.rankinggame.usecases.JoinRoomResult;
import com.example.rankinggame.usecases.JoinRoomService;
import com.example.rankinggame.usecases.PlayerDetailsResult;
import com.example.rankinggame.usecases.RoomDetailsResult;
import com.example.rankinggame.usecases.RoomCodeUnavailableException;
import com.example.rankinggame.usecases.RoomNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomControllerTest {
    @Test
    void createsRoomViaApiEndpoint() throws Exception {
        CreateRoomService createRoomService = mock(CreateRoomService.class);
        JoinRoomService joinRoomService = mock(JoinRoomService.class);
        GetRoomService getRoomService = mock(GetRoomService.class);
        UUID roomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(createRoomService.createRoom(any(CreateRoomCommand.class)))
                .thenReturn(new CreateRoomResult("ABCD12", roomId, playerId, "Marta"));
        MockMvc mockMvc = mockMvc(createRoomService, joinRoomService, getRoomService);

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"Marta\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value("ABCD12"))
                .andExpect(jsonPath("$.roomId").value(roomId.toString()))
                .andExpect(jsonPath("$.playerId").value(playerId.toString()))
                .andExpect(jsonPath("$.nickname").value("Marta"))
                .andExpect(jsonPath("$.host").value(true));

        ArgumentCaptor<CreateRoomCommand> commandCaptor = ArgumentCaptor.forClass(CreateRoomCommand.class);
        org.mockito.Mockito.verify(createRoomService).createRoom(commandCaptor.capture());
        assertThat(commandCaptor.getValue().playerName()).isEqualTo("Marta");
    }

    @Test
    void createsRoomWithDocumentedHostNicknameAlias() throws Exception {
        CreateRoomService createRoomService = mock(CreateRoomService.class);
        JoinRoomService joinRoomService = mock(JoinRoomService.class);
        GetRoomService getRoomService = mock(GetRoomService.class);
        when(createRoomService.createRoom(any(CreateRoomCommand.class)))
                .thenReturn(new CreateRoomResult("ABCD12", UUID.randomUUID(), UUID.randomUUID(), "Marta"));
        MockMvc mockMvc = mockMvc(createRoomService, joinRoomService, getRoomService);

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostNickname\":\"Marta\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("Marta"));

        ArgumentCaptor<CreateRoomCommand> commandCaptor = ArgumentCaptor.forClass(CreateRoomCommand.class);
        org.mockito.Mockito.verify(createRoomService).createRoom(commandCaptor.capture());
        assertThat(commandCaptor.getValue().playerName()).isEqualTo("Marta");
    }

    @Test
    void rejectsBlankCreateRoomPlayerName() throws Exception {
        CreateRoomService createRoomService = mock(CreateRoomService.class);
        JoinRoomService joinRoomService = mock(JoinRoomService.class);
        GetRoomService getRoomService = mock(GetRoomService.class);
        MockMvc mockMvc = mockMvc(createRoomService, joinRoomService, getRoomService);

        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerName\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Player name is required"));
    }

    @Test
    void rejectsMalformedCreateRoomBody() throws Exception {
        CreateRoomService createRoomService = mock(CreateRoomService.class);
        JoinRoomService joinRoomService = mock(JoinRoomService.class);
        GetRoomService getRoomService = mock(GetRoomService.class);
        MockMvc mockMvc = mockMvc(createRoomService, joinRoomService, getRoomService);

        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.message").value("Invalid request body"));
    }

    @Test
    void returnsServiceUnavailableWhenRoomCodeCannotBeAllocated() throws Exception {
        CreateRoomService createRoomService = mock(CreateRoomService.class);
        JoinRoomService joinRoomService = mock(JoinRoomService.class);
        GetRoomService getRoomService = mock(GetRoomService.class);
        when(createRoomService.createRoom(any(CreateRoomCommand.class)))
                .thenThrow(new RoomCodeUnavailableException(new RuntimeException("duplicate")));
        MockMvc mockMvc = mockMvc(createRoomService, joinRoomService, getRoomService);

        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerName\":\"Marta\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ROOM_CODE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Unable to allocate a unique room code"));
    }

    @Test
    void joinsRoomViaApiEndpoint() throws Exception {
        CreateRoomService createRoomService = mock(CreateRoomService.class);
        JoinRoomService joinRoomService = mock(JoinRoomService.class);
        GetRoomService getRoomService = mock(GetRoomService.class);
        UUID roomId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(joinRoomService.joinRoom(any(JoinRoomCommand.class)))
                .thenReturn(new JoinRoomResult("ABCD12", roomId, playerId, "Alex"));
        MockMvc mockMvc = mockMvc(createRoomService, joinRoomService, getRoomService);

        mockMvc.perform(post("/api/rooms/ABCD12/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerName\":\"Alex\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value("ABCD12"))
                .andExpect(jsonPath("$.roomId").value(roomId.toString()))
                .andExpect(jsonPath("$.playerId").value(playerId.toString()))
                .andExpect(jsonPath("$.nickname").value("Alex"))
                .andExpect(jsonPath("$.host").value(false));

        ArgumentCaptor<JoinRoomCommand> commandCaptor = ArgumentCaptor.forClass(JoinRoomCommand.class);
        org.mockito.Mockito.verify(joinRoomService).joinRoom(commandCaptor.capture());
        assertThat(commandCaptor.getValue().roomCode()).isEqualTo("ABCD12");
        assertThat(commandCaptor.getValue().playerName()).isEqualTo("Alex");
    }

    @Test
    void joinsRoomWithDocumentedNicknameAlias() throws Exception {
        CreateRoomService createRoomService = mock(CreateRoomService.class);
        JoinRoomService joinRoomService = mock(JoinRoomService.class);
        GetRoomService getRoomService = mock(GetRoomService.class);
        when(joinRoomService.joinRoom(any(JoinRoomCommand.class)))
                .thenReturn(new JoinRoomResult("ABCD12", UUID.randomUUID(), UUID.randomUUID(), "Alex"));
        MockMvc mockMvc = mockMvc(createRoomService, joinRoomService, getRoomService);

        mockMvc.perform(post("/api/rooms/abcd12/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Alex\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("Alex"));

        ArgumentCaptor<JoinRoomCommand> commandCaptor = ArgumentCaptor.forClass(JoinRoomCommand.class);
        org.mockito.Mockito.verify(joinRoomService).joinRoom(commandCaptor.capture());
        assertThat(commandCaptor.getValue().roomCode()).isEqualTo("abcd12");
        assertThat(commandCaptor.getValue().playerName()).isEqualTo("Alex");
    }

    @Test
    void returnsNotFoundWhenJoiningMissingRoom() throws Exception {
        CreateRoomService createRoomService = mock(CreateRoomService.class);
        JoinRoomService joinRoomService = mock(JoinRoomService.class);
        GetRoomService getRoomService = mock(GetRoomService.class);
        when(joinRoomService.joinRoom(any(JoinRoomCommand.class))).thenThrow(new RoomNotFoundException("MISS1"));
        MockMvc mockMvc = mockMvc(createRoomService, joinRoomService, getRoomService);

        mockMvc.perform(post("/api/rooms/MISS1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerName\":\"Alex\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Room not found: MISS1"));
    }

    @Test
    void returnsBadRequestWhenJoiningWithDuplicateName() throws Exception {
        CreateRoomService createRoomService = mock(CreateRoomService.class);
        JoinRoomService joinRoomService = mock(JoinRoomService.class);
        GetRoomService getRoomService = mock(GetRoomService.class);
        when(joinRoomService.joinRoom(any(JoinRoomCommand.class)))
                .thenThrow(new IllegalArgumentException("Player name is already taken"));
        MockMvc mockMvc = mockMvc(createRoomService, joinRoomService, getRoomService);

        mockMvc.perform(post("/api/rooms/ABCD12/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerName\":\"Alex\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Player name is already taken"));
    }

    @Test
    void getsRoomLobbyDataViaApiEndpoint() throws Exception {
        CreateRoomService createRoomService = mock(CreateRoomService.class);
        JoinRoomService joinRoomService = mock(JoinRoomService.class);
        GetRoomService getRoomService = mock(GetRoomService.class);
        UUID roomId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(getRoomService.getRoom("ABCD12")).thenReturn(new RoomDetailsResult(
                roomId,
                "ABCD12",
                RoomStatus.LOBBY,
                List.of(
                        new PlayerDetailsResult(hostId, "Marta", true, PlayerConnectionStatus.CONNECTED),
                        new PlayerDetailsResult(playerId, "Alex", false, PlayerConnectionStatus.CONNECTED)
                )
        ));
        MockMvc mockMvc = mockMvc(createRoomService, joinRoomService, getRoomService);

        mockMvc.perform(get("/api/rooms/ABCD12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(roomId.toString()))
                .andExpect(jsonPath("$.roomCode").value("ABCD12"))
                .andExpect(jsonPath("$.status").value("LOBBY"))
                .andExpect(jsonPath("$.players", hasSize(2)))
                .andExpect(jsonPath("$.players[0].playerId").value(hostId.toString()))
                .andExpect(jsonPath("$.players[0].nickname").value("Marta"))
                .andExpect(jsonPath("$.players[0].host").value(true))
                .andExpect(jsonPath("$.players[0].connectionStatus").value("CONNECTED"))
                .andExpect(jsonPath("$.players[1].playerId").value(playerId.toString()));
    }

    @Test
    void returnsNotFoundForMissingRoom() throws Exception {
        CreateRoomService createRoomService = mock(CreateRoomService.class);
        JoinRoomService joinRoomService = mock(JoinRoomService.class);
        GetRoomService getRoomService = mock(GetRoomService.class);
        when(getRoomService.getRoom("MISS1")).thenThrow(new RoomNotFoundException("MISS1"));
        MockMvc mockMvc = mockMvc(createRoomService, joinRoomService, getRoomService);

        mockMvc.perform(get("/api/rooms/MISS1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Room not found: MISS1"));
    }

    @Test
    void returnsJsonForUnexpectedErrors() throws Exception {
        CreateRoomService createRoomService = mock(CreateRoomService.class);
        JoinRoomService joinRoomService = mock(JoinRoomService.class);
        GetRoomService getRoomService = mock(GetRoomService.class);
        when(getRoomService.getRoom("ABCD12")).thenThrow(new RuntimeException("database unavailable"));
        MockMvc mockMvc = mockMvc(createRoomService, joinRoomService, getRoomService);

        mockMvc.perform(get("/api/rooms/ABCD12"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void keepsMissingResourcesAsNotFoundJson() throws Exception {
        CreateRoomService createRoomService = mock(CreateRoomService.class);
        JoinRoomService joinRoomService = mock(JoinRoomService.class);
        GetRoomService getRoomService = mock(GetRoomService.class);
        MockMvc mockMvc = mockMvc(createRoomService, joinRoomService, getRoomService);

        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    private MockMvc mockMvc(
            CreateRoomService createRoomService,
            JoinRoomService joinRoomService,
            GetRoomService getRoomService
    ) {
        return MockMvcBuilders.standaloneSetup(new RoomController(createRoomService, joinRoomService, getRoomService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
