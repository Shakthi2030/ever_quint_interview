package com.example.meeting.controller;

import com.example.meeting.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomService roomService;

    @Test
    public void testNoBookingsInRangeReturnsZeroUtilization() throws Exception {
        List<Map<String, Object>> mockResponse = Arrays.asList(
            createUtilizationMap("1", "Room A", 0.0, 0.0)
        );

        when(roomService.getUtilization(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(get("/reports/room-utilization")
                .param("from", "2026-03-10T08:00:00")
                .param("to", "2026-03-14T20:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalBookingHours").value(0.0))
                .andExpect(jsonPath("$[0].utilizationPercent").value(0.0));
    }

    @Test
    public void testReturnsCorrectUtilizationFields() throws Exception {
        List<Map<String, Object>> mockResponse = Arrays.asList(
            createUtilizationMap("1", "Room A", 4.0, 0.067)
        );

        when(roomService.getUtilization(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(get("/reports/room-utilization")
                .param("from", "2026-03-10T08:00:00")
                .param("to", "2026-03-14T20:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomName").value("Room A"))
                .andExpect(jsonPath("$[0].utilizationPercent").value(0.067));
    }

    @Test
    public void testMissingRequiredParamsReturns400() throws Exception {
        mockMvc.perform(get("/reports/room-utilization"))
                .andExpect(status().isBadRequest());
    }

    private Map<String, Object> createUtilizationMap(String roomId, String roomName, 
                                                   double totalBookingHours, double utilizationPercent) {
        Map<String, Object> map = new HashMap<>();
        map.put("roomId", roomId);
        map.put("roomName", roomName);
        map.put("totalBookingHours", totalBookingHours);
        map.put("utilizationPercent", utilizationPercent);
        return map;
    }
}
