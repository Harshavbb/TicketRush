package com.example.demo.Controller;

import com.example.demo.DTO.BookingHoldRequest;
import com.example.demo.DTO.BookingHoldResponse;
import com.example.demo.service.BookingService;
import com.example.demo.utils.SeatUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/hold")
    public ResponseEntity<BookingHoldResponse> holdSeats(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody BookingHoldRequest request
    ) {
        BookingHoldResponse response = bookingService.holdSeats(request.userId(), idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleConflict(SeatUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "SEAT_UNAVAILABLE", "message", ex.getMessage()));
    }
}