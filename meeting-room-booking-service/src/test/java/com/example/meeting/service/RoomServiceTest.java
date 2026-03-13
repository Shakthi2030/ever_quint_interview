package com.example.meeting.service;

import com.example.meeting.model.Room;
import com.example.meeting.repository.BookingRepository;
import com.example.meeting.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    public void testCreateRoom() {
        Room room = new Room();
        room.setName("Conference Room");
        room.setCapacity(10);
        room.setFloor(1);

        when(roomRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        Room result = roomService.create(room);

        assertNotNull(result);
        assertEquals("Conference Room", result.getName());
        verify(roomRepository).save(room);
    }

    @Test
    public void testCreateRoomWithDuplicateName() {
        Room room = new Room();
        room.setName("Conference Room");
        room.setCapacity(10);
        room.setFloor(1);

        when(roomRepository.existsByNameIgnoreCase(anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> roomService.create(room));
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    public void testListRooms() {
        Room room1 = new Room();
        room1.setName("Room A");
        room1.setCapacity(5);
        room1.setFloor(1);

        Room room2 = new Room();
        room2.setName("Room B");
        room2.setCapacity(10);
        room2.setFloor(2);

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room1, room2));

        List<Room> result = roomService.list(null, null);

        assertEquals(2, result.size());
        verify(roomRepository).findAll();
    }

    @Test
    public void testListRoomsByMinCapacity() {
        Room room1 = new Room();
        room1.setName("Small Room");
        room1.setCapacity(5);
        room1.setFloor(1);

        Room room2 = new Room();
        room2.setName("Large Room");
        room2.setCapacity(15);
        room2.setFloor(2);

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room1, room2));

        List<Room> result = roomService.list(10, null);

        assertEquals(1, result.size());
        assertEquals("Large Room", result.get(0).getName());
    }

    @Test
    public void testListRoomsByAmenity() {
        Room room1 = new Room();
        room1.setName("Room A");
        room1.setCapacity(5);
        room1.setFloor(1);
        room1.setAmenities(Arrays.asList("Projector"));

        Room room2 = new Room();
        room2.setName("Room B");
        room2.setCapacity(10);
        room2.setFloor(2);
        room2.setAmenities(Arrays.asList("Whiteboard", "Projector"));

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room1, room2));

        List<Room> result = roomService.list(null, "Projector");

        assertEquals(2, result.size());
        verify(roomRepository).findAll();
    }

    @Test
    public void testGetRoom() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Test Room");

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        Room result = roomService.get(1L);

        assertNotNull(result);
        assertEquals("Test Room", result.getName());
    }

    @Test
    public void testGetRoomNotFound() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        Room result = roomService.get(1L);

        assertNull(result);
    }

    @Test
    public void testGetUtilization() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Test Room");

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room));
        when(bookingRepository.findByRoomIdAndStatus(anyLong(), anyString()))
            .thenReturn(Arrays.asList());

        List<Map<String, Object>> result = roomService.getUtilization(
            "2026-03-10T08:00:00", "2026-03-14T20:00:00");

        assertEquals(1, result.size());
        Map<String, Object> utilization = result.get(0);
        assertEquals("1", utilization.get("roomId"));
        assertEquals("Test Room", utilization.get("roomName"));
        assertEquals(0.0, utilization.get("totalBookingHours"));
        assertEquals(0.0, utilization.get("utilizationPercent"));
    }
}
