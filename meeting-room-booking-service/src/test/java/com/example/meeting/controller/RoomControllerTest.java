package com.example.meeting.controller;

import com.example.meeting.model.Room;
import com.example.meeting.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
public class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomService roomService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateRoom() throws Exception {
        Room room = new Room();
        room.setName("Conference Room");
        room.setCapacity(10);
        room.setFloor(1);
        room.setAmenities(Arrays.asList("Projector", "Whiteboard"));

        when(roomService.create(any(Room.class))).thenReturn(room);

        mockMvc.perform(post("/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Conference Room"))
                .andExpect(jsonPath("$.capacity").value(10));
    }

    @Test
    public void testListRooms() throws Exception {
        Room room1 = new Room();
        room1.setName("Room A");
        room1.setCapacity(5);
        room1.setFloor(1);

        Room room2 = new Room();
        room2.setName("Room B");
        room2.setCapacity(10);
        room2.setFloor(2);

        List<Room> rooms = Arrays.asList(room1, room2);

        when(roomService.list(null, null)).thenReturn(rooms);

        mockMvc.perform(get("/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Room A"))
                .andExpect(jsonPath("$[1].name").value("Room B"));
    }

    @Test
    public void testListRoomsByCapacity() throws Exception {
        Room room = new Room();
        room.setName("Large Room");
        room.setCapacity(20);
        room.setFloor(1);

        when(roomService.list(10, null)).thenReturn(Arrays.asList(room));

        mockMvc.perform(get("/rooms?minCapacity=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].capacity").value(20));
    }

    @Test
    public void testCreateRoomWithInvalidData() throws Exception {
        Room room = new Room();
        room.setName("");
        room.setCapacity(-1);
        room.setFloor(-1);

        mockMvc.perform(post("/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isBadRequest());
    }
}
