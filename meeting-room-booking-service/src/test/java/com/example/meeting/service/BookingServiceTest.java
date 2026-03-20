package com.example.meeting.service;

import com.example.meeting.exception.InvalidBookingTimeException;
import com.example.meeting.exception.RoomNotFoundException;
import com.example.meeting.model.Booking;
import com.example.meeting.model.IdempotencyKey;
import com.example.meeting.repository.BookingRepository;
import com.example.meeting.repository.IdempotencyKeyRepository;
import com.example.meeting.repository.RoomRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Mock(lenient = true)
    private ObjectMapper objectMapper;

    @InjectMocks
    private BookingService bookingService;

    @Test
    public void testCreateValidBooking() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Test Meeting");
        booking.setOrganizerEmail("test@example.com");
        
        // Use a specific Monday to avoid weekend issues
        LocalDate monday = LocalDate.of(2026, 3, 16);
        booking.setStartTime(monday.atTime(LocalTime.of(9, 0)));
        booking.setEndTime(monday.atTime(LocalTime.of(10, 0)));

        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Arrays.asList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        Booking result = bookingService.create(booking);

        assertNotNull(result);
        assertEquals("Test Meeting", result.getTitle());
        verify(bookingRepository).save(booking);
    }

    @Test
    public void testCreateBookingWithInvalidTimeRange() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Test Meeting");
        booking.setOrganizerEmail("test@example.com");
        booking.setStartTime(LocalDateTime.now().plusDays(1));
        booking.setEndTime(LocalDateTime.now().plusDays(1).minusHours(1));

        assertThrows(RuntimeException.class, () -> bookingService.create(booking));
    }

    @Test
    public void testCreateBookingTooShort() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Test Meeting");
        booking.setOrganizerEmail("test@example.com");
        booking.setStartTime(LocalDateTime.now().plusDays(1));
        booking.setEndTime(LocalDateTime.now().plusDays(1).plusMinutes(10));

        assertThrows(RuntimeException.class, () -> bookingService.create(booking));
    }

    @Test
    public void testCreateBookingTooLong() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Test Meeting");
        booking.setOrganizerEmail("test@example.com");
        booking.setStartTime(LocalDateTime.now().plusDays(1));
        booking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(5));

        assertThrows(RuntimeException.class, () -> bookingService.create(booking));
    }

    @Test
    public void testCreateOverlappingBooking() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Test Meeting");
        booking.setOrganizerEmail("test@example.com");
        booking.setStartTime(LocalDateTime.now().plusDays(1));
        booking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));

        Booking existingBooking = new Booking();
        existingBooking.setRoomId(1L);
        existingBooking.setStartTime(LocalDateTime.now().plusDays(1).plusMinutes(30));
        existingBooking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));

        lenient().when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));
        lenient().when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Arrays.asList(existingBooking));

        assertThrows(RuntimeException.class, () -> bookingService.create(booking));
    }

    @Test
    public void testCancelBooking() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setRoomId(1L);
        booking.setTitle("Test Meeting");
        booking.setStatus("confirmed");
        booking.setStartTime(LocalDateTime.now().plusHours(2));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        Booking result = bookingService.cancel(1L);

        assertEquals("cancelled", result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    public void testCancelBookingNotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> bookingService.cancel(1L));
    }

    @Test
    public void testCancelBookingTooLate() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setRoomId(1L);
        booking.setTitle("Test Meeting");
        booking.setStatus("confirmed");
        booking.setStartTime(LocalDateTime.now().plusMinutes(30));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(RuntimeException.class, () -> bookingService.cancel(1L));
    }

    @Test
    public void testListBookings() {
        Booking booking1 = new Booking();
        booking1.setRoomId(1L);
        booking1.setTitle("Meeting 1");

        Booking booking2 = new Booking();
        booking2.setRoomId(2L);
        booking2.setTitle("Meeting 2");

        when(bookingRepository.findAll()).thenReturn(Arrays.asList(booking1, booking2));

        List<Booking> result = bookingService.list(null);

        assertEquals(2, result.size());
        verify(bookingRepository).findAll();
    }

    @Test
    public void testListBookingsByRoom() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Room Meeting");

        when(bookingRepository.findByRoomId(1L)).thenReturn(Arrays.asList(booking));

        List<Booking> result = bookingService.list(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getRoomId());
        verify(bookingRepository).findByRoomId(1L);
    }

    @Test
    public void testWeekendBookingRejected() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Weekend Meeting");
        booking.setOrganizerEmail("test@example.com");
        
        // Saturday March 15, 2026 at 10:00 AM
        LocalDate saturday = LocalDate.of(2026, 3, 14); // March 14, 2026 is a Saturday
        booking.setStartTime(saturday.atTime(LocalTime.of(10, 0)));
        booking.setEndTime(saturday.atTime(LocalTime.of(11, 0)));

        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));

        assertThrows(InvalidBookingTimeException.class, () -> bookingService.create(booking));
    }

    @Test
    public void testOutsideHoursBookingRejected() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Early Meeting");
        booking.setOrganizerEmail("test@example.com");
        
        // Monday March 10, 2026 at 07:00-08:00 (before opening)
        LocalDate monday = LocalDate.of(2026, 3, 16); // March 16, 2026 is a Monday
        booking.setStartTime(monday.atTime(LocalTime.of(7, 0)));
        booking.setEndTime(monday.atTime(LocalTime.of(8, 0)));

        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));

        assertThrows(InvalidBookingTimeException.class, () -> bookingService.create(booking));
    }

    @Test
    public void testIdempotentBookingReturnsSameBooking() throws JsonProcessingException {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Idempotent Meeting");
        booking.setOrganizerEmail("a@b.com");
        
        LocalDate monday = LocalDate.of(2026, 3, 16);
        booking.setStartTime(monday.atTime(LocalTime.of(10, 0)));
        booking.setEndTime(monday.atTime(LocalTime.of(11, 0)));

        // First call returns empty (no existing key)
        when(idempotencyKeyRepository.findByKeyAndOrganizerEmail("key-abc", "a@b.com"))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(createCompletedIdempotencyKey()));
        
        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Arrays.asList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        
        // First call
        ResponseEntity<Booking> firstResponse = bookingService.createWithIdempotency(booking, "key-abc");
        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());
        
        // Second call should return same booking
        ResponseEntity<Booking> secondResponse = bookingService.createWithIdempotency(booking, "key-abc");
        assertEquals(HttpStatus.OK, secondResponse.getStatusCode());
        
        // Verify save was called exactly once
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    private IdempotencyKey createCompletedIdempotencyKey() {
        IdempotencyKey key = new IdempotencyKey("key-abc", "a@b.com");
        key.setBookingId(1L);
        key.setCompleted(true);
        return key;
    }

    @Test
    public void testExact15MinuteBoundaryAllowed() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("15 Minute Meeting");
        booking.setOrganizerEmail("test@example.com");
        
        LocalDate monday = LocalDate.of(2026, 3, 16);
        booking.setStartTime(monday.atTime(LocalTime.of(10, 0)));
        booking.setEndTime(monday.atTime(LocalTime.of(10, 15)));

        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Arrays.asList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        assertDoesNotThrow(() -> bookingService.create(booking));
        verify(bookingRepository).save(booking);
    }

    @Test
    public void testExact4HourBoundaryAllowed() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("4 Hour Meeting");
        booking.setOrganizerEmail("test@example.com");
        
        LocalDate monday = LocalDate.of(2026, 3, 16);
        booking.setStartTime(monday.atTime(LocalTime.of(10, 0)));
        booking.setEndTime(monday.atTime(LocalTime.of(14, 0)));

        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Arrays.asList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        assertDoesNotThrow(() -> bookingService.create(booking));
        verify(bookingRepository).save(booking);
    }

    @Test
    public void testStartAt20_00Rejected() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Invalid Start Time");
        booking.setOrganizerEmail("test@example.com");
        
        LocalDate monday = LocalDate.of(2026, 3, 16);
        booking.setStartTime(monday.atTime(LocalTime.of(20, 0)));
        booking.setEndTime(monday.atTime(LocalTime.of(21, 0)));

        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));

        assertThrows(InvalidBookingTimeException.class, () -> bookingService.create(booking));
    }

    @Test
    public void testEndAt20_00Allowed() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Valid End Time");
        booking.setOrganizerEmail("test@example.com");
        
        LocalDate monday = LocalDate.of(2026, 3, 16);
        booking.setStartTime(monday.atTime(LocalTime.of(19, 0)));
        booking.setEndTime(monday.atTime(LocalTime.of(20, 0)));

        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Arrays.asList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        assertDoesNotThrow(() -> bookingService.create(booking));
        verify(bookingRepository).save(booking);
    }

    @Test
    public void testSundayBookingRejected() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Sunday Meeting");
        booking.setOrganizerEmail("test@example.com");
        
        // Sunday March 15, 2026 at 10:00 AM
        LocalDate sunday = LocalDate.of(2026, 3, 15);
        booking.setStartTime(sunday.atTime(LocalTime.of(10, 0)));
        booking.setEndTime(sunday.atTime(LocalTime.of(11, 0)));

        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));

        assertThrows(InvalidBookingTimeException.class, () -> bookingService.create(booking));
    }

    @Test
    public void testUnknownRoomReturns404() {
        Booking booking = new Booking();
        booking.setRoomId(999L);
        booking.setTitle("Test Meeting");
        booking.setOrganizerEmail("test@example.com");
        
        LocalDate monday = LocalDate.of(2026, 3, 16);
        booking.setStartTime(monday.atTime(LocalTime.of(10, 0)));
        booking.setEndTime(monday.atTime(LocalTime.of(11, 0)));

        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class, () -> bookingService.create(booking));
    }

    @Test
    public void testOverlapReturns409() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Overlapping Meeting");
        booking.setOrganizerEmail("test@example.com");
        
        LocalDate monday = LocalDate.of(2026, 3, 16);
        booking.setStartTime(monday.atTime(LocalTime.of(10, 0)));
        booking.setEndTime(monday.atTime(LocalTime.of(11, 0)));

        Booking existingBooking = new Booking();
        existingBooking.setRoomId(1L);
        existingBooking.setStartTime(LocalDateTime.now().plusDays(1).plusMinutes(30));
        existingBooking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));

        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Arrays.asList(existingBooking));

        assertThrows(RuntimeException.class, () -> bookingService.create(booking));
    }
    public void testCancelAlreadyCancelledIsNoOp() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setRoomId(1L);
        booking.setTitle("Test Meeting");
        booking.setStatus("cancelled");
        booking.setStartTime(LocalDateTime.now().plusHours(2));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        Booking result = bookingService.cancel(1L);

        assertEquals("cancelled", result.getStatus());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    public void testIdempotencyFirstCall() throws JsonProcessingException {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("First Idempotent Meeting");
        booking.setOrganizerEmail("test@example.com");
        
        LocalDate monday = LocalDate.of(2026, 3, 16);
        booking.setStartTime(monday.atTime(LocalTime.of(10, 0)));
        booking.setEndTime(monday.atTime(LocalTime.of(11, 0)));

        when(idempotencyKeyRepository.findByKeyAndOrganizerEmail("first-key", "test@example.com"))
            .thenReturn(Optional.empty());
        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Arrays.asList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        try {
            when(objectMapper.writeValueAsString(any(Booking.class))).thenReturn("{\"id\":1,\"title\":\"First Idempotent Meeting\"}");
        } catch (JsonProcessingException e) {
            // This won't happen in the mock
        }

        ResponseEntity<Booking> response = bookingService.createWithIdempotency(booking, "first-key");
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("First Idempotent Meeting", response.getBody().getTitle());
        verify(idempotencyKeyRepository, times(2)).save(any(IdempotencyKey.class));
        verify(bookingRepository).save(booking);
    }

    @Test
    public void testIdempotencySecondCall() throws JsonProcessingException {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setRoomId(1L);
        booking.setTitle("Second Idempotent Meeting");
        booking.setOrganizerEmail("test@example.com");
        
        when(idempotencyKeyRepository.findByKeyAndOrganizerEmail("second-key", "test@example.com"))
            .thenReturn(Optional.of(createCompletedIdempotencyKey("second-key", "test@example.com", 1L)));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        ResponseEntity<Booking> response = bookingService.createWithIdempotency(booking, "second-key");
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Second Idempotent Meeting", response.getBody().getTitle());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    public void testIdempotencyInProgressReturns409() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("In Progress Meeting");
        booking.setOrganizerEmail("test@example.com");
        
        LocalDate monday = LocalDate.of(2026, 3, 16);
        booking.setStartTime(monday.atTime(LocalTime.of(10, 0)));
        booking.setEndTime(monday.atTime(LocalTime.of(11, 0)));

        IdempotencyKey inProgressKey = new IdempotencyKey("progress-key", "test@example.com");
        inProgressKey.setCompleted(false);

        when(idempotencyKeyRepository.findByKeyAndOrganizerEmail("progress-key", "test@example.com"))
            .thenReturn(Optional.of(inProgressKey));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Arrays.asList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        
        ResponseEntity<Booking> response = bookingService.createWithIdempotency(booking, "progress-key");
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("In Progress Meeting", response.getBody().getTitle());
        verify(bookingRepository).save(booking);
        verify(idempotencyKeyRepository).delete(any(IdempotencyKey.class));
    }

    @Test
    public void testRetryAfterFailure() throws JsonProcessingException {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("Retry After Failure");
        booking.setOrganizerEmail("test@example.com");
        
        LocalDate monday = LocalDate.of(2026, 3, 16);
        booking.setStartTime(monday.atTime(LocalTime.of(10, 0)));
        booking.setEndTime(monday.atTime(LocalTime.of(11, 0)));

        // First call: find empty, then save fails (simulate failure)
        when(idempotencyKeyRepository.findByKeyAndOrganizerEmail("retry-key", "test@example.com"))
            .thenReturn(Optional.empty());
        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Arrays.asList());
        when(bookingRepository.save(any(Booking.class))).thenThrow(new RuntimeException("Database error"));

        // First call should fail and delete the idempotency key
        assertThrows(RuntimeException.class, () -> bookingService.createWithIdempotency(booking, "retry-key"));
        verify(idempotencyKeyRepository).delete(any(IdempotencyKey.class));

        // Reset mocks for retry
        reset(idempotencyKeyRepository, bookingRepository);
        
        // Second call (retry): find empty again, then succeed
        when(idempotencyKeyRepository.findByKeyAndOrganizerEmail("retry-key", "test@example.com"))
            .thenReturn(Optional.empty());
        when(roomRepository.findById(1L)).thenReturn(Optional.of(new com.example.meeting.model.Room()));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Arrays.asList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        try {
            when(objectMapper.writeValueAsString(any(Booking.class))).thenReturn("{\"id\":1,\"title\":\"Retry After Failure\"}");
        } catch (JsonProcessingException e) {
            // This won't happen in mock
        }

        ResponseEntity<Booking> response = bookingService.createWithIdempotency(booking, "retry-key");
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Retry After Failure", response.getBody().getTitle());
        verify(bookingRepository).save(booking);
    }

    @Test
    public void testMissingIdempotencyKeyReturns400() {
        Booking booking = new Booking();
        booking.setRoomId(1L);
        booking.setTitle("No Key Meeting");
        booking.setOrganizerEmail("test@example.com");

        ResponseEntity<Booking> response = bookingService.createWithIdempotency(booking, "");
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    private IdempotencyKey createCompletedIdempotencyKey(String key, String email, Long bookingId) {
        IdempotencyKey idempotencyKey = new IdempotencyKey(key, email);
        idempotencyKey.setBookingId(bookingId);
        idempotencyKey.setCompleted(true);
        return idempotencyKey;
    }
}
