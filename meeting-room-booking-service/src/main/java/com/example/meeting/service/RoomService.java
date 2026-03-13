
package com.example.meeting.service;

import com.example.meeting.exception.RoomNameAlreadyExistsException;
import com.example.meeting.model.Room;
import com.example.meeting.repository.BookingRepository;
import com.example.meeting.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private BookingRepository bookingRepository;

    public Room create(Room room){
        if(roomRepository.existsByNameIgnoreCase(room.getName())){
            throw new RoomNameAlreadyExistsException("Room with name '" + room.getName() + "' already exists");
        }
        return roomRepository.save(room);
    }

    public List<Room> list(Integer minCapacity, String amenity){
        List<Room> rooms = roomRepository.findAll();
        
        if(minCapacity != null){
            rooms = rooms.stream()
                    .filter(r -> r.getCapacity() >= minCapacity)
                    .toList();
        }
        
        if(amenity != null){
            rooms = rooms.stream()
                    .filter(r -> r.getAmenities().contains(amenity))
                    .toList();
        }
        
        return rooms;
    }

    public Room get(Long id){
        return roomRepository.findById(id).orElse(null);
    }
    
    public List<Map<String, Object>> getUtilization(String from, String to) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(from, formatter);
        LocalDateTime end = LocalDateTime.parse(to, formatter);
        
        long totalBizMinutes = countBusinessMinutes(start, end);
        
        return roomRepository.findAll().stream().map(room -> {
            List<com.example.meeting.model.Booking> bookings = bookingRepository
                .findByRoomIdAndStatus(room.getId(), "confirmed")
                .stream()
                .filter(b -> b.getStartTime().isBefore(end) && b.getEndTime().isAfter(start))
                .collect(Collectors.toList());
            
            long bookedMinutes = 0;
            for (com.example.meeting.model.Booking b : bookings) {
                LocalDateTime clippedStart = b.getStartTime().isBefore(start) ? start : b.getStartTime();
                LocalDateTime clippedEnd = b.getEndTime().isAfter(end) ? end : b.getEndTime();
                bookedMinutes += Duration.between(clippedStart, clippedEnd).toMinutes();
            }
            
            double totalBookingHours = Math.round((bookedMinutes / 60.0) * 10.0) / 10.0;
            double utilizationPercent = totalBizMinutes > 0 ? 
                Math.round((double) bookedMinutes / totalBizMinutes * 1000.0) / 1000.0 : 0.0;
            
            Map<String, Object> result = new HashMap<>();
            result.put("roomId", String.valueOf(room.getId()));
            result.put("roomName", room.getName());
            result.put("totalBookingHours", totalBookingHours);
            result.put("utilizationPercent", utilizationPercent);
            
            return result;
        }).collect(Collectors.toList());
    }
    
    private long countBusinessMinutes(LocalDateTime start, LocalDateTime end) {
        long totalMinutes = 0;
        LocalDate current = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        
        while (!current.isAfter(endDate)) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && 
                current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                
                LocalDateTime dayStart = current.atTime(LocalTime.of(8, 0));
                LocalDateTime dayEnd = current.atTime(LocalTime.of(20, 0));
                
                LocalDateTime businessStart = start.isAfter(dayStart) ? start : dayStart;
                LocalDateTime businessEnd = end.isBefore(dayEnd) ? end : dayEnd;
                
                if (businessStart.isBefore(businessEnd)) {
                    totalMinutes += Duration.between(businessStart, businessEnd).toMinutes();
                }
            }
            current = current.plusDays(1);
        }
        
        return totalMinutes;
    }
}
