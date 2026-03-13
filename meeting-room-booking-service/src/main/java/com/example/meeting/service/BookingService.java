
package com.example.meeting.service;

import com.example.meeting.dto.BookingListResponse;
import com.example.meeting.exception.BookingNotFoundException;
import com.example.meeting.exception.BookingOverlapException;
import com.example.meeting.exception.CancellationNotAllowedException;
import com.example.meeting.exception.InvalidBookingTimeException;
import com.example.meeting.exception.RoomNotFoundException;
import com.example.meeting.model.Booking;
import com.example.meeting.model.IdempotencyKey;
import com.example.meeting.repository.BookingRepository;
import com.example.meeting.repository.IdempotencyKeyRepository;
import com.example.meeting.repository.RoomRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    public Booking create(Booking booking){

        if(!booking.getStartTime().isBefore(booking.getEndTime())){
            throw new InvalidBookingTimeException("Start time must be before end time");
        }

        long minutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();

        if(minutes < 15 || minutes > 240){
            throw new InvalidBookingTimeException("Booking duration must be between 15 minutes and 4 hours");
        }

        roomRepository.findById(booking.getRoomId())
            .orElseThrow(() -> new RoomNotFoundException(
                "Room not found with id: " + booking.getRoomId()));

        // Validate booking time is Monday-Friday, 08:00-20:00
        validateBookingTimeRestrictions(booking.getStartTime(), booking.getEndTime());

        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                booking.getRoomId(), booking.getStartTime(), booking.getEndTime());

        if(!overlappingBookings.isEmpty()){
            throw new BookingOverlapException("Room is already booked for the requested time slot");
        }

        booking.setStatus("confirmed");
        return bookingRepository.save(booking);
    }

    private void validateBookingTimeRestrictions(LocalDateTime startTime, LocalDateTime endTime) {
        // Check if booking is on Monday-Friday
        DayOfWeek startDay = startTime.getDayOfWeek();
        DayOfWeek endDay = endTime.getDayOfWeek();
        
        if (startDay == DayOfWeek.SATURDAY || startDay == DayOfWeek.SUNDAY ||
            endDay == DayOfWeek.SATURDAY || endDay == DayOfWeek.SUNDAY) {
            throw new InvalidBookingTimeException("Bookings are only allowed Monday-Friday");
        }

        // Check if booking time is between 08:00-20:00
        LocalTime bookingStartTime = startTime.toLocalTime();
        LocalTime bookingEndTime = endTime.toLocalTime();
        LocalTime allowedStart = LocalTime.of(8, 0);
        LocalTime allowedEnd = LocalTime.of(20, 0);

        if (bookingStartTime.isBefore(allowedStart) || bookingStartTime.isAfter(allowedEnd) ||
            bookingEndTime.isBefore(allowedStart) || bookingEndTime.isAfter(allowedEnd)) {
            throw new InvalidBookingTimeException("Bookings are only allowed between 08:00-20:00");
        }
    }

    public List<Booking> list(Long roomId){
        if(roomId == null){
            return bookingRepository.findAll();
        }
        return bookingRepository.findByRoomId(roomId);
    }

    public BookingListResponse listWithFilters(Long roomId, String from, String to, Integer limit, Integer offset) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime fromDate = from != null ? LocalDateTime.parse(from, formatter) : null;
        LocalDateTime toDate = to != null ? LocalDateTime.parse(to, formatter) : null;
        
        int pageLimit = limit != null ? limit : 50;
        int pageOffset = offset != null ? offset : 0;
        Pageable pageable = PageRequest.of(pageOffset / pageLimit, pageLimit);
        
        Page<Booking> bookingPage = bookingRepository.findBookingsWithFilters(roomId, fromDate, toDate, pageable);
        
        return new BookingListResponse(
            bookingPage.getContent(),
            (int) bookingPage.getTotalElements(),
            pageLimit,
            pageOffset
        );
    }

    public Booking cancel(Long id){

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking with ID " + id + " not found"));

        if(booking.getStatus().equals("cancelled")){
            return booking;
        }

        if(LocalDateTime.now().isAfter(booking.getStartTime().minusHours(1))){
            throw new CancellationNotAllowedException("Bookings can only be cancelled at least 1 hour before start time");
        }

        booking.setStatus("cancelled");
        return bookingRepository.save(booking);
    }

    public ResponseEntity<Booking> createWithIdempotency(Booking booking, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository
                .findByKeyAndOrganizerEmail(idempotencyKey, booking.getOrganizerEmail());

        if (existingKey.isPresent()) {
            IdempotencyKey key = existingKey.get();
            if (key.isCompleted() && key.getBookingId() != null) {
                // Return the existing booking
                Booking existingBooking = bookingRepository.findById(key.getBookingId())
                        .orElseThrow(() -> new BookingNotFoundException("Original booking not found"));
                return ResponseEntity.status(HttpStatus.OK).body(existingBooking);
            } else if (!key.isCompleted()) {
                // Request is still in progress
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }

        // Create new idempotency key record
        IdempotencyKey newKey = new IdempotencyKey(idempotencyKey, booking.getOrganizerEmail());
        idempotencyKeyRepository.save(newKey);

        try {
            Booking createdBooking = create(booking);
            
            // Update idempotency key with success result
            newKey.setBookingId(createdBooking.getId());
            newKey.setResponseStatus(HttpStatus.CREATED.value());
            try {
                newKey.setResponseBody(
                    objectMapper.writeValueAsString(createdBooking));
            } catch (JsonProcessingException ex) {
                newKey.setResponseBody("{}");
            }
            newKey.setCompleted(true);
            idempotencyKeyRepository.save(newKey);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBooking);
            
        } catch (Exception e) {
            // Update idempotency key with error result
            try {
                newKey.setResponseStatus(HttpStatus.BAD_REQUEST.value());
                newKey.setResponseBody(objectMapper.writeValueAsString(e.getMessage()));
                newKey.setCompleted(true);
                idempotencyKeyRepository.save(newKey);
            } catch (JsonProcessingException ex) {
                newKey.setResponseStatus(HttpStatus.BAD_REQUEST.value());
                newKey.setResponseBody("Error processing request");
                newKey.setCompleted(true);
                idempotencyKeyRepository.save(newKey);
                throw new RuntimeException("Failed to serialize booking response", ex);
            }
            throw e;
        }
    }
}
