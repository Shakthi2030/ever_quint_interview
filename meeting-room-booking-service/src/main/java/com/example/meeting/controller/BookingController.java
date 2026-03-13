
package com.example.meeting.controller;

import com.example.meeting.dto.BookingListResponse;
import com.example.meeting.model.Booking;
import com.example.meeting.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Booking> create(@Valid @RequestBody Booking booking,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            return service.createWithIdempotency(booking, idempotencyKey);
        } else {
            Booking createdBooking = service.create(booking);
            return ResponseEntity.status(201).body(createdBooking);
        }
    }

    @GetMapping
    public BookingListResponse list(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset){
        return service.listWithFilters(roomId, from, to, limit, offset);
    }

    @PostMapping("/{id}/cancel")
    public Booking cancel(@PathVariable Long id){
        return service.cancel(id);
    }
}
