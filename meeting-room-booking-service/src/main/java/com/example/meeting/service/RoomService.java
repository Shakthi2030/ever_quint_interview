
package com.example.meeting.service;

import com.example.meeting.model.Room;
import com.example.meeting.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    public Room create(Room room){
        if(roomRepository.existsByNameIgnoreCase(room.getName())){
            throw new RuntimeException("Room name must be unique");
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
}
