
package com.example.meeting.controller;

import com.example.meeting.model.Room;
import com.example.meeting.service.RoomService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService service;

    public RoomController(RoomService service) {
        this.service = service;
    }

    @PostMapping
    public Room create(@RequestBody Room room){
        return service.create(room);
    }

    @GetMapping
    public List<Room> list(@RequestParam(required=false) Integer minCapacity,
                           @RequestParam(required=false) String amenity){
        return service.list(minCapacity, amenity);
    }
}
