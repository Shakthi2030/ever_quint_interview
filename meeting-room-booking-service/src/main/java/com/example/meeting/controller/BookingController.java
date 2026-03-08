
package com.example.meeting.controller;

import com.example.meeting.model.Booking;
import com.example.meeting.service.BookingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    public Booking create(@RequestBody Booking booking){
        return service.create(booking);
    }

    @GetMapping
    public List<Booking> list(@RequestParam(required=false) Long roomId){
        return service.list(roomId);
    }

    @PostMapping("/{id}/cancel")
    public Booking cancel(@PathVariable Long id){
        return service.cancel(id);
    }
}
