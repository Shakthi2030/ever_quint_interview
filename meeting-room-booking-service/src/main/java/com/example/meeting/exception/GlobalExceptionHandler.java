package com.example.meeting.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidBookingTimeException.class)
    public ResponseEntity<Map<String, String>> handleInvalidBookingTimeException(InvalidBookingTimeException ex) {
        return createErrorResponse("ValidationError", ex.getMessage());
    }

    @ExceptionHandler(BookingOverlapException.class)
    public ResponseEntity<Map<String, String>> handleBookingOverlapException(BookingOverlapException ex) {
        return createErrorResponse("ConflictError", ex.getMessage());
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleBookingNotFoundException(BookingNotFoundException ex) {
        return createErrorResponse("NotFoundError", ex.getMessage());
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRoomNotFoundException(RoomNotFoundException ex) {
        return createErrorResponse("NotFoundError", ex.getMessage());
    }

    @ExceptionHandler(RoomNameAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleRoomNameAlreadyExistsException(RoomNameAlreadyExistsException ex) {
        return createErrorResponse("ConflictError", ex.getMessage());
    }

    @ExceptionHandler(CancellationNotAllowedException.class)
    public ResponseEntity<Map<String, String>> handleCancellationNotAllowedException(CancellationNotAllowedException ex) {
        return createErrorResponse("ValidationError", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        FieldError firstError = ex.getBindingResult().getFieldErrors().get(0);
        return createErrorResponse("ValidationError", firstError.getDefaultMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGlobalException(Exception ex) {
        return createErrorResponse("InternalError", "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, String>> createErrorResponse(String errorType, String message) {
        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("error", errorType);
        errorBody.put("message", message);
        
        HttpStatus status;
        switch (errorType) {
            case "ValidationError":
                status = HttpStatus.BAD_REQUEST;
                break;
            case "NotFoundError":
                status = HttpStatus.NOT_FOUND;
                break;
            case "ConflictError":
                status = HttpStatus.CONFLICT;
                break;
            case "InternalError":
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                break;
        }
        
        return new ResponseEntity<>(errorBody, status);
    }
}
