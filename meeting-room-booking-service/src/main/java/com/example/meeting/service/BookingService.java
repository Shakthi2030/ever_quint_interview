
package com.example.meeting.service;

import com.example.meeting.model.Booking;
import com.example.meeting.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    public Booking create(Booking booking){

        if(!booking.getStartTime().isBefore(booking.getEndTime())){
            throw new RuntimeException("startTime must be before endTime");
        }

        long minutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();

        if(minutes < 15 || minutes > 240){
            throw new RuntimeException("Booking must be between 15 minutes and 4 hours");
        }

        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                booking.getRoomId(), booking.getStartTime(), booking.getEndTime());

        if(!overlappingBookings.isEmpty()){
            throw new RuntimeException("Booking overlap detected");
        }

        return bookingRepository.save(booking);
    }

    public List<Booking> list(Long roomId){
        if(roomId == null){
            return bookingRepository.findAll();
        }
        return bookingRepository.findByRoomId(roomId);
    }

    public Booking cancel(Long id){

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if(booking.getStatus().equals("cancelled")){
            return booking;
        }

        if(LocalDateTime.now().isAfter(booking.getStartTime().minusHours(1))){
            throw new RuntimeException("Cannot cancel less than 1 hour before start");
        }

        booking.setStatus("cancelled");
        return bookingRepository.save(booking);
    }
}
