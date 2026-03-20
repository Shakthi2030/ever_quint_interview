package com.example.meeting.service;

import com.example.meeting.model.Room;
import com.example.meeting.model.Booking;
import com.example.meeting.repository.BookingRepository;
import com.example.meeting.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @Test
    public void testBookingStartsBeforeFromClipped() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Test Room");

        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setStartTime(LocalDateTime.of(2026, 3, 16, 7, 0)); // Before from time
        booking.setEndTime(LocalDateTime.of(2026, 3, 16, 10, 0)); // After from time

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room));
        when(bookingRepository.findByRoomIdAndStatus(1L, "confirmed")).thenReturn(Arrays.asList(booking));

        List<Map<String, Object>> result = roomService.getUtilization(
            "2026-03-16T08:00:00", "2026-03-16T20:00:00");

        assertEquals(1, result.size());
        Map<String, Object> utilization = result.get(0);
        assertEquals(2.0, utilization.get("totalBookingHours")); // Only 08:00-10:00 counted
    }

    @Test
    public void testBookingEndsAfterToClipped() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Test Room");

        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setStartTime(LocalDateTime.of(2026, 3, 16, 18, 0));
        booking.setEndTime(LocalDateTime.of(2026, 3, 16, 21, 0)); // After to time

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room));
        when(bookingRepository.findByRoomIdAndStatus(1L, "confirmed")).thenReturn(Arrays.asList(booking));

        List<Map<String, Object>> result = roomService.getUtilization(
            "2026-03-16T08:00:00", "2026-03-16T20:00:00");

        assertEquals(1, result.size());
        Map<String, Object> utilization = result.get(0);
        assertEquals(2.0, utilization.get("totalBookingHours")); // Only 18:00-20:00 counted
    }

    @Test
    public void testBookingEntirelyOutsideRangeZero() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Test Room");

        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setStartTime(LocalDateTime.of(2026, 3, 15, 10, 0)); // Previous day
        booking.setEndTime(LocalDateTime.of(2026, 3, 15, 11, 0));

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room));
        when(bookingRepository.findByRoomIdAndStatus(1L, "confirmed")).thenReturn(Arrays.asList(booking));

        List<Map<String, Object>> result = roomService.getUtilization(
            "2026-03-16T08:00:00", "2026-03-16T20:00:00");

        assertEquals(1, result.size());
        Map<String, Object> utilization = result.get(0);
        assertEquals(0.0, utilization.get("totalBookingHours")); // No overlap
    }

    @Test
    public void testBookingStraddlingBusinessHoursBoundaryOnlyBusinessPortionCounted() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Test Room");

        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setStartTime(LocalDateTime.of(2026, 3, 16, 7, 0)); // Before business hours
        booking.setEndTime(LocalDateTime.of(2026, 3, 16, 21, 0)); // After business hours

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room));
        when(bookingRepository.findByRoomIdAndStatus(1L, "confirmed")).thenReturn(Arrays.asList(booking));

        List<Map<String, Object>> result = roomService.getUtilization(
            "2026-03-16T00:00:00", "2026-03-16T23:59:59");

        assertEquals(1, result.size());
        Map<String, Object> utilization = result.get(0);
        assertEquals(12.0, utilization.get("totalBookingHours")); // Only 08:00-20:00 counted
    }

    @Test
    public void testMultipleBookingsAcrossDays() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Test Room");

        Booking booking1 = new Booking();
        booking1.setRoomId(1L);
        booking1.setStartTime(LocalDateTime.of(2026, 3, 16, 10, 0));
        booking1.setEndTime(LocalDateTime.of(2026, 3, 16, 11, 0));

        Booking booking2 = new Booking();
        booking2.setRoomId(1L);
        booking2.setStartTime(LocalDateTime.of(2026, 3, 17, 14, 0));
        booking2.setEndTime(LocalDateTime.of(2026, 3, 17, 16, 0));

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room));
        when(bookingRepository.findByRoomIdAndStatus(1L, "confirmed")).thenReturn(Arrays.asList(booking1, booking2));

        List<Map<String, Object>> result = roomService.getUtilization(
            "2026-03-16T00:00:00", "2026-03-17T23:59:59");

        assertEquals(1, result.size());
        Map<String, Object> utilization = result.get(0);
        assertEquals(3.0, utilization.get("totalBookingHours")); // 1 hour + 2 hours
    }

    @Test
    public void testResponseShapeValidation() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Test Room");

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room));
        when(bookingRepository.findByRoomIdAndStatus(1L, "confirmed")).thenReturn(Arrays.asList());

        List<Map<String, Object>> result = roomService.getUtilization(
            "2026-03-10T08:00:00", "2026-03-14T20:00:00");

        assertEquals(1, result.size());
        Map<String, Object> utilization = result.get(0);
        
        // Verify response shape
        assertTrue(utilization.containsKey("roomId"));
        assertTrue(utilization.containsKey("roomName"));
        assertTrue(utilization.containsKey("totalBookingHours"));
        assertTrue(utilization.containsKey("utilizationPercent"));
        
        // Verify data types
        assertInstanceOf(String.class, utilization.get("roomId"));
        assertInstanceOf(String.class, utilization.get("roomName"));
        assertInstanceOf(Double.class, utilization.get("totalBookingHours"));
        assertInstanceOf(Double.class, utilization.get("utilizationPercent"));
        
        // Verify specific values
        assertEquals("1", utilization.get("roomId"));
        assertEquals("Test Room", utilization.get("roomName"));
    }
}
