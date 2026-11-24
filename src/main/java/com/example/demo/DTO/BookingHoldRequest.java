package com.example.demo.DTO;

 // or com.ticketrush.dto — match your project's package

import java.util.List;

public record BookingHoldRequest(
        Long userId,
        Long showId,
        List<Long> showSeatIds
) {}
