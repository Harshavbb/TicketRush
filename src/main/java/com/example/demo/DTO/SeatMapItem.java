
package com.example.demo.DTO;

import java.math.BigDecimal;

public record SeatMapItem(
        Long showSeatId,
        String seatNumber,
        String seatType,
        BigDecimal price,
        String status
) {

}