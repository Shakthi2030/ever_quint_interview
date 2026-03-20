package com.example.meeting.controller;

import com.example.meeting.dto.BookingListResponse;
import com.example.meeting.model.Booking;
import com.example.meeting.service.BookingService;
import com.example.meeting.exception.RoomNotFoundException;
import com.example.meeting.exception.BookingOverlapException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
public class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateBooking() throws Exception {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Team Meeting");
        booking.setOrganizerEmail("team@example.com");
        booking.setStartTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0));
        booking.setEndTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0));
        booking.setStatus("confirmed");

        when(bookingService.create(any(Booking.class))).thenReturn(booking);

        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Team Meeting"))
                .andExpect(jsonPath("$.organizerEmail").value("team@example.com"));
    }

    @Test
    public void testListBookings() throws Exception {
        Booking booking1 = new Booking();
        booking1.setRoomId(1L);
        booking1.setTitle("Meeting 1");
        booking1.setOrganizerEmail("user1@example.com");

        Booking booking2 = new Booking();
        booking2.setRoomId(2L);
        booking2.setTitle("Meeting 2");
        booking2.setOrganizerEmail("user2@example.com");

        BookingListResponse response = new BookingListResponse(Arrays.asList(booking1, booking2), 2, 50, 0);

        when(bookingService.listWithFilters(null, null, null, null, null)).thenReturn(response);

        mockMvc.perform(get("/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.limit").value(50))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.items[0].title").value("Meeting 1"))
                .andExpect(jsonPath("$.items[1].title").value("Meeting 2"));
    }

    @Test
    public void testListBookingsByRoom() throws Exception {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Room Meeting");
        booking.setOrganizerEmail("user@example.com");

        BookingListResponse response = new BookingListResponse(Arrays.asList(booking), 1, 50, 0);

        when(bookingService.listWithFilters(1L, null, null, null, null)).thenReturn(response);

        mockMvc.perform(get("/bookings?roomId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Room Meeting"));
    }

    @Test
    public void testCancelBooking() throws Exception {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setRoomId(1L);
        booking.setTitle("Meeting to Cancel");
        booking.setStatus("cancelled");

        when(bookingService.cancel(1L)).thenReturn(booking);

        mockMvc.perform(post("/bookings/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"));
    }

    @Test
    public void testCreateBookingWithInvalidData() throws Exception {
        Booking booking = new Booking();
        booking.setRoomId(null);
        booking.setTitle("");
        booking.setOrganizerEmail("invalid-email");

        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testConflictReturns409() throws Exception {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Overlapping Meeting");
        booking.setOrganizerEmail("test@example.com");
        booking.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0));
        booking.setEndTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0));

        when(bookingService.create(any(Booking.class))).thenThrow(new BookingOverlapException("Room is already booked"));

        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testUnknownRoomReturns404() throws Exception {
        Booking booking = new Booking();
        booking.setRoomId(999L);
        booking.setTitle("Meeting in Unknown Room");
        booking.setOrganizerEmail("test@example.com");
        booking.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0));
        booking.setEndTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0));

        when(bookingService.create(any(Booking.class))).thenThrow(new RoomNotFoundException("Room not found"));

        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testIdempotencyKeyReturns200OnRepeat() throws Exception {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Idempotent Meeting");
        booking.setOrganizerEmail("test@example.com");
        booking.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0));
        booking.setEndTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0));
        booking.setId(1L);

        when(bookingService.createWithIdempotency(any(Booking.class), any()))
            .thenReturn(ResponseEntity.ok(booking));

        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "test-key-123")
                .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Idempotent Meeting"));
    }

    @Test
    public void testAllErrorResponsesIncludeErrorField() throws Exception {
        // Test 409 Conflict error includes error field
        Booking overlappingBooking = new Booking();
        overlappingBooking.setRoomId(1L);
        overlappingBooking.setTitle("Overlapping Meeting");
        overlappingBooking.setOrganizerEmail("test@example.com");
        overlappingBooking.setStartTime(LocalDateTime.now().plusDays(1));
        overlappingBooking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));

        when(bookingService.create(any(Booking.class))).thenThrow(new BookingOverlapException("Room is already booked"));

        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(overlappingBooking)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        // Test 404 Not Found error includes error field
        Booking unknownRoomBooking = new Booking();
        unknownRoomBooking.setRoomId(999L);
        unknownRoomBooking.setTitle("Unknown Room Meeting");
        unknownRoomBooking.setOrganizerEmail("test@example.com");
        unknownRoomBooking.setStartTime(LocalDateTime.now().plusDays(1));
        unknownRoomBooking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));

        when(bookingService.create(any(Booking.class))).thenThrow(new RoomNotFoundException("Room not found"));

        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unknownRoomBooking)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        // Test 400 Bad Request error includes error field
        Booking invalidBooking = new Booking();
        invalidBooking.setRoomId(null);
        invalidBooking.setTitle("");
        invalidBooking.setOrganizerEmail("invalid-email");

        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidBooking)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
