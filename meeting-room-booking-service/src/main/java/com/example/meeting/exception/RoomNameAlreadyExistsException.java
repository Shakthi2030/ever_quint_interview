package com.example.meeting.exception;

public class RoomNameAlreadyExistsException extends RuntimeException {
    public RoomNameAlreadyExistsException(String message) {
        super(message);
    }
}
