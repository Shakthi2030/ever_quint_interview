package com.example.meeting.controller;

import com.example.meeting.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private RoomService roomService;

    @GetMapping("/room-utilization")
    public List<Map<String, Object>> getRoomUtilization(
            @RequestParam String from,
            @RequestParam String to) {
        return roomService.getUtilization(from, to);
    }
}
