package com.example.demo.DTO;

// com/ticketrush/dto/BookingHoldResponse.java


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingHoldResponse(
        Long bookingId,
        String bookingReference,
        String status,
        BigDecimal totalAmount,
        Instant expiresAt,
        List<Long> showSeatIds
) {}
