package com.example.meeting.dto;

import com.example.meeting.model.Booking;
import java.util.List;

public class BookingListResponse {
    private List<Booking> items;
    private int total;
    private int limit;
    private int offset;

    public BookingListResponse() {}

    public BookingListResponse(List<Booking> items, int total, int limit, int offset) {
        this.items = items;
        this.total = total;
        this.limit = limit;
        this.offset = offset;
    }

    public List<Booking> getItems() { return items; }
    public void setItems(List<Booking> items) { this.items = items; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }
}
